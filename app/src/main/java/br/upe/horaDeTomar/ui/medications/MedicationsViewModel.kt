package br.upe.horaDeTomar.ui.medications

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateListOf
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
import br.upe.horaDeTomar.data.entities.Alarm
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.data.manager.AlarmScheduler
import br.upe.horaDeTomar.data.remote.FhirDataSource
import br.upe.horaDeTomar.data.remote.MedicationSearchResult
import br.upe.horaDeTomar.data.repositories.AlarmRepository
import br.upe.horaDeTomar.data.repositories.MedicationRepository
import br.upe.horaDeTomar.data.worker.MedicationFhirSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationsViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val alarmRepository: AlarmRepository,
    private val alarmScheduler: AlarmScheduler,
    private val workManager: WorkManager,
    private val fhirDataSource: FhirDataSource
) : ViewModel(), AlarmActions {

    val medications: StateFlow<List<Medication>> = repository.medications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val alarmListState: StateFlow<List<Alarm>> = alarmRepository.alarmList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var medicationCreationState by mutableStateOf(
        Medication(name = "", via = "", dose = "", userId = 1, imageUri = "")
    )
        private set

    var pendingAlarms = mutableStateListOf<Alarm>()
        private set

    private var searchJob: Job? = null

    var searchResults = mutableStateListOf<MedicationSearchResult>()
        private set

    var isSearching by mutableStateOf(false)
        private set

    private var selectedFhirId: String? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun preparePendingAlarms(rep: Int, medicationId: Int? = null) {
        val mId = medicationId ?: medicationCreationState.id
        while (pendingAlarms.size < rep) pendingAlarms.add(Alarm(medicationId = mId))
        while (pendingAlarms.size > rep) pendingAlarms.removeLast()
        if (medicationId != null) {
            for (i in 0 until pendingAlarms.size) {
                pendingAlarms[i] = pendingAlarms[i].copy(medicationId = medicationId)
            }
        }
    }

    private fun String.two(): String = this.padStart(2, '0')
    fun updatePendingAlarm(index: Int, hour: String? = null, minute: String? = null, daysJson: String? = null) {
        val curr = pendingAlarms.getOrNull(index) ?: return
        pendingAlarms[index] = curr.copy(
            hour = (hour ?: curr.hour).two(),
            minute = (minute ?: curr.minute).two(),
            daysSelectedJson = daysJson ?: curr.daysSelectedJson
        )
    }

    fun clearPendingAlarms() {
        pendingAlarms.clear()
    }

    fun updateMedicationCreationState(medication: Medication) {
        Log.d("MEDICATION UPDATE", "Updating medication: ${medication.name}")
        medicationCreationState = medication
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    suspend fun createMedication() {
        Log.d("TESTE", "criando medicamento")
        val newMedicationId = repository.insert(medicationCreationState).toInt()
        medicationCreationState = medicationCreationState.copy(id = newMedicationId)
        if (pendingAlarms.isEmpty()) {
            pendingAlarms.add(Alarm(medicationId = newMedicationId, isScheduled = true))
        } else {
            preparePendingAlarms(pendingAlarms.size, medicationId = newMedicationId)
        }
        saveAllPendingAlarmsForMedication(newMedicationId)

        initSync(localId = newMedicationId, fhirId = selectedFhirId)
    }

    private suspend fun saveAllPendingAlarmsForMedication(medicationId: Int) {
        for (a in pendingAlarms) {
            val toInsert = a.copy(medicationId = medicationId, isScheduled = true)
            val id = alarmRepository.insert(toInsert).toInt()
            val saved = toInsert.copy(id = id)
            alarmScheduler.schedule(saved)
        }
        clearPendingAlarms()
    }

    var alarmCreationState by mutableStateOf(Alarm(medicationId = medicationCreationState.id))
        private set

    override fun updateAlarmCreationState(alarm: Alarm) {
        Log.d("ALARM UPDATE", "Updating alarm: ${alarm.daysSelected} ${alarm.hour}:${alarm.minute}")
        alarm.setDaysSelected(alarm.daysSelected)
        alarmCreationState = alarm
    }

    override fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarm.setDaysSelected(alarm.daysSelected)
            alarmRepository.update(alarm)
            if (alarm.isScheduled) alarmScheduler.schedule(alarm) else alarmScheduler.cancelAlarm(alarm)
        }
    }

    override fun removeAlarm(alarm: Alarm) {
        viewModelScope.launch {
            alarmRepository.delete(alarm)
            if (alarm.isScheduled) alarmScheduler.cancelAlarm(alarm)
        }
    }

    override fun saveAlarm() {
        viewModelScope.launch {
            val exists = alarmRepository.getAlarmById(alarmCreationState.id)
            if (exists != null) {
                val upd = alarmCreationState.copy(isScheduled = true)
                alarmRepository.update(upd)
                alarmScheduler.schedule(upd)
            } else {
                val id = alarmRepository.insert(alarmCreationState.copy(isScheduled = true)).toInt()
                val saved = alarmCreationState.copy(id = id)
                alarmCreationState = saved
                alarmScheduler.schedule(saved)
            }
        }
    }

    suspend fun getMedicationByAlarmId(alarmId: Int): Medication? {
        val alarm = alarmRepository.getAlarmById(alarmId) ?: return null
        return repository.getById(alarm.medicationId)
    }

    private fun initSync(localId: Int, fhirId: String?) {
        Log.d("TESTE", "initSync: $localId $fhirId")
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = androidx.work.Data.Builder()
            .putInt("local_medication_id", localId)
            .apply {
                if(fhirId != null) putString("remove_fhir_id", fhirId)
            }
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<MedicationFhirSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "sync_novos_medicamentos_$localId",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            syncRequest
        )
    }

    fun onMedicationNameChange(text: String) {
        selectedFhirId = null
        medicationCreationState = medicationCreationState.copy(name = text)

        searchJob?.cancel()

        searchJob = viewModelScope.launch {
            delay(500)
            if(text.length >= 3) {
                isSearching = true
                var results = fhirDataSource.searchMedications(text)
                searchResults.clear()
                searchResults.addAll(results)
                isSearching = false
            } else {
                searchResults.clear()
            }
        }
    }

    fun onMedicationSelected(medication: MedicationSearchResult) {
        medicationCreationState = medicationCreationState.copy(name = medication.name)
        selectedFhirId = medication.id
        searchResults.clear()
        isSearching = false
    }
}
