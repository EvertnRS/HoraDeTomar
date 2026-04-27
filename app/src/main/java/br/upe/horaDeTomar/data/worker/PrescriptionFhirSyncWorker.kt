package br.upe.horaDeTomar.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.upe.horaDeTomar.data.daos.AlarmDao
import br.upe.horaDeTomar.data.daos.MedicationDao
import br.upe.horaDeTomar.data.daos.PrescriptionDao
import br.upe.horaDeTomar.data.daos.UserDao
import br.upe.horaDeTomar.data.entities.Prescription
import br.upe.horaDeTomar.data.mapper.FhirPrescriptionMapper
import br.upe.horaDeTomar.data.remote.FhirDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

@HiltWorker
class PrescriptionFhirSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prescriptionDao: PrescriptionDao,
    private val alarmDao: AlarmDao,
    private val userDao: UserDao,
    private val dataSource: FhirDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val isPullRequest = inputData.getBoolean("is_pull_request", false)

            if (isPullRequest) {
                fetchNewPrescriptions()
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PrescriptionSyncWorker", "Erro fatal: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchNewPrescriptions() {
        val userList = userDao.getUsers().firstOrNull()

        val currentUser = userList?.firstOrNull()


        if (currentUser == null) {
            Log.w("PrescriptionSyncWorker", "Nenhum usuário encontrado.")
            return
        }

        try {
            val fhirRequests = dataSource.getPatientMedicationRequests(currentUser.cpf)
            val patient = dataSource.getPatientByIdentifier(currentUser.cpf)
            val expectedReference = "Patient/${patient?.idElement?.idPart}"

            fhirRequests.forEach { fhirRequest ->
                val fhirId = fhirRequest.idElement.idPart ?: return@forEach
                val subjectReference = fhirRequest.subject?.reference

                if (subjectReference != expectedReference) {
                    Log.w("PrescriptionSync", "Prescrição ignorada. subject=$subjectReference esperado=$expectedReference")
                    return@forEach
                }

                val existsLocally = prescriptionDao.getByFhirId(fhirId) != null
                if (!existsLocally) {
                    val newPrescription = FhirPrescriptionMapper.toPrescription(fhirRequest, currentUser.id)
                    prescriptionDao.insert(newPrescription)
                }
            }
        } catch (e: Exception) {
            Log.e("PrescriptionSync", "Erro ao buscar novas prescrições: ${e.message}")
            throw e
        }
    }
}