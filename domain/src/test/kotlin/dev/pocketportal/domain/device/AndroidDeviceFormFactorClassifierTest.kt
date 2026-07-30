package dev.pocketportal.domain.device

import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidDeviceFormFactorClassifierTest {
    @Test
    fun `classifies a handset below the Android tablet breakpoint`() {
        assertEquals(
            AndroidDeviceFormFactor.PHONE,
            AndroidDeviceFormFactorClassifier.classify(
                characteristics = emptySet(),
                displayWidthPixels = 1_440,
                displayHeightPixels = 3_040,
                displayDensityDpi = 560,
            ),
        )
    }

    @Test
    fun `classifies a display at the Android tablet breakpoint`() {
        assertEquals(
            AndroidDeviceFormFactor.TABLET,
            AndroidDeviceFormFactorClassifier.classify(
                characteristics = emptySet(),
                displayWidthPixels = 1_600,
                displayHeightPixels = 2_560,
                displayDensityDpi = 320,
            ),
        )
    }

    @Test
    fun `leaves foldable hardware unknown for an explicit style override`() {
        assertEquals(
            AndroidDeviceFormFactor.UNKNOWN,
            AndroidDeviceFormFactorClassifier.classify(
                characteristics = setOf("foldable"),
                displayWidthPixels = 1_840,
                displayHeightPixels = 2_208,
                displayDensityDpi = 420,
            ),
        )
    }
}
