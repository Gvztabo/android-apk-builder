package com.cuentasclaras.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class EstadoFactura {
    BORRADOR,
    PAGADA
}

@Entity(
    tableName = "facturas",
    indices = [Index(value = ["fecha", "id"])]
)
data class Factura(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val numeroFactura: Int = 0,
    val nombre: String = "",
    val fecha: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val estado: EstadoFactura = EstadoFactura.BORRADOR
) {
    fun displayName(): String = nombre.ifBlank { "Factura #$numeroFactura" }
}
