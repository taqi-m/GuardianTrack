package com.guardiantrack.core.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.guardiantrack.core.model.oem.OemDiagnosticReport
import com.guardiantrack.core.model.oem.OemIntentSpec
import com.guardiantrack.core.model.oem.OemManufacturer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OemSurvivalEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun detectManufacturer(): OemManufacturer {
        val m = Build.MANUFACTURER.lowercase()
        return when {
            m.contains("samsung") -> OemManufacturer.SAMSUNG
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> OemManufacturer.XIAOMI
            m.contains("huawei") || m.contains("honor") -> OemManufacturer.HUAWEI
            m.contains("oppo") || m.contains("realme") -> OemManufacturer.OPPO
            m.contains("vivo") || m.contains("iqoo") -> OemManufacturer.VIVO
            m.contains("oneplus") -> OemManufacturer.ONEPLUS
            m.contains("google") -> OemManufacturer.GOOGLE_PIXEL
            else -> OemManufacturer.GENERIC
        }
    }

    fun isBatteryOptimizationExempted(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
    }

    fun createStandardExemptionIntent(): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    fun getOemDiagnosticReport(): OemDiagnosticReport {
        val m = detectManufacturer()
        val exempted = isBatteryOptimizationExempted()

        val guideTitle = when (m) {
            OemManufacturer.SAMSUNG -> "Samsung One UI Survival Setup"
            OemManufacturer.XIAOMI -> "Xiaomi HyperOS / MIUI Autostart Setup"
            OemManufacturer.HUAWEI -> "Huawei EMUI Launch Protection"
            OemManufacturer.OPPO -> "Oppo ColorOS Startup Authorization"
            OemManufacturer.VIVO -> "Vivo Funtouch Background Power"
            OemManufacturer.ONEPLUS -> "OnePlus OxygenOS Battery Optimization"
            OemManufacturer.GOOGLE_PIXEL -> "Google Pixel Adaptive Battery Exemption"
            OemManufacturer.GENERIC -> "Standard Android Battery Exemption"
        }

        val steps = when (m) {
            OemManufacturer.XIAOMI -> listOf(
                "Open App Info -> Enable 'Autostart'",
                "Open Battery Saver -> Set to 'No Restrictions'",
                "Lock GuardianTrack in Recent Apps tray to prevent auto-kill"
            )
            OemManufacturer.SAMSUNG -> listOf(
                "Go to Settings -> Apps -> GuardianTrack -> Battery",
                "Select 'Unrestricted' (prevents sleeping app policy)",
                "Under 'Device Care', ensure app is in 'Never Sleeping Apps'"
            )
            OemManufacturer.HUAWEI -> listOf(
                "Go to Settings -> Battery -> App Launch",
                "Find GuardianTrack -> Switch to 'Manage Manually'",
                "Enable Auto-launch, Secondary launch, and Run in background"
            )
            OemManufacturer.OPPO -> listOf(
                "Go to Settings -> App Management -> Auto-launch",
                "Toggle GuardianTrack to 'Allowed'",
                "Disable Deep Sleep / Background freeze in Battery settings"
            )
            OemManufacturer.VIVO -> listOf(
                "Go to Settings -> Battery -> High Background Power Usage",
                "Enable GuardianTrack to consume power continuously",
                "Grant 'Autostart' in iManager"
            )
            else -> listOf(
                "Disable Battery Optimization for GuardianTrack",
                "Ensure background data access is unrestricted"
            )
        }

        return OemDiagnosticReport(
            manufacturer = m,
            deviceModel = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            androidVersion = Build.VERSION.SDK_INT,
            isBatteryOptimizationIgnored = exempted,
            requiresAutoStartPermission = m in listOf(OemManufacturer.XIAOMI, OemManufacturer.HUAWEI, OemManufacturer.OPPO, OemManufacturer.VIVO),
            requiresBackgroundDataWhitelisting = m == OemManufacturer.SAMSUNG,
            manufacturerGuideTitle = guideTitle,
            manufacturerGuideSteps = steps
        )
    }

    fun getOemIntentSpecs(): List<OemIntentSpec> {
        return listOf(
            OemIntentSpec(
                targetManufacturer = OemManufacturer.XIAOMI,
                packageName = "com.miui.securitycenter",
                className = "com.miui.permcenter.autostart.AutoStartManagementActivity",
                humanReadableName = "Xiaomi Autostart Settings"
            ),
            OemIntentSpec(
                targetManufacturer = OemManufacturer.XIAOMI,
                packageName = "com.miui.powerkeeper",
                className = "com.miui.powerkeeper.ui.HiddenAppsConfigActivity",
                humanReadableName = "Xiaomi No Restrictions Mode"
            ),
            OemIntentSpec(
                targetManufacturer = OemManufacturer.SAMSUNG,
                packageName = "com.samsung.android.lool",
                className = "com.samsung.android.sm.ui.battery.BatteryActivity",
                humanReadableName = "Samsung Battery Management"
            ),
            OemIntentSpec(
                targetManufacturer = OemManufacturer.HUAWEI,
                packageName = "com.huawei.systemmanager",
                className = "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
                humanReadableName = "Huawei App Launch Settings"
            ),
            OemIntentSpec(
                targetManufacturer = OemManufacturer.OPPO,
                packageName = "com.coloros.safecenter",
                className = "com.coloros.safecenter.permission.startup.StartupAppListActivity",
                humanReadableName = "Oppo Auto-launch Manager"
            ),
            OemIntentSpec(
                targetManufacturer = OemManufacturer.VIVO,
                packageName = "com.iqoo.secure",
                className = "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
                humanReadableName = "Vivo High Power Whitelist"
            )
        )
    }

    fun launchOemSetting(spec: OemIntentSpec): Boolean {
        val intent = Intent().apply {
            component = ComponentName(spec.packageName, spec.className ?: "")
            if (spec.action != null) action = spec.action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            if (resolved != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
