package com.cuentasclaras.app.data.local.dao

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cuentasclaras.app.data.local.entity.Cuenta
import kotlinx.coroutines.flow.Flow

@Immutable
data class CuentaConSaldo(
    @Embedded val cuenta: Cuenta,
    val saldoActual: Double,
    val transaccionesCount: Int = 0
)

@Dao
interface CuentaDao {
    @Query(
        """
        SELECT c.*,
               (c.saldoInicial + COALESCE(agg.netDelta, 0.0)) AS saldoActual,
               COALESCE(agg.txCount, 0) AS transaccionesCount
        FROM cuentas c
        LEFT JOIN (
            SELECT cuentaId,
                   COUNT(*) AS txCount,
                   SUM(
                       CASE tipo
                           WHEN 'INGRESO' THEN monto
                           WHEN 'GASTO' THEN -monto
                           WHEN 'AJUSTE' THEN monto
                           ELSE 0.0
                       END
                   ) AS netDelta
            FROM transacciones
            GROUP BY cuentaId
        ) agg ON agg.cuentaId = c.id
        ORDER BY c.id ASC
        """
    )
    fun observeCuentasConSaldo(): Flow<List<CuentaConSaldo>>

    @Query(
        """
        SELECT c.*,
               (c.saldoInicial + COALESCE(agg.netDelta, 0.0)) AS saldoActual,
               COALESCE(agg.txCount, 0) AS transaccionesCount
        FROM cuentas c
        LEFT JOIN (
            SELECT cuentaId,
                   COUNT(*) AS txCount,
                   SUM(
                       CASE tipo
                           WHEN 'INGRESO' THEN monto
                           WHEN 'GASTO' THEN -monto
                           WHEN 'AJUSTE' THEN monto
                           ELSE 0.0
                       END
                   ) AS netDelta
            FROM transacciones
            WHERE cuentaId = :cuentaId
            GROUP BY cuentaId
        ) agg ON agg.cuentaId = c.id
        WHERE c.id = :cuentaId
        LIMIT 1
        """
    )
    fun observeCuentaConSaldo(cuentaId: Long): Flow<CuentaConSaldo?>

    @Query("SELECT COUNT(*) FROM cuentas")
    fun observeAccountCount(): Flow<Int>

    @Query(
        """
        SELECT (c.saldoInicial + COALESCE((
            SELECT SUM(
                CASE tipo
                    WHEN 'INGRESO' THEN monto
                    WHEN 'GASTO' THEN -monto
                    WHEN 'AJUSTE' THEN monto
                    ELSE 0.0
                END
            )
            FROM transacciones t
            WHERE t.cuentaId = c.id
        ), 0.0))
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
