package com.cuentasclaras.app.data.local.dao

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.data.local.entity.ItemFactura
import kotlinx.coroutines.flow.Flow

@Immutable
data class FacturaConItems(
    @Embedded val factura: Factura,
    @Relation(
        parentColumn = "id",
        entityColumn = "facturaId"
    )
    val items: List<ItemFactura>
)

@Dao
interface FacturaDao {
    @Transaction
    @Query("SELECT * FROM facturas ORDER BY fecha DESC, id DESC")
    fun observeFacturasConItems(): Flow<List<FacturaConItems>>

    @Transaction
    @Query("SELECT * FROM facturas WHERE id = :facturaId LIMIT 1")
    fun observeFacturaConItems(facturaId: Long): Flow<FacturaConItems?>

    @Query("SELECT COUNT(*) FROM facturas")
    fun observeInvoiceCount(): Flow<Int>

    @Query("SELECT * FROM facturas WHERE id = :facturaId LIMIT 1")
    suspend fun getFacturaById(facturaId: Long): Factura?

    @Query("SELECT COALESCE(MAX(numeroFactura), 0) FROM facturas")
    suspend fun getMaxNumeroFactura(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFactura(factura: Factura): Long

    @Transaction
    suspend fun createSequentialInvoice(fecha: Long = System.currentTimeMillis()): Long {
        val nextNumber = getMaxNumeroFactura() + 1
        return insertFactura(
            Factura(
                numeroFactura = nextNumber,
                nombre = "Factura #$nextNumber",
                fecha = fecha,
                total = 0.0,
                estado = EstadoFactura.BORRADOR
            )
        )
    }

    @Update
    suspend fun updateFactura(factura: Factura)

    @Delete
    suspend fun deleteFactura(factura: Factura)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemFactura): Long

    @Update
    suspend fun updateItem(item: ItemFactura)

    @Delete
    suspend fun deleteItem(item: ItemFactura)

    @Query("SELECT COALESCE(SUM(precioUnitario * cantidad), 0.0) FROM items_factura WHERE facturaId = :facturaId")
    suspend fun calculateFacturaTotal(facturaId: Long): Double

    @Query("UPDATE facturas SET total = :total WHERE id = :facturaId")
    suspend fun updateFacturaTotal(facturaId: Long, total: Double)

    @Query("UPDATE facturas SET estado = :estado WHERE id = :facturaId")
    suspend fun updateFacturaEstado(facturaId: Long, estado: EstadoFactura)

    @Query("UPDATE facturas SET nombre = :nombre WHERE id = :facturaId")
    suspend fun updateFacturaNombre(facturaId: Long, nombre: String)

    @Transaction
    suspend fun addItemAndRecalculate(item: ItemFactura) {
        insertItem(item)
        val total = calculateFacturaTotal(item.facturaId)
        updateFacturaTotal(item.facturaId, total)
    }

    @Transaction
    suspend fun updateItemAndRecalculate(item: ItemFactura) {
        updateItem(item)
        val total = calculateFacturaTotal(item.facturaId)
        updateFacturaTotal(item.facturaId, total)
    }

    @Transaction
    suspend fun deleteItemAndRecalculate(item: ItemFactura) {
        deleteItem(item)
        val total = calculateFacturaTotal(item.facturaId)
        updateFacturaTotal(item.facturaId, total)
    }

    @Query("DELETE FROM items_factura")
    suspend fun deleteAllItems()

    @Query("DELETE FROM facturas")
    suspend fun deleteAllFacturas()
}
