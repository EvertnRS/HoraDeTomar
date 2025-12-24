package br.upe.horaDeTomar.data.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Period

@Entity(
    tableName = "medications", foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("userId"),
    ]
)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dose: String,
    val via: String,
    val userId: Int,
    val imageUri: String,
    var isSynced: Boolean = false
)
