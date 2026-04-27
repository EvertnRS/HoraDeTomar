package br.upe.horaDeTomar.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FhirService {
    @POST("Patient")
    suspend fun postPatient(@Body body: RequestBody): Response<ResponseBody>

    @GET("Patient")
    suspend fun getPatientByIdentifier(@Query("identifier") identifierString: String): ResponseBody

    @POST("Medication")
    suspend fun postMedication(@Body body: RequestBody): Response<ResponseBody>

    @POST("MedicationStatement")
    suspend fun postMedicationStatement(@Body body: RequestBody): Response<ResponseBody>
    @GET("Medication")
    suspend fun getMedicationByCode(@Query("code:text") code: String): Response<ResponseBody>

    @GET("MedicationRequest")
    suspend fun getMedicationRequestByPatient(@Query("subject") patientReference: String?): Response<ResponseBody>
}