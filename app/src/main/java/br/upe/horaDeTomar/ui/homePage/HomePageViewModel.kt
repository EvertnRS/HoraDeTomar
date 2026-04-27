package br.upe.horaDeTomar.ui.homePage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import br.upe.horaDeTomar.data.daos.PrescriptionDao
import br.upe.horaDeTomar.data.entities.Prescription
import br.upe.horaDeTomar.data.worker.PrescriptionFhirSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomePageViewModel @Inject constructor(
    private val prescriptionDao: PrescriptionDao,
    @ApplicationContext application: Context
) : ViewModel() {

    private val workManager = WorkManager.getInstance(application)

    val pendingPrescription = prescriptionDao.getUnprocessedPrescriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun searchPrescriptions() {
        val request = OneTimeWorkRequestBuilder<PrescriptionFhirSyncWorker>()
            .setInputData(workDataOf("is_pull_request" to true))
            .build()

        workManager.enqueue(request)
    }

    suspend fun markAsProcessed(prescription: Prescription) {
        prescriptionDao.update(prescription.copy(isProcessed = true))
    }
}