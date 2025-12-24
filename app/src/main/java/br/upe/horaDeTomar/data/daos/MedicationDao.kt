package br.upe.horaDeTomar.data.daos

import android.net.Uri
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.upe.horaDeTomar.data.entities.Medication
import br.upe.horaDeTomar.data.entities.User
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication)

    @Delete
    suspend fun delete(medication: Medication)

    @Query("SELECT * FROM medications WHERE userId = :userId")
    suspend fun getMedicationsByUser(userId: Int): List<Medication>?

    @Query("SELECT * FROM medications")
    suspend fun getAllMedications(): List<Medication>?

    @Query("SELECT * FROM medications WHERE id = :medicationId LIMIT 1")
    suspend fun getMedicationById(medicationId: Int): Medication?

    @Query("SELECT * FROM medications")
    fun getMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE isSynced = 0")
    suspend fun getUnsyncedMedications(): List<Medication>

    @Query("SELECT imageUri FROM medications WHERE id = :medicationId LIMIT 1")
    fun getMedicationImage(medicationId: Int): String
}