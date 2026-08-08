package com.GrupoD.moneymanageristg

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        private const val DATABASE_NAME = "money_manager.db"
        private const val DATABASE_VERSION = 2 // Aumentamos versión para recrear BD

        // Tabla USUARIOS
        const val TABLE_USERS = "USUARIOS"
        const val COLUMN_ID = "id_usuario"
        const val COLUMN_USER = "usuario"
        const val COLUMN_EMAIL = "correo"
        const val COLUMN_PASSWORD = "contrasena"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // ============================================
        // 1. CREAR TABLAS
        // ============================================

        // Tabla USUARIOS
        val createUsers = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER TEXT NOT NULL UNIQUE,
                $COLUMN_EMAIL TEXT,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()
        db.execSQL(createUsers)

        // Tabla CATEGORIAS
        val createCategories = """
            CREATE TABLE CATEGORIAS (
                id_categoria INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            )
        """.trimIndent()
        db.execSQL(createCategories)

        // Tabla SUBCATEGORIAS
        val createSubcategories = """
            CREATE TABLE SUBCATEGORIAS (
                id_subcategoria INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                id_categoria INTEGER NOT NULL,
                FOREIGN KEY (id_categoria) REFERENCES CATEGORIAS(id_categoria) ON DELETE CASCADE,
                UNIQUE(nombre, id_categoria)
            )
        """.trimIndent()
        db.execSQL(createSubcategories)

        // Tabla MEDIOS_DE_PAGO
        val createMedios = """
            CREATE TABLE MEDIOS_DE_PAGO (
                id_medio INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL UNIQUE
            )
        """.trimIndent()
        db.execSQL(createMedios)

        // Tabla CUENTAS_PERSONALES
        val createCuentas = """
            CREATE TABLE CUENTAS_PERSONALES (
                id_cuenta INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                saldo REAL NOT NULL DEFAULT 0,
                id_usuario INTEGER NOT NULL,
                FOREIGN KEY (id_usuario) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE,
                UNIQUE(nombre, id_usuario)
            )
        """.trimIndent()
        db.execSQL(createCuentas)

        // Tabla TRANSACCIONES
        val createTransactions = """
            CREATE TABLE TRANSACCIONES (
                id_transaccion INTEGER PRIMARY KEY AUTOINCREMENT,
                concepto TEXT NOT NULL,
                monto REAL NOT NULL CHECK(monto > 0),
                fecha TEXT NOT NULL,
                id_categoria INTEGER NOT NULL,
                id_subcategoria INTEGER,
                id_medio_pago INTEGER,
                id_cuenta_origen INTEGER,
                id_cuenta_destino INTEGER,
                tipo TEXT NOT NULL CHECK (tipo IN ('Ingreso', 'Egreso', 'Traspaso')),
                id_usuario INTEGER NOT NULL,
                FOREIGN KEY (id_categoria) REFERENCES CATEGORIAS(id_categoria) ON DELETE RESTRICT,
                FOREIGN KEY (id_subcategoria) REFERENCES SUBCATEGORIAS(id_subcategoria) ON DELETE SET NULL,
                FOREIGN KEY (id_medio_pago) REFERENCES MEDIOS_DE_PAGO(id_medio) ON DELETE SET NULL,
                FOREIGN KEY (id_cuenta_origen) REFERENCES CUENTAS_PERSONALES(id_cuenta) ON DELETE RESTRICT,
                FOREIGN KEY (id_cuenta_destino) REFERENCES CUENTAS_PERSONALES(id_cuenta) ON DELETE RESTRICT,
                FOREIGN KEY (id_usuario) REFERENCES USUARIOS(id_usuario) ON DELETE CASCADE,
                CHECK (
                    (tipo = 'Ingreso' AND id_cuenta_origen IS NULL AND id_cuenta_destino IS NOT NULL) OR
                    (tipo = 'Egreso' AND id_cuenta_origen IS NOT NULL AND id_cuenta_destino IS NULL) OR
                    (tipo = 'Traspaso' AND id_cuenta_origen IS NOT NULL AND id_cuenta_destino IS NOT NULL)
                )
            )
        """.trimIndent()
        db.execSQL(createTransactions)

        // ============================================
        // 2. INSERTAR DATOS DE PRUEBA
        // ============================================

        // Usuario admin
        val valuesUser = ContentValues().apply {
            put(COLUMN_USER, "admin")
            put(COLUMN_EMAIL, "admin@mail.com")
            put(COLUMN_PASSWORD, "1234")
        }
        val adminId = db.insert(TABLE_USERS, null, valuesUser)

        // Medios de pago
        val medios = listOf("Efectivo", "Depósito Banco", "Tarjeta Crédito", "Tarjeta Débito", "Transferencia", "Monedero Digital")
        medios.forEach { medio ->
            val values = ContentValues().apply { put("nombre", medio) }
            db.insert("MEDIOS_DE_PAGO", null, values)
        }

        // Categorías y subcategorías
        val categoriasConSub = mapOf(
            "Alimentación" to listOf("Supermercado", "Restaurante", "Comida rápida"),
            "Transporte" to listOf("Bus", "Taxi", "Gasolina"),
            "Vestimenta" to listOf("Ropa", "Calzado"),
            "Salario" to emptyList(),
            "Ahorro" to emptyList()
        )

        categoriasConSub.forEach { (categoria, subcategorias) ->
            val catValues = ContentValues().apply { put("nombre", categoria) }
            val catId = db.insert("CATEGORIAS", null, catValues)
            subcategorias.forEach { sub ->
                val subValues = ContentValues().apply {
                    put("nombre", sub)
                    put("id_categoria", catId)
                }
                db.insert("SUBCATEGORIAS", null, subValues)
            }
        }

        // Cuentas personales para admin
        val cuentas = listOf("Banco Pichincha", "Banco Guayaquil", "Chanchito")
        cuentas.forEach { cuenta ->
            val values = ContentValues().apply {
                put("nombre", cuenta)
                put("saldo", 500.0) // saldo inicial
                put("id_usuario", adminId)
            }
            db.insert("CUENTAS_PERSONALES", null, values)
        }

        // Transacciones de prueba (para admin)
        val transacciones = listOf(
            mapOf(
                "concepto" to "Compra supermercado",
                "monto" to 50.0,
                "fecha" to "2026-08-05",
                "categoria" to "Alimentación",
                "subcategoria" to "Supermercado",
                "medio" to "Efectivo",
                "cuenta_origen" to "Banco Pichincha",
                "tipo" to "Egreso"
            ),
            mapOf(
                "concepto" to "Pasaje bus",
                "monto" to 1.5,
                "fecha" to "2026-08-06",
                "categoria" to "Transporte",
                "subcategoria" to "Bus",
                "medio" to "Efectivo",
                "cuenta_origen" to "Chanchito",
                "tipo" to "Egreso"
            ),
            mapOf(
                "concepto" to "Cena restaurante",
                "monto" to 25.0,
                "fecha" to "2026-08-07",
                "categoria" to "Alimentación",
                "subcategoria" to "Restaurante",
                "medio" to "Transferencia",
                "cuenta_origen" to "Banco Guayaquil",
                "tipo" to "Egreso"
            ),
            mapOf(
                "concepto" to "Ropa",
                "monto" to 80.0,
                "fecha" to "2026-08-08",
                "categoria" to "Vestimenta",
                "subcategoria" to null,
                "medio" to "Tarjeta Crédito",
                "cuenta_origen" to "Banco Pichincha",
                "tipo" to "Egreso"
            ),
            mapOf(
                "concepto" to "Traspaso a ahorros",
                "monto" to 100.0,
                "fecha" to "2026-08-09",
                "categoria" to "Ahorro",
                "subcategoria" to null,
                "medio" to null,
                "cuenta_origen" to "Chanchito",
                "cuenta_destino" to "Banco Pichincha",
                "tipo" to "Traspaso"
            ),
            mapOf(
                "concepto" to "Sueldo",
                "monto" to 1200.0,
                "fecha" to "2026-08-01",
                "categoria" to "Salario",
                "subcategoria" to null,
                "medio" to "Transferencia",
                "cuenta_destino" to "Banco Pichincha",
                "tipo" to "Ingreso"
            )
        )

        transacciones.forEach { t ->
            val categoriaId = db.query("CATEGORIAS", arrayOf("id_categoria"), "nombre = ?", arrayOf(t["categoria"] as String), null, null, null)
                .use { cursor ->
                    if (cursor.moveToFirst()) cursor.getLong(0) else -1
                }

            val subcategoriaId = if (t["subcategoria"] != null) {
                db.query("SUBCATEGORIAS", arrayOf("id_subcategoria"), "nombre = ?", arrayOf(t["subcategoria"] as String), null, null, null)
                    .use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
            } else null

            val medioId = if (t["medio"] != null) {
                db.query("MEDIOS_DE_PAGO", arrayOf("id_medio"), "nombre = ?", arrayOf(t["medio"] as String), null, null, null)
                    .use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
            } else null

            val cuentaOrigenId = if (t["cuenta_origen"] != null) {
                db.query("CUENTAS_PERSONALES", arrayOf("id_cuenta"), "nombre = ? AND id_usuario = ?", arrayOf(t["cuenta_origen"] as String, adminId.toString()), null, null, null)
                    .use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
            } else null

            val cuentaDestinoId = if (t["cuenta_destino"] != null) {
                db.query("CUENTAS_PERSONALES", arrayOf("id_cuenta"), "nombre = ? AND id_usuario = ?", arrayOf(t["cuenta_destino"] as String, adminId.toString()), null, null, null)
                    .use { cursor ->
                        if (cursor.moveToFirst()) cursor.getLong(0) else null
                    }
            } else null

            val values = ContentValues().apply {
                put("concepto", t["concepto"] as String)
                put("monto", t["monto"] as Double)
                put("fecha", t["fecha"] as String)
                put("id_categoria", categoriaId)
                put("id_subcategoria", subcategoriaId)
                put("id_medio_pago", medioId)
                put("id_cuenta_origen", cuentaOrigenId)
                put("id_cuenta_destino", cuentaDestinoId)
                put("tipo", t["tipo"] as String)
                put("id_usuario", adminId)
            }
            db.insert("TRANSACCIONES", null, values)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS TRANSACCIONES")
        db.execSQL("DROP TABLE IF EXISTS CUENTAS_PERSONALES")
        db.execSQL("DROP TABLE IF EXISTS MEDIOS_DE_PAGO")
        db.execSQL("DROP TABLE IF EXISTS SUBCATEGORIAS")
        db.execSQL("DROP TABLE IF EXISTS CATEGORIAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // ============================================
    // MÉTODOS PARA USUARIOS (login/registro)
    // ============================================

    fun userExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USER = ?",
            arrayOf(username),
            null, null, null
        )
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun registerUser(username: String, email: String, password: String): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER, username)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun validateUser(username: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USER = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password),
            null, null, null
        )
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    fun getUserIdByUsername(username: String): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(COLUMN_ID),
            "$COLUMN_USER = ?",
            arrayOf(username),
            null, null, null
        )
        var userId = -1L
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
        }
        cursor.close()
        return userId
    }

    // ============================================
    // MÉTODOS PARA TRANSACCIONES (Home)
    // ============================================

    fun getTransactions(userId: Long): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val db = readableDatabase

        val query = """
        SELECT 
            T.id_transaccion,
            T.concepto,
            C.nombre AS categoria,
            S.nombre AS subcategoria,
            T.monto,
            T.fecha,
            T.tipo,
            MP.nombre AS medio_pago,
            T.id_cuenta_origen,
            T.id_cuenta_destino,
            CO.nombre AS cuenta_origen_nombre,
            CD.nombre AS cuenta_destino_nombre
        FROM TRANSACCIONES T
        INNER JOIN CATEGORIAS C ON T.id_categoria = C.id_categoria
        LEFT JOIN SUBCATEGORIAS S ON T.id_subcategoria = S.id_subcategoria
        LEFT JOIN MEDIOS_DE_PAGO MP ON T.id_medio_pago = MP.id_medio
        LEFT JOIN CUENTAS_PERSONALES CO ON T.id_cuenta_origen = CO.id_cuenta
        LEFT JOIN CUENTAS_PERSONALES CD ON T.id_cuenta_destino = CD.id_cuenta
        WHERE T.id_usuario = ?
        ORDER BY T.fecha DESC
    """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id_transaccion"))
            val concepto = cursor.getString(cursor.getColumnIndexOrThrow("concepto"))
            val categoria = cursor.getString(cursor.getColumnIndexOrThrow("categoria"))
            val subcategoria = cursor.getString(cursor.getColumnIndexOrThrow("subcategoria"))
            val monto = cursor.getDouble(cursor.getColumnIndexOrThrow("monto"))
            val fecha = cursor.getString(cursor.getColumnIndexOrThrow("fecha"))
            val tipo = cursor.getString(cursor.getColumnIndexOrThrow("tipo"))
            val medioPago = cursor.getString(cursor.getColumnIndexOrThrow("medio_pago"))

            val cuentaOrigen = cursor.getString(cursor.getColumnIndexOrThrow("cuenta_origen_nombre"))
            val cuentaDestino = cursor.getString(cursor.getColumnIndexOrThrow("cuenta_destino_nombre"))
            val cuenta = when {
                !cuentaOrigen.isNullOrEmpty() && !cuentaDestino.isNullOrEmpty() -> "Origen: $cuentaOrigen → Destino: $cuentaDestino"
                !cuentaOrigen.isNullOrEmpty() -> "Origen: $cuentaOrigen"
                !cuentaDestino.isNullOrEmpty() -> "Destino: $cuentaDestino"
                else -> ""
            }

            val transaction = Transaction(
                id = id,
                concepto = concepto,
                categoria = categoria,
                subcategoria = subcategoria,
                monto = monto,
                fecha = fecha,
                tipo = tipo,
                medioPago = medioPago,
                cuenta = cuenta
            )
            transactions.add(transaction)
        }
        cursor.close()
        return transactions
    }
    fun getMonthlySpent(userId: Long): Double {
        val db = readableDatabase
        val query = """
            SELECT COALESCE(SUM(monto), 0) as total
            FROM TRANSACCIONES
            WHERE id_usuario = ? 
            AND tipo = 'Egreso'
            AND strftime('%Y-%m', fecha) = strftime('%Y-%m', 'now')
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var total = 0.0
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"))
        }
        cursor.close()
        return total
    }
    fun insertTransaction(
        concepto: String,
        monto: Double,
        fecha: String,
        categoriaId: Long,
        subcategoriaId: Long?,
        medioId: Long?,
        cuentaOrigenId: Long?,
        cuentaDestinoId: Long?,
        tipo: String,
        userId: Long
    ): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("concepto", concepto)
            put("monto", monto)
            put("fecha", fecha)
            put("id_categoria", categoriaId)
            put("id_subcategoria", subcategoriaId)
            put("id_medio_pago", medioId)
            put("id_cuenta_origen", cuentaOrigenId)
            put("id_cuenta_destino", cuentaDestinoId)
            put("tipo", tipo)
            put("id_usuario", userId)
        }
        val result = db.insert("TRANSACCIONES", null, values)
        return result != -1L
    }
    // ============================================
// MÉTODOS PARA OBTENER DATOS REALES DE LA BD
// ============================================

    // Obtener todas las categorías
    fun getCategorias(): List<Pair<Long, String>> {
        val lista = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.query("CATEGORIAS", arrayOf("id_categoria", "nombre"), null, null, null, null, "nombre ASC")
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val nombre = cursor.getString(1)
            lista.add(Pair(id, nombre))
        }
        cursor.close()
        return lista
    }

    // Obtener subcategorías por categoría
    fun getSubcategorias(categoriaId: Long): List<Pair<Long, String>> {
        val lista = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.query(
            "SUBCATEGORIAS",
            arrayOf("id_subcategoria", "nombre"),
            "id_categoria = ?",
            arrayOf(categoriaId.toString()),
            null, null, "nombre ASC"
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val nombre = cursor.getString(1)
            lista.add(Pair(id, nombre))
        }
        cursor.close()
        return lista
    }

    // Obtener todos los medios de pago
    fun getMediosPago(): List<Pair<Long, String>> {
        val lista = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.query("MEDIOS_DE_PAGO", arrayOf("id_medio", "nombre"), null, null, null, null, "nombre ASC")
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val nombre = cursor.getString(1)
            lista.add(Pair(id, nombre))
        }
        cursor.close()
        return lista
    }

    // Obtener cuentas personales de un usuario
    fun getCuentasByUser(userId: Long): List<Pair<Long, String>> {
        val lista = mutableListOf<Pair<Long, String>>()
        val db = readableDatabase
        val cursor = db.query(
            "CUENTAS_PERSONALES",
            arrayOf("id_cuenta", "nombre"),
            "id_usuario = ?",
            arrayOf(userId.toString()),
            null, null, "nombre ASC"
        )
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val nombre = cursor.getString(1)
            lista.add(Pair(id, nombre))
        }
        cursor.close()
        return lista
    }
}