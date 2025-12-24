package br.upe.horaDeTomar.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.upe.horaDeTomar.data.daos.AlarmDao
import br.upe.horaDeTomar.data.daos.MedicationDao
import br.upe.horaDeTomar.data.daos.UserDao
import br.upe.horaDeTomar.data.remote.FhirDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class MedicationFhirSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationDao: MedicationDao,
    private val alarmDao: AlarmDao,
    private val userDao: UserDao,
    private val dataSource: FhirDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val unsyncedMedications = medicationDao.getUnsyncedMedications()

            unsyncedMedications.forEach { medication ->
                val alarms = alarmDao.getAlarmsForMedicationOnce(medication.id)
                val patientId = getFhirPatientId(medication.userId)
                if (patientId == null) {
                    Log.e("MedicationFhirSyncWorker", "Paciente não encontrado para o usuário ${medication.userId}")
                    return Result.failure()
                }
                val result = dataSource.createMedicationStatement(medication, alarms, patientId)
                if (result != null) {
                    medication.isSynced = true
                    medicationDao.update(medication)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("MedicationFhirSyncWorker", "Erro: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun getFhirPatientId(userId: Int): String? {
        val user = userDao.getUserById(userId)
        if(user == null) {
            Log.e("MedicationFhirSyncWorker", "Usuário não encontrado para o ID $userId")
            return null
        }

        val patient = dataSource.getPatientByIdentifier(user.cpf)
        return patient?.idElement?.idPart
    }
}