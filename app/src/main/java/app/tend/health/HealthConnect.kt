package app.tend.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import app.tend.core.ActivitySource
import java.time.LocalDate
import java.time.LocalDateTime

/** Steps and exercise minutes for today, read from Health Connect while tend is in the foreground. */
class HealthConnect(private val context: Context) : ActivitySource {

    enum class Status { READY, NEEDS_PERMISSION, NEEDS_INSTALL, UNSUPPORTED }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    suspend fun status(): Status = when (HealthConnectClient.getSdkStatus(context, PROVIDER)) {
        HealthConnectClient.SDK_AVAILABLE ->
            if (client.permissionController.getGrantedPermissions().containsAll(PERMISSIONS)) Status.READY
            else Status.NEEDS_PERMISSION
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Status.NEEDS_INSTALL
        else -> Status.UNSUPPORTED
    }

    override suspend fun stepsToday(): Long? = readIfReady {
        client.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), today()))[StepsRecord.COUNT_TOTAL] ?: 0
    }

    override suspend fun exerciseMinutesToday(): Int? = readIfReady {
        val total = client.aggregate(AggregateRequest(setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL), today()))
        total[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toInt() ?: 0
    }

    private suspend fun <T> readIfReady(read: suspend () -> T): T? =
        if (status() == Status.READY) runCatching { read() }.getOrNull() else null

    private fun today() = TimeRangeFilter.between(LocalDate.now().atStartOfDay(), LocalDateTime.now())

    companion object {
        /** Health Connect's own app; on Android 13 and below it comes from the Play Store. */
        const val PROVIDER = "com.google.android.apps.healthdata"

        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        )

        fun installIntent(): Intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"),
        ).setPackage("com.android.vending")
    }
}
