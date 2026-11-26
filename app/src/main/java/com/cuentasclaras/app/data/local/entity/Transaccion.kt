package com.cuentasclaras.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TipoTransaccion {
    INGRESO,
    GASTO,
    AJUSTE
}

@Entity(
    tableName = "transacciones",
    foreignKeys = [
        ForeignKey(
            entity = Cuenta::class,
            parentColumns = ["id"],
            childColumns = ["cuentaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Factura::class,
            parentColumns = ["id"],
            childColumns = ["facturaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["cuentaId"]),
        Index(value = ["facturaId"])
    ]
)
data class Transaccion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val cuentaId: Long,
    val facturaId: Long? = null,
    val monto: Double,
    val tipo: TipoTransaccion,
    val concepto: String? = null,
    val fecha: Long = System.currentTimeMillis()
)
