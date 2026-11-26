package com.cuentasclaras.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cuentasclaras.app.data.local.dao.CuentaDao
import com.cuentasclaras.app.data.local.dao.FacturaDao
import com.cuentasclaras.app.data.local.dao.TransaccionDao
import com.cuentasclaras.app.data.local.entity.Cuenta
import com.cuentasclaras.app.data.local.entity.Factura
import com.cuentasclaras.app.data.local.entity.ItemFactura
import com.cuentasclaras.app.data.local.entity.Transaccion

@Database(
    entities = [
        Cuenta::class,
        Transaccion::class,
        Factura::class,
        ItemFactura::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cuentaDao(): CuentaDao
    abstract fun transaccionDao(): TransaccionDao
    abstract fun facturaDao(): FacturaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE facturas ADD COLUMN numeroFactura INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE facturas SET numeroFactura = id WHERE numeroFactura = 0")
                db.execSQL("ALTER TABLE transacciones ADD COLUMN facturaId INTEGER DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_facturaId ON transacciones(facturaId)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE facturas ADD COLUMN nombre TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE facturas SET nombre = 'Factura #' || numeroFactura WHERE nombre = ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_cuentaId_tipo ON transacciones(cuentaId, tipo)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transacciones_cuentaId_fecha_id ON transacciones(cuentaId, fecha, id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_facturas_fecha_id ON facturas(fecha, id)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cuentas_claras.db"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
