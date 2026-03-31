package br.upe.horaDeTomar.data.mapper

import android.util.Log
import br.upe.horaDeTomar.data.entities.Alarm
import br.upe.horaDeTomar.data.entities.Medication
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Dosage
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Medication as FhirMedication
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Timing
import java.util.Date

object FhirMedicationMapper {

    fun toFhirMedication(medication: Medication): FhirMedication {
        return FhirMedication().apply {
            status = FhirMedication.MedicationStatus.ACTIVE
            code = CodeableConcept().apply {
                text = medication.name
            }
        }
    }
    fun toFhirMedicationStatement(
        medication: Medication,
        alarms: List<Alarm>,
        patientFhirId: String,
        medicationFhirId: String?
    ): MedicationStatement {
        val statement = MedicationStatement()
        statement.status = MedicationStatement.MedicationStatementStatus.ACTIVE

        if (medicationFhirId != null) {
            val medReference = Reference("Medication/$medicationFhirId")
            medReference.display = medication.name
            statement.medication = medReference
        } else {
            val medConcept = CodeableConcept()
            medConcept.text = medication.name
            statement.medication = medConcept
        }


        statement.subject = Reference("Patient/$patientFhirId")
        statement.effective = org.hl7.fhir.r4.model.DateTimeType(Date())

        val dosageElement = Dosage()
        dosageElement.text = "${medication.dose} - ${medication.via}"

        val frequencyDescription = buildString {
            if (alarms.isEmpty()) {
                append("Nenhuma vez por dia")
            } else {
                alarms.forEach { alarm ->
                    val days = alarm.daysSelected
                        .filter { it.value }
                        .keys
                        .joinToString(", ") { it.toString() }

                    val hourFormatted = "%02d:%02d".format(
                        alarm.hour.toIntOrNull() ?: 0,
                        alarm.minute.toIntOrNull() ?: 0
                    )

                    if (days.isNotEmpty()) {
                        append("$hourFormatted ($days); ")
                    } else {
                        append("$hourFormatted; ")
                    }
                }
            }
        }.trim()

        val timing = Timing()
        timing.code = CodeableConcept().setText(frequencyDescription)
        dosageElement.timing = timing

        statement.dosage = listOf(dosageElement)

        return statement
    }
}