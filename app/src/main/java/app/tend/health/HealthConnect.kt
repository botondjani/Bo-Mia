package app.tend.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import app.tend.core.TaskDef
import app.tend.core.Tasks
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.tend.core.ActivitySource
import java.time.LocalDate
import java.time.LocalDateTime

/** Steps and exercise minutes for today, read from Health Connect while tend is in the foreground. */
/**
 * Today's steps and exercise, and last night's sleep, read from Health Connect while tend is in
 * the foreground. Only what the picked activities need is asked for and read.
 */
class HealthConnect(private val context: Context, private val selected: List<TaskDef>) : ActivitySource {

    enum class Status { READY, NEEDS_PERMISSION, NEEDS_INSTALL, UNSUPPORTED }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun status(): Status = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE ->
            if (client.permissionController.getGrantedPermissions().containsAll(permissionsFor(selected))) Status.READY
            else Status.NEEDS_PERMISSION
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NEEDS_INSTALL
        else -> Status.UNSUPPORTED
    }

    override suspend fun stepsToday(): Long? = readIf(STEPS) {
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), today()))[StepsRecord.COUNT_TOTAL] ?: 0
    }

    override suspend fun exerciseMinutesToday(): Int? = readIf(EXERCISE) {
        val total = client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), today()))
        total[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toInt() ?: 0
    }

    /** Workout minutes over the last [days] days: zero means nothing writes workouts here. */
    suspend fun exerciseMinutesLately(days: Long = 30): Int? = readIf(EXERCISE) {
        val range = TimeRangeFilter.between(LocalDate.now().minusDays(days).atStartOfDay(), LocalDateTime.now())
        client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), range))[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
            ?.toMinutes()?.toInt() ?: 0
    }

    /** Sleep that ended this morning: sessions between 6 pm yesterday and 2 pm today. */
    override suspend fun sleepMinutesLastNight(): Int? = readIf(SLEEP) {
        val night = TimeRangeFilter.between(LocalDate.now().minusDays(1).atTime(18, 0), LocalDate.now().atTime(14, 0))
        client.aggregate(AggregateRequest(setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL), night))[SleepSessionRecord.SLEEP_DURATION_TOTAL]
            ?.toMinutes()?.toInt() ?: 0
    }

    /** Whether this phone's Health Connect supports background reads. */
    suspend fun backgroundAvailable(): Boolean = runCatching {
        HealthConnectClient.getSdkStatus(context, PROVIDER) == HealthConnectClient.SDK_AVAILABLE &&
            client.features.getFeatureStatus(androidx.health.connect.client.HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            androidx.health.connect.client.HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }.getOrDefault(false)

    private suspend fun <T> readIf(permission: String, read: suspend () -> T): T? {
        if (permission !in permissionsFor(selected)) return null
        if (HealthConnectClient.getSdkStatus(context, PROVIDER) != HealthConnectClient.SDK_AVAILABLE) return null
        if (permission !in client.permissionController.getGrantedPermissions()) return null
        return runCatching { read() }.getOrNull()
    }

    private fun today() = TimeRangeFilter.between(LocalDate.now().atStartOfDay(), LocalDateTime.now())

    companion object {
        /** Health Connect's own app; on Android 13 and below it comes from the Play Store. */
        const val PROVIDER = "com.google.android.apps.healthdata"

        private val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
        private val EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
        private val SLEEP = HealthPermission.getReadPermission(SleepSessionRecord::class)

        /** Only what the picked activities need. */
        /**
         * Lets the monitor read in the background (walks count without opening tend). Asked on the
         * same Health Connect screen as the rest, and optional: nothing breaks without it.
         */
        const val BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        fun permissionsFor(selected: List<TaskDef>): Set<String> = buildSet {
            if (Tasks.OUTSIDE in selected) add(STEPS)
            if (Tasks.MOVE in selected) add(EXERCISE)
            if (Tasks.SLEEP in selected) add(SLEEP)
        }

        fun installIntent(): Intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"),
        ).setPackage("com.android.vending")
    }
}
