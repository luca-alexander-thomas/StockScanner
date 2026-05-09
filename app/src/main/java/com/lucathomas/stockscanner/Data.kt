package com.lucathomas.stockscanner

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "data")
data class Data(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ean: String,
    val name: String,
    val brandName: String,
    val category: String,
    val image: String,
    val nutriScore: String,
    val ingredients: String,
    val nutrimentsJson: String,
    val allergens: String = "",
    val additives: String = "",
    val novaGroup: String = "unknown",
    val ecoScore: String = "unknown",
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface DataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(data: Data)

    @Update
    suspend fun update(data: Data)

    @Delete
    suspend fun delete(data: Data)

    @Query("DELETE FROM data")
    suspend fun deleteAll()

    @Query("SELECT * FROM data ORDER BY timestamp DESC")
    fun getAllDataFlow(): Flow<List<Data>>
    
    @Query("SELECT * FROM data WHERE ean = :ean LIMIT 1")
    suspend fun findByEan(ean: String): Data?
}