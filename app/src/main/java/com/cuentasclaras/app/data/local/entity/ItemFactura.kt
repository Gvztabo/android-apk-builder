package com.cuentasclaras.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items_factura",
    foreignKeys = [
        ForeignKey(
            entity = Factura::class,
            parentColumns = ["id"],
            childColumns = ["facturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["facturaId"])]
)
data class ItemFactura(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val facturaId: Long,
    val nombre: String? = null,
    val precioUnitario: Double,
    val cantidad: Int
)
