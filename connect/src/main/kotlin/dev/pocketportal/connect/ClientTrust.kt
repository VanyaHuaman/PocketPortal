package dev.pocketportal.connect

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

internal object ClientTrust {
    fun trustManager(additionalCaBundle: Path?): X509TrustManager {
        val system = trustManagerFor(null)
        if (additionalCaBundle == null) return system
        require(Files.isRegularFile(additionalCaBundle)) {
            "CA certificate bundle does not exist: ${additionalCaBundle.toAbsolutePath()}"
        }

        val certificates = Files.newInputStream(additionalCaBundle).use { input ->
            CertificateFactory.getInstance(CERTIFICATE_TYPE)
                .generateCertificates(input)
                .map { it as X509Certificate }
        }
        require(certificates.isNotEmpty()) {
            "CA certificate bundle contains no X.509 certificates"
        }
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null)
            certificates.forEachIndexed { index, certificate ->
                setCertificateEntry("$CERTIFICATE_ALIAS_PREFIX$index", certificate)
            }
        }
        return CompositeTrustManager(
            listOf(system, trustManagerFor(keyStore)),
        )
    }

    private fun trustManagerFor(keyStore: KeyStore?): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(keyStore)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().single()
    }

    private const val CERTIFICATE_TYPE = "X.509"
    private const val CERTIFICATE_ALIAS_PREFIX = "pocketportal-ca-"
}

private class CompositeTrustManager(
    private val delegates: List<X509TrustManager>,
) : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
        checkTrusted { it.checkClientTrusted(chain, authType) }
    }

    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        checkTrusted { it.checkServerTrusted(chain, authType) }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegates.flatMap { it.acceptedIssuers.asList() }.distinct().toTypedArray()

    private fun checkTrusted(check: (X509TrustManager) -> Unit) {
        var lastFailure: Exception? = null
        for (delegate in delegates) {
            try {
                check(delegate)
                return
            } catch (exception: Exception) {
                lastFailure = exception
            }
        }
        throw requireNotNull(lastFailure)
    }
}
