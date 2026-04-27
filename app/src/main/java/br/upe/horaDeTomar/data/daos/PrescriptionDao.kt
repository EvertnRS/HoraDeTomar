package br.upe.horaDeTomar.data.daos

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.data.entities.Prescription
import br.upe.horaDeTomar.data.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(prescription: Prescription): Long

    @Update
    suspend fun update(prescription: Prescription)

    @Delete
    suspend fun delete(prescription: Prescription)

    @Query("SELECT * FROM prescriptions WHERE patientId = :userId")
    suspend fun getPrescriptionsByUser(userId: Int): List<Prescription>?

    @Query("SELECT * FROM prescriptions")
    suspend fun getAllPrescriptions(): List<Prescription>?

    @Query("SELECT * FROM prescriptions WHERE id = :prescriptionId LIMIT 1")
    suspend fun getPrescriptionById(prescriptionId: Int): Prescription?

    @Query("SELECT * FROM prescriptions WHERE isProcessed = 0")
    fun getUnprocessedPrescriptions(): Flow<List<Prescription>>

    @Query("SELECT * FROM prescriptions WHERE fhirId = :fhirId LIMIT 1")
    suspend fun getByFhirId(fhirId: String): Prescription?
}