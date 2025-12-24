package br.upe.horaDeTomar.data.remote

import android.os.Bundle
import br.upe.horaDeTomar.data.entities.Alarm
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.data.entities.User
import org.hl7.fhir.r4.model.Patient

interface FhirDataSource {
    suspend fun createPatient(user: User): String?
    suspend fun getPatientByIdentifier(identifier: String): Patient?
    suspend fun createMedicationStatement(medication: Medication, alarms: List<Alarm>, patientFhirId: String): String?
}