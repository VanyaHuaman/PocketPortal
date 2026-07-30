package dev.pocketportal.infrastructure.diagnostics

internal object DiagnosticConstants {
    const val OPERATING_SYSTEM_CHECK_ID = "operating-system"
    const val JAVA_CHECK_ID = "java-runtime"
    const val ADB_CHECK_ID = "adb"
    const val DEVICES_CHECK_ID = "android-devices"
    const val SYSTEMD_CHECK_ID = "systemd-user"
    const val LINGER_CHECK_ID = "systemd-linger"
    const val SERVICE_CHECK_ID = "pocketportal-service"
    const val SEGFAULT_CHECK_ID = "kernel-segfaults"

    const val OS_RELEASE_PATH = "/etc/os-release"
    const val OS_ID_KEY = "ID"
    const val OS_VERSION_KEY = "VERSION_ID"
    const val OS_PRETTY_NAME_KEY = "PRETTY_NAME"
    const val OS_ID_LIKE_KEY = "ID_LIKE"
    const val JAVA_VERSION_PROPERTY = "java.version"
    const val JAVA_VENDOR_PROPERTY = "java.vendor"
    const val USER_ENVIRONMENT_VARIABLE = "USER"
    const val UNKNOWN_VALUE = "unknown"
    const val MINIMUM_JAVA_VERSION = 17
    const val SUCCESS_EXIT_CODE = 0
    const val NO_SEGFAULTS = 0

    const val LINGER_ENABLED = "yes"
    const val SERVICE_ACTIVE = "active"
    const val SYSTEMD_RUNNING = "running"
    const val SYSTEMD_DEGRADED = "degraded"
    const val POCKETPORTAL_SERVICE = "pocketportal.service"
    const val SEGFAULT_MARKER = "segfault"
    const val ADB_VERSION_ARGUMENT = "version"
    const val SYSTEMCTL_COMMAND = "systemctl"
    const val USER_ARGUMENT = "--user"
    const val SYSTEMD_STATE_ARGUMENT = "is-system-running"
    const val LOGINCTL_COMMAND = "loginctl"
    const val SHOW_USER_ARGUMENT = "show-user"
    const val PROPERTY_ARGUMENT = "-p"
    const val LINGER_PROPERTY = "Linger"
    const val VALUE_ARGUMENT = "--value"
    const val SERVICE_STATE_ARGUMENT = "is-active"
    const val JOURNALCTL_COMMAND = "journalctl"
    const val KERNEL_ARGUMENT = "-k"
    const val CURRENT_BOOT_ARGUMENT = "-b"
    const val NO_PAGER_ARGUMENT = "--no-pager"

    val VERIFIED_LINUX_RELEASES = setOf("ubuntu:25.10")
    val END_OF_LIFE_LINUX_RELEASES = setOf("ubuntu:25.10")
    val RECOGNIZED_LINUX_FAMILIES = setOf(
        "ubuntu",
        "debian",
        "fedora",
        "rhel",
        "centos",
        "rocky",
        "almalinux",
        "linuxmint",
        "pop",
    )
}
