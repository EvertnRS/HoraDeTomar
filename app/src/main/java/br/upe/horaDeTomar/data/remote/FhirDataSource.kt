package br.upe.horaDeTomar.data.remote

import br.upe.horaDeTomar.data.entities.User

interface FhirDataSource {
    suspend fun createPatient(user: User): String?
}