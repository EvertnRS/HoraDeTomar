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
        val medication = medicationDao.getMedicationById(localId) ?: return Result.failure()
        val alarms = alarmDao.getAlarmsForMedicationOnce(localId)
        val patientId = getFhirPatientId(medication.userId)

        if (patientId == null) {
            Log.w("syncSingleMedication", "Paciente não encontrado no FHIR.")
            return Result.retry()
        }

        var finalMedicationFhirId = fhirId

        if (finalMedicationFhirId.isNullOrBlank()) {
            Log.d("syncSingleMedication", "MedicationFhirId não informado. Buscando medicamento no FHIR...")

            val foundMedications = dataSource.searchMedications(medication.name)

            val existingMedication = foundMedications.firstOrNull {
                it.name.equals(medication.name, ignoreCase = true)
            }

            if (existingMedication != null) {
                finalMedicationFhirId = existingMedication.id
                Log.d("syncSingleMedication", "Medication encontrado no FHIR. ID: $finalMedicationFhirId")
            } else {
                Log.d(
                    "syncSingleMedication",
                    "Medication não encontrado no FHIR. Será enviado no MedicationStatement como CodeableConcept."
                )
            }
        }

        val result = dataSource.createMedicationStatement(
            medication = medication,
            alarms = alarms,
            patientFhirId = patientId,
            medicationFhirId = finalMedicationFhirId
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
                var medicationFhirId: String? = null

                val foundMedications = dataSource.searchMedications(medication.name)
                val existingMedication = foundMedications.firstOrNull {
                    it.name.equals(medication.name, ignoreCase = true)
                }

                if (existingMedication != null) {
                    medicationFhirId = existingMedication.id
                    Log.d("syncAllUnsynced", "Medication encontrado no FHIR. ID: $medicationFhirId")
                } else {
                    Log.d(
                        "syncAllUnsynced",
                        "Medication não encontrado no FHIR. Será enviado como CodeableConcept."
                    )
                }

                val result = dataSource.createMedicationStatement(
                    medication = medication,
                    alarms = alarms,
                    patientFhirId = patientId,
                    medicationFhirId = medicationFhirId
                )

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