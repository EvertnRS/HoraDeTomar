package br.upe.horaDeTomar.data.mapper

import android.util.Log
import br.upe.horaDeTomar.data.entities.User
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.HumanName
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Patient
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

object FhirUserMapper {
    fun toFhirPatient(user: User): Patient {
        val patient = Patient()

        val parts = user.name.split(" ", limit = 2)
        val firstName = parts[0]
        val lastName = if (parts.size > 1) parts[1] else ""

        val name = HumanName()
        name.use = HumanName.NameUse.OFFICIAL
        name.addGiven(firstName)
        if (lastName.isNotEmpty()) name.family = lastName
        patient.addName(name)

        if (user.cpf.isNotBlank()) {
            val cpfIdentifier = Identifier()
            cpfIdentifier.system = "https://saude.gov.br/sid/cpf"
            cpfIdentifier.value = user.cpf.filter { it.isDigit() }
            patient.addIdentifier(cpfIdentifier)
        }

        patient.gender = when (user.gender.lowercase()) {
            "masculino", "m", "homem", "male" -> Enumerations.AdministrativeGender.MALE
            "feminino", "f", "mulher", "female" -> Enumerations.AdministrativeGender.FEMALE
            "outro", "other" -> Enumerations.AdministrativeGender.OTHER
            else -> Enumerations.AdministrativeGender.UNKNOWN
        }

        try {
            val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            val localDate = LocalDate.parse(user.birthDate, formatter)
            patient.birthDate = Date.from(
                localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
        } catch (e: Exception) {
            Log.e("toFhirPatient", "Erro ao converter data: ${user.birthDate}. Erro: ${e.message}")
        }

        Log.d("TESTE", "Address: ${user.address}")

        if (user.address.isNotBlank()) {
            val address = Address().apply {
                text = user.address
                addLine(user.address)
            }
            patient.addAddress(address)
        }

        patient.active = true

        return patient
    }
}