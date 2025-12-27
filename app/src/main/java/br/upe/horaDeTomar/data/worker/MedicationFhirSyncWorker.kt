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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@HiltWorker
class MedicationFhirSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val medicationDao: MedicationDao,
    private val alarmDao: AlarmDao,
    private val userDao: UserDao,
    private val dataSource: FhirDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val localId = inputData.getInt("local_medication_id", -1)
            val remoteFhirId = inputData.getString("remove_fhir_id")

            Log.d("TESTE", "doWork: $localId $remoteFhirId")

            if (localId != -1) {
                syncSingleMedication(localId, remoteFhirId)
            } else {
                syncAllUnsynced()
            }
        } catch (e: Exception) {
            Log.e("MedicationFhirSyncWorker", "Erro fatal: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun syncSingleMedication(localId: Int, fhirId: String?): Result {
        val medication = medicationDao.getMedicationById(localId)

        if (medication == null) {
            Log.w("SyncWorker", "Medicamento $localId não encontrado ainda. Tentando novamente...")
            return Result.retry()
        }

        val alarms = alarmDao.getAlarmsForMedicationOnce(localId)
        val patientId = getFhirPatientId(medication.userId)

        if (patientId == null) {
            Log.w("SyncWorker", "Paciente ou ID FHIR não encontrado. Tentando novamente...")
            return Result.retry()
        }

        val result = dataSource.createMedicationStatement(
            medication = medication,
            alarms = alarms,
            patientFhirId = patientId,
            medicationFhirId = fhirId
        )

        return if (result != null) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    private suspend fun syncAllUnsynced(): Result {
        val unsyncedMedications = medicationDao.getUnsyncedMedications()

        if (unsyncedMedications.isEmpty()) return Result.success()

        var allSuccess = true

        unsyncedMedications.forEach { medication ->
            val alarms = alarmDao.getAlarmsForMedicationOnce(medication.id)
            val patientId = getFhirPatientId(medication.userId)

            if (patientId != null) {
                val result = dataSource.createMedicationStatement(medication, alarms, patientId, null)
                if (result == null) allSuccess = false
            } else {
                allSuccess = false
            }
        }

        return if (allSuccess) Result.success() else Result.retry()
    }

    private suspend fun getFhirPatientId(userId: Int): String? {
        val user = userDao.getUserById(userId) ?: return null
        return try {
            val patient = dataSource.getPatientByIdentifier(user.cpf)
            patient?.idElement?.idPart
        } catch (e: Exception) {
            Log.e("SyncWorker", "Erro ao buscar paciente no FHIR: ${e.message}")
            null
        }
    }
}