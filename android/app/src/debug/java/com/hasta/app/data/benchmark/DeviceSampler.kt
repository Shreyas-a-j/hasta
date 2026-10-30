package com.hasta.app.data.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Debug
import android.os.PowerManager
import androidx.core.content.pm.PackageInfoCompat

/** Identifies the device and app build that produced a run. */
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val device: String,
    val androidRelease: String,
    val sdkInt: Int,
    val supportedAbis: List<String>,
    val appVersionName: String,
    val appVersionCode: Long,
    val totalRamMb: Long,
)

/** One 1 Hz environmental sample. */
data class DeviceSample(
    val pssKb: Long,
    val batteryPercent: Int?,
    val charging: Boolean?,
    val batteryTemperatureC: Float?,
    /** `PowerManager.currentThermalStatus`, or -1 below API 29. */
    val thermalStatus: Int,
)

/**
 * Reads device facts from the platform. Nothing here is estimated or fabricated; a
 * value the platform does not expose is recorded as absent.
 */
class DeviceSampler(private val context: Context) {

    fun deviceInfo(): DeviceInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as android.app.ActivityManager
        activityManager.getMemoryInfo(memoryInfo)

        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            androidRelease = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            appVersionName = packageInfo.versionName ?: "unknown",
            appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            totalRamMb = memoryInfo.totalMem / BYTES_PER_MB,
        )
    }

    fun sample(): DeviceSample {
        val battery = batterySnapshot()
        return DeviceSample(
            pssKb = Debug.getPss().toLong(),
            batteryPercent = battery.percent,
            charging = battery.charging,
            batteryTemperatureC = battery.temperatureC,
            thermalStatus = thermalStatus(),
        )
    }

    private fun batterySnapshot(): BatterySnapshot {
        val intent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        ) ?: return BatterySnapshot(null, null, null)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) level * 100 / scale else null

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING,
            BatteryManager.BATTERY_STATUS_FULL,
            -> true

            BatteryManager.BATTERY_STATUS_DISCHARGING,
            BatteryManager.BATTERY_STATUS_NOT_CHARGING,
            -> false

            else -> null
        }

        val temperatureTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val temperatureC = if (temperatureTenths == Int.MIN_VALUE) {
            null
        } else {
            temperatureTenths / 10f
        }

        return BatterySnapshot(percent, charging, temperatureC)
    }

    private fun thermalStatus(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return -1
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.currentThermalStatus
    }

    private data class BatterySnapshot(
        val percent: Int?,
        val charging: Boolean?,
        val temperatureC: Float?,
    )

    private companion object {
        const val BYTES_PER_MB = 1024L * 1024L
    }
}

/** Human-readable thermal status for the report and JSON. */
fun thermalStatusName(status: Int): String = when (status) {
    PowerManager.THERMAL_STATUS_NONE -> "none"
    PowerManager.THERMAL_STATUS_LIGHT -> "light"
    PowerManager.THERMAL_STATUS_MODERATE -> "moderate"
    PowerManager.THERMAL_STATUS_SEVERE -> "severe"
    PowerManager.THERMAL_STATUS_CRITICAL -> "critical"
    PowerManager.THERMAL_STATUS_EMERGENCY -> "emergency"
    PowerManager.THERMAL_STATUS_SHUTDOWN -> "shutdown"
    -1 -> "unavailable"
    else -> "unknown($status)"
}
