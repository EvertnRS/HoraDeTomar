package br.upe.horaDeTomar.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface FhirService {
    @POST("Patient")
    suspend fun postPatient(@Body body: RequestBody): Response<ResponseBody>
}