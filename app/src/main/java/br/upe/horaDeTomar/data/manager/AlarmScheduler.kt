package br.upe.horaDeTomar.data.manager

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import br.upe.horaDeTomar.data.entities.Alarm

class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager
) {
    fun schedule(alarm: Alarm) {
        val hour = alarm.hour.toIntOrNull() ?: 0
        val minute = alarm.minute.toIntOrNull() ?: 0

        alarm.daysSelected.forEach { (dayKey, selected) ->
            if (!selected) return@forEach

            val triggerAt = nextTriggerMillis(dayKey, hour, minute)
            val reqCode = requestCodeFor(alarm.id, dayKey)

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("ALARM_ID", alarm.id)
                putExtra("MEDICATION_ID", alarm.medicationId)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Try-Catch para evitar o crash
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    !alarmManager.canScheduleExactAlarms()
                ) {
                    val showIntent = PendingIntent.getActivity(
                        context,
                        reqCode,
                        Intent(context, br.upe.horaDeTomar.ui.reminders.AlarmActivity::class.java)
                            .putExtra("alarmId", alarm.id)
                            .putExtra("MEDICATION_ID", alarm.medicationId),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val clockInfo = AlarmClockInfo(triggerAt, showIntent)
                    alarmManager.setAlarmClock(clockInfo, pi)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } catch (e: SecurityException) {
                Log.e("ALARM_SCHEDULER", "Permissão negada para alarme exato. Usando fallback.", e)
                // Usa alarme comum que não exige permissão, para o app não fechar
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }

    fun snooze(alarmId: Int, medicationId: Int, hour: Int, minute: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("MEDICATION_ID", medicationId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            alarmId, // snooze usa um requestCode simples
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Try-Catch para evitar o crash
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                val show = PendingIntent.getActivity(
                    context,
                    alarmId,
                    Intent(context, br.upe.horaDeTomar.ui.reminders.AlarmActivity::class.java)
                        .putExtra("alarmId", alarmId)
                        .putExtra("MEDICATION_ID", medicationId),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val clockInfo = AlarmClockInfo(cal.timeInMillis, show)
                alarmManager.setAlarmClock(clockInfo, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } catch (e: SecurityException) {
            Log.e("ALARM_SCHEDULER", "Permissão negada para alarme exato (Snooze). Usando fallback.", e)
            // Usa alarme comum que não exige permissão, para o app não fechar
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        alarm.daysSelected.forEach { (dayKey, selected) ->
            if (!selected) return@forEach
            val intent = Intent(context, AlarmReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                requestCodeFor(alarm.id, dayKey),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pi)
        }
        Log.d("ALARM_SCHEDULE", "Alarme cancelado: ${alarm.id}")
    }

    private fun nextTriggerMillis(dayKey: String, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.DAY_OF_WEEK, dayOfWeekFromKey(dayKey))
            if (before(Calendar.getInstance())) add(Calendar.WEEK_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun dayOfWeekFromKey(key: String): Int = when (key) {
        "Dom" -> Calendar.SUNDAY
        "Seg" -> Calendar.MONDAY
        "Ter" -> Calendar.TUESDAY
        "Qua" -> Calendar.WEDNESDAY
        "Qui" -> Calendar.THURSDAY
        "Sex" -> Calendar.FRIDAY
        "Sab" -> Calendar.SATURDAY
        else  -> Calendar.MONDAY
    }

    private fun requestCodeFor(alarmId: Int, dayKey: String): Int =
        alarmId * 31 + dayKey.hashCode()
}