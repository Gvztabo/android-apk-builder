package com.cuentasclaras.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cuentasclaras.app.data.local.entity.Transaccion
import kotlinx.coroutines.flow.Flow

@Dao
interface TransaccionDao {
    @Query("SELECT * FROM transacciones WHERE cuentaId = :cuentaId ORDER BY fecha DESC, id DESC")
    fun observeTransaccionesByCuenta(cuentaId: Long): Flow<List<Transaccion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaccion(transaccion: Transaccion): Long

    @Query("DELETE FROM transacciones WHERE facturaId = :facturaId")
    suspend fun deleteByFacturaId(facturaId: Long)

    @Query("UPDATE transacciones SET concepto = :concepto WHERE facturaId = :facturaId")
    suspend fun updateConceptoByFacturaId(facturaId: Long, concepto: String)

    @Delete
    suspend fun deleteTransaccion(transaccion: Transaccion)

    @Query("DELETE FROM transacciones")
    suspend fun deleteAll()
}
