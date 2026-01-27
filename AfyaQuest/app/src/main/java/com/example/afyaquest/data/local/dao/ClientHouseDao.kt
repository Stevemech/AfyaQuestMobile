package com.example.afyaquest.data.local.dao

import androidx.room.*
import com.example.afyaquest.data.local.entity.ClientHouseEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ClientHouse operations.
 */
@Dao
interface ClientHouseDao {
    @Query("SELECT * FROM client_houses WHERE id = :clientId")
    suspend fun getClientById(clientId: String): ClientHouseEntity?

    @Query("SELECT * FROM client_houses WHERE userId = :userId")
    suspend fun getClientsByUser(userId: String): List<ClientHouseEntity>

    @Query("SELECT * FROM client_houses WHERE userId = :userId")
    fun getClientsByUserFlow(userId: String): Flow<List<ClientHouseEntity>>

    @Query("SELECT * FROM client_houses WHERE userId = :userId AND status = :status")
    suspend fun getClientsByStatus(userId: String, status: String): List<ClientHouseEntity>

    @Query("SELECT * FROM client_houses WHERE isSynced = 0")
    suspend fun getUnsyncedClients(): List<ClientHouseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientHouseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClients(clients: List<ClientHouseEntity>)

    @Update
    suspend fun updateClient(client: ClientHouseEntity)

    @Query("UPDATE client_houses SET isSynced = 1 WHERE id = :clientId")
    suspend fun markClientAsSynced(clientId: String)

    @Delete
    suspend fun deleteClient(client: ClientHouseEntity)

    @Query("DELETE FROM client_houses WHERE userId = :userId")
    suspend fun deleteClientsByUser(userId: String)
}
