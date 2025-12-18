package br.upe.horaDeTomar.data.remote

import android.util.Log
import br.upe.horaDeTomar.data.entities.User
import br.upe.horaDeTomar.data.mapper.FhirUserMapper
import ca.uhn.fhir.context.FhirContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class FhirDataSourceImpl @Inject constructor(
    private val service: FhirService
) : FhirDataSource {

    private val fhirContext = FhirContext.forR4()
    private val parser = fhirContext.newJsonParser()

    override suspend fun createPatient(user: User): String? {
        val fhirPatient = FhirUserMapper.toFhirPatient(user)
        Log.d("TESTE", "Criando paciente no FHIR: ${fhirPatient.name}")

        val jsonResource = parser.encodeResourceToString(fhirPatient)

        val mediaType = "application/fhir+json".toMediaType()
        val requestBody = jsonResource.toRequestBody(mediaType)

        return try {
            val response = service.postPatient(requestBody)
            if(response.isSuccessful) {
                response.body()?.string()
            } else {
                Log.d("TESTE", "Erro ao criar paciente no FHIR: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.d("TESTE", "Erro ao criar paciente no FHIR: ${e.message}")
            null
        }
    }
}