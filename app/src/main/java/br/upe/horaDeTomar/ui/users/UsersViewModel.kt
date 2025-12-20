    package br.upe.horaDeTomar.ui.users

    import android.util.Log
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.setValue
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import androidx.work.Constraints
    import androidx.work.ExistingWorkPolicy
    import androidx.work.NetworkType
    import androidx.work.OneTimeWorkRequestBuilder
    import androidx.work.WorkManager
    import br.upe.horaDeTomar.data.entities.User
    import br.upe.horaDeTomar.data.repositories.UserRepository
    import br.upe.horaDeTomar.data.worker.UserFhirSyncWorker
    import dagger.hilt.android.lifecycle.HiltViewModel
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.SharingStarted
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.stateIn
    import kotlinx.coroutines.launch
    import javax.inject.Inject

    @HiltViewModel
    class UsersViewModel @Inject constructor(
        private val repository: UserRepository,
        private val workManager: WorkManager
    ): ViewModel() {
        val users: StateFlow<List<User>> = repository.users
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

        private var validationJob: Job? = null
        var cpf by  mutableStateOf("")
        var isErrorOnCPF by  mutableStateOf(false)
        var errorMessage by mutableStateOf<String?>(null)


        suspend fun createUser(userName: String, address: String, birthDate: String, imageUri: String, cpf: String) {
            val user = User(
                name = userName,
                address = address,
                birthDate = birthDate,
                accountId = 1,
                imageUri = imageUri, // Assumindo uma account ID fixa para simplificação
                cpf = cpf,
                isSynced = false
            )
            repository.insert(user)

            initSync()
        }

        //valida se o cpf já está cadastrado no sistema
        fun onCpfChange(newCpf: String) {
            val cleanCpf = newCpf.filter { it.isDigit() }.take(11)
            cpf = cleanCpf

            if(cleanCpf.length < 11) {
                isErrorOnCPF = false
                return
            }

            validationJob?.cancel()
            validationJob = viewModelScope.launch {
                delay(500)
                val userExists = repository.getByCpf(cleanCpf) != null
                if (userExists) {
                    isErrorOnCPF = true
                    errorMessage = "CPF já cadastrado"
                } else {
                    isErrorOnCPF = false
                    errorMessage = null
                }
            }

        }

        private fun initSync() {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<UserFhirSyncWorker>()
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniqueWork(
                "sync_novos_pacientes",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }