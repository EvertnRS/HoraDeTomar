package br.upe.horaDeTomar.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prescriptions"
)
data class Prescription(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fhirId: String,
    val patientId: Int,
    val medicationName: String,
    val dosageInstruction: String,
    val via: String,
    val startDateTime: Long,
    val isProcessed: Boolean = false
)