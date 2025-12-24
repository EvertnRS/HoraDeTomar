package br.upe.horaDeTomar.data.remote

import android.util.Log
import br.upe.horaDeTomar.data.entities.Alarm
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.data.entities.User
import br.upe.horaDeTomar.data.mapper.FhirMedicationMapper
import br.upe.horaDeTomar.data.mapper.FhirUserMapper
import ca.uhn.fhir.context.FhirContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.Patient
import javax.inject.Inject

class FhirDataSourceImpl @Inject constructor(
    private val service: FhirService
) : FhirDataSource {

    private val fhirContext = FhirContext.forR4()
    private val parser = fhirContext.newJsonParser()

    override suspend fun createPatient(user: User): String? {
        val fhirPatient = FhirUserMapper.toFhirPatient(user)

        val jsonResource = parser.encodeResourceToString(fhirPatient)

        val mediaType = "application/fhir+json".toMediaType()
        val requestBody = jsonResource.toRequestBody(mediaType)

        return try {
            val response = service.postPatient(requestBody)
            if (response.isSuccessful) {
                response.body()?.string()
            } else {
                Log.d(
                    "createPatient",
                    "Erro ao criar paciente no FHIR: ${response.errorBody()?.string()}"
                )
                null
            }
        } catch (e: Exception) {
            Log.d("createPatient", "Erro ao criar paciente no FHIR: ${e.message}")
            null
        }
    }

    override suspend fun getPatientByIdentifier(identifier: String): Patient? {
        try {
            val cleanIdentifier = identifier.filter { it.isDigit() }
            val system = "https://saude.gov.br/sid/cpf"
            val searchString = "$system|$cleanIdentifier"

            val responseBody = service.getPatientByIdentifier(searchString)
            val jsonString = responseBody.string()

            val ctx = FhirContext.forR4()
            val parser = ctx.newJsonParser()

            val bundle = parser.parseResource(Bundle::class.java, jsonString)

            if (bundle.hasEntry() && !bundle.entry.isEmpty()) {
                val resource = bundle.entryFirstRep.resource
                if (resource is Patient) {
                    return resource
                }
            }

            return null

        } catch (e: Exception) {
            Log.e("getPatientByIdentifier", "Erro ao parsear FHIR: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    override suspend fun createMedicationStatement(medication: Medication, alarms: List<Alarm>, patientFhirId: String): String? {
        val fhirStatement = FhirMedicationMapper.toFhirMedicationStatement(
            medication = medication,
            alarms = alarms,
            patientFhirId = patientFhirId
        )

        val jsonResource = parser.encodeResourceToString(fhirStatement)
        val mediaType = "application/fhir+json".toMediaType()
        val requestBody = jsonResource.toRequestBody(mediaType)

        return try {
            val response = service.postMedicationStatement(requestBody)
            if (response.isSuccessful) {
                response.body()?.string()
            } else {
                Log.d(
                    "createMedicationStatement",
                    "Erro ao criar MedicationStatement no FHIR: ${response.errorBody()?.string()}"
                )
                null
            }
        }catch (e: Exception) {
            Log.d("createMedicationStatement", "Erro ao criar MedicationStatement no FHIR: ${e.message}")
            null
        }
    }
}