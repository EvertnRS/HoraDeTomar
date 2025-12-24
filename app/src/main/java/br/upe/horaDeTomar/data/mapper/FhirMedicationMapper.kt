package br.upe.horaDeTomar.data.mapper

import br.upe.horaDeTomar.data.entities.Alarm
import br.upe.horaDeTomar.data.entities.Medication
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Dosage
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Timing
import java.util.Date

object FhirMedicationMapper {
    fun toFhirMedicationStatement(medication: Medication, alarms: List<Alarm>, patientFhirId: String): MedicationStatement {
        val statement = MedicationStatement()
        statement.status = MedicationStatement.MedicationStatementStatus.ACTIVE

        val medConcept = CodeableConcept()
        medConcept.text = medication.name
        statement.medication = medConcept

        statement.subject = Reference("Patient/$patientFhirId")
        statement.effective = org.hl7.fhir.r4.model.DateTimeType(Date())

        val dosageElement = Dosage()

        dosageElement.text = "${medication.dose} - ${medication.via}"

        val frequencyDescription = buildString {
            if(alarms.isEmpty()) {
                append("Nenhuma vez por dia")
            } else {
                alarms.forEach { alarm ->
                    val days = alarm.daysSelected.filter { it.value }.keys.joinToString(", ")
                    if(days.isNotEmpty()) {
                        append("${alarm.hour}:${alarm.minute} ($days);")
                    } else {
                        append("${alarm.hour}:${alarm.minute};")
                    }
                }
            }
        }

        val timing = Timing()
        timing.code = CodeableConcept().setText(frequencyDescription)
        dosageElement.timing = timing

        statement.dosage = listOf(dosageElement)

        return statement

    }
}