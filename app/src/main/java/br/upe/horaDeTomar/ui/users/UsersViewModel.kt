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
    import br.upe.horaDeTomar.data.remote.FhirDataSource
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
        private val workManager: WorkManager,
        private val fhirDataSource: FhirDataSource
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

        var remotePatientFound by mutableStateOf<User?>(null)
        var showRemotePatientDialog by mutableStateOf(false)
        var isCheckingRemoteCpf by mutableStateOf(false)





        suspend fun createUser(userName: String, address: String, birthDate: String, imageUri: String, cpf: String, gender: String) {
            val user = User(
                name = userName,
                address = address,
                birthDate = birthDate,
                accountId = 1,
                imageUri = imageUri, // Assumindo uma account ID fixa para simplificação
                cpf = cpf,
                isSynced = false,
                gender = gender
            )
            repository.insert(user)

            initSync()
        }

        //valida se o cpf já está cadastrado no sistema
        fun onCpfChange(newCpf: String) {
            val cleanCpf = newCpf.filter { it.isDigit() }.take(11)
            cpf = cleanCpf

            remotePatientFound = null
            showRemotePatientDialog = false

            if (cleanCpf.length < 11) {
                isErrorOnCPF = false
                errorMessage = null
                return
            }

            validationJob?.cancel()
            validationJob = viewModelScope.launch {
                delay(500)

                val userExistsLocal = repository.getByCpf(cleanCpf) != null
                if (userExistsLocal) {
                    isErrorOnCPF = true
                    errorMessage = "CPF já cadastrado localmente"
                    return@launch
                }

                isErrorOnCPF = false
                errorMessage = null

                try {
                    isCheckingRemoteCpf = true

                    val patient = fhirDataSource.getPatientByIdentifier(cleanCpf)

                    if (patient != null) {
                        remotePatientFound = User(
                            name = patient.nameFirstRep?.nameAsSingleString ?: "",
                            address = patient.addressFirstRep?.text ?: "",
                            birthDate = formatFhirDateToBrazilian(patient.birthDateElement?.asStringValue()),
                            accountId = 1,
                            imageUri = "",
                            cpf = cleanCpf,
                            isSynced = true,
                            gender = mapFhirGenderToUi(patient.gender?.toCode())
                        )
                        showRemotePatientDialog = true
                        errorMessage = "CPF já cadastrado no servidor"
                    }
                } catch (e: Exception) {
                    Log.e("UsersViewModel", "Erro ao buscar CPF no FHIR: ${e.message}")
                } finally {
                    isCheckingRemoteCpf = false
                }
            }
        }

        fun dismissRemotePatientDialog() {
            showRemotePatientDialog = false
        }

        fun consumeRemotePatient(): User? {
            showRemotePatientDialog = false
            return remotePatientFound
        }

        fun clearRemotePatient() {
            remotePatientFound = null
            showRemotePatientDialog = false
        }

        private fun formatFhirDateToBrazilian(date: String?): String {
            if (date.isNullOrBlank()) return ""
            return try {
                val parts = date.split("-")
                "${parts[2]}/${parts[1]}/${parts[0]}"
            } catch (e: Exception) {
                date
            }
        }

        private fun mapFhirGenderToUi(gender: String?): String {
            return when (gender?.lowercase()) {
                "male" -> "Masculino"
                "female" -> "Feminino"
                "other" -> "Outro"
                else -> ""
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