package br.upe.horaDeTomar.data.mapper

import br.upe.horaDeTomar.data.entities.Prescription
import okhttp3.ResponseBody
import org.hl7.fhir.r4.model.MedicationRequest
import org.json.JSONObject

object FhirPrescriptionMapper {


    fun toPrescription(fhirRequest: MedicationRequest, patientLocalId: Int): Prescription {
        val fhirId = fhirRequest.idElement.idPart ?: ""

        val medicationName = fhirRequest.medicationCodeableConcept?.text
            ?: fhirRequest.medicationCodeableConcept?.codingFirstRep?.display
            ?: "Medicamento Desconhecido"

        val dose = if (fhirRequest.hasDosageInstruction()) {
            fhirRequest.dosageInstructionFirstRep.text ?: ""
        } else {
            ""
        }

        val frequency = if (fhirRequest.hasDosageInstruction()) {
            fhirRequest.dosageInstructionFirstRep.timing?.code?.text ?: ""
        } else {
            ""
        }

        val fullInstruction = when {
            dose.isNotEmpty() && frequency.isNotEmpty() -> "$dose - $frequency"
            dose.isNotEmpty() -> dose
            frequency.isNotEmpty() -> frequency
            else -> "Sem instruções"
        }
        
        val via = if (fhirRequest.hasDosageInstruction()) {
            val route = fhirRequest.dosageInstructionFirstRep.route
            route?.text ?: route?.codingFirstRep?.display ?: "Oral"
        } else {
            "Oral"
        }

        return Prescription(
            fhirId = fhirId,
            patientId = patientLocalId,
            medicationName = medicationName,
            dosageInstruction = fullInstruction,
            via = via,
            startDateTime = System.currentTimeMillis(),
            isProcessed = false
        )
    }

}