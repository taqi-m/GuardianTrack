package com.guardiantrack.core.model.oem

enum class OemManufacturer {
    SAMSUNG,
    XIAOMI,
    HUAWEI,
    OPPO,
    VIVO,
    ONEPLUS,
    GOOGLE_PIXEL,
    GENERIC
}

data class OemDiagnosticReport(
    val manufacturer: OemManufacturer,
    val deviceModel: String,
    val androidVersion: Int,
    val isBatteryOptimizationIgnored: Boolean,
    val requiresAutoStartPermission: Boolean,
    val requiresBackgroundDataWhitelisting: Boolean,
    val manufacturerGuideTitle: String,
    val manufacturerGuideSteps: List<String>
)

data class OemIntentSpec(
    val targetManufacturer: OemManufacturer,
    val packageName: String,
    val className: String? = null,
    val action: String? = null,
    val humanReadableName: String
)
