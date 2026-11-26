package com.cuentasclaras.app.data.local

import androidx.room.TypeConverter
import com.cuentasclaras.app.data.local.entity.EstadoFactura
import com.cuentasclaras.app.data.local.entity.TipoTransaccion

class Converters {
    @TypeConverter
    fun fromTipoTransaccion(value: TipoTransaccion): String = value.name

    @TypeConverter
    fun toTipoTransaccion(value: String): TipoTransaccion = TipoTransaccion.valueOf(value)

    @TypeConverter
    fun fromEstadoFactura(value: EstadoFactura): String = value.name

    @TypeConverter
    fun toEstadoFactura(value: String): EstadoFactura = EstadoFactura.valueOf(value)
}
