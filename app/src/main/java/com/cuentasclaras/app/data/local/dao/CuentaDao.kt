package com.cuentasclaras.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuentasclaras.app.data.local.entity.Cuenta
import kotlinx.coroutines.flow.Flow

data class CuentaConSaldo(
    @Embedded val cuenta: Cuenta,
    val saldoActual: Double
)

@Dao
interface CuentaDao {
    @Query(
        """
        SELECT c.*,
               (c.saldoInicial
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'INGRESO'), 0.0)
                - COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'GASTO'), 0.0)
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'AJUSTE'), 0.0)
               ) AS saldoActual
        FROM cuentas c
        ORDER BY c.id ASC
        """
    )
    fun observeCuentasConSaldo(): Flow<List<CuentaConSaldo>>

    @Query(
        """
        SELECT c.*,
               (c.saldoInicial
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'INGRESO'), 0.0)
                - COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'GASTO'), 0.0)
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'AJUSTE'), 0.0)
               ) AS saldoActual
        FROM cuentas c
        WHERE c.id = :cuentaId
        LIMIT 1
        """
    )
    fun observeCuentaConSaldo(cuentaId: Long): Flow<CuentaConSaldo?>

    @Query(
        """
        SELECT (c.saldoInicial
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'INGRESO'), 0.0)
                - COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'GASTO'), 0.0)
                + COALESCE((SELECT SUM(t.monto) FROM transacciones t WHERE t.cuentaId = c.id AND t.tipo = 'AJUSTE'), 0.0)
               )
        FROM cuentas c
        WHERE c.id = :cuentaId
        """
    )
    suspend fun getSaldoActual(cuentaId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCuenta(cuenta: Cuenta): Long

    @Update
    suspend fun updateCuenta(cuenta: Cuenta)

    @Delete
    suspend fun deleteCuenta(cuenta: Cuenta)

    @Query("DELETE FROM cuentas")
    suspend fun deleteAll()
}
