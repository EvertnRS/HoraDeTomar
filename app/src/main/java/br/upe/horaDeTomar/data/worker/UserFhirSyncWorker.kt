package br.upe.horaDeTomar.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import br.upe.horaDeTomar.data.daos.UserDao
import br.upe.horaDeTomar.data.remote.FhirDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class UserFhirSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val dao: UserDao,
    private val dataSource: FhirDataSource
): CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {4
        Log.d("TESTE", "ENTROU!")
        return try {
            val users = dao.getUnsyncedUsers()
            users.forEach { user ->
                Log.d("TESTE", "USER: ${user.name}")
                val result = dataSource.createPatient(user)
                Log.d("TESTE", "RESULTADO: $result")
                if(result != null) {
                    user.isSynced = true
                    dao.update(user)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("UserFhirSyncWorker", "DEU RUIM! Erro: ${e.message}")
            e.printStackTrace()
            Result.retry()
        }
    }
}