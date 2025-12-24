package br.upe.horaDeTomar.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.upe.horaDeTomar.data.daos.UserDao
import br.upe.horaDeTomar.data.entities.User
import br.upe.horaDeTomar.data.remote.FhirDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UserFhirSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: UserDao,
    private val dataSource: FhirDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val users = dao.getUnsyncedUsers()
            users.forEach { user ->
                if (!isPatientAlreadyRegister(user)) {
                    val result = dataSource.createPatient(user)
                    if (result != null) {
                        user.isSynced = true
                        dao.update(user)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("UserFhirSyncWorker", "Erro: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun isPatientAlreadyRegister(user: User): Boolean {
        val patient = dataSource.getPatientByIdentifier(user.cpf)

        return patient != null
    }
}