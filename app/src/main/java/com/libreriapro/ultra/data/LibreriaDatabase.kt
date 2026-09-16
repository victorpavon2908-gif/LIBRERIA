package com.libreriapro.ultra.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.libreriapro.ultra.domain.Product

/**
 * Local SQLite database of the app.
 *
 * The schema, the migrations and the transactions are explicit on purpose: the
 * project ships without annotation processing, so the shop data stays under our
 * control and upgrading never destroys existing inventory.
 *
 * Any structural change must increment [DB_VERSION] and add the matching step in
 * [onUpgrade].
 */
class LibreriaDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        CREATION_SCRIPT.forEach { db.execSQL(it) }
        seedDemoCatalog(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1 is the first released schema. Future migrations go here, for example:
        // if (oldVersion < 2) db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT NOT NULL DEFAULT ''")
    }

    companion object {
        const val DB_NAME = "libreria_ultra.db"
        const val DB_VERSION = 1

        @Volatile
        private var instance: LibreriaDatabase? = null

        fun get(context: Context): LibreriaDatabase =
            instance ?: synchronized(this) {
                instance ?: LibreriaDatabase(context).also { instance = it }
            }

        private val CREATION_SCRIPT = listOf(
            "CREATE TABLE products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "barcode TEXT NOT NULL DEFAULT ''," +
                "category TEXT NOT NULL DEFAULT ''," +
                "stock INTEGER NOT NULL DEFAULT 0," +
                "min_stock INTEGER NOT NULL DEFAULT 0," +
                "buy_price REAL NOT NULL DEFAULT 0," +
                "sell_price REAL NOT NULL DEFAULT 0)",
            "CREATE UNIQUE INDEX index_products_barcode ON products(barcode) WHERE barcode <> ''",
            "CREATE INDEX index_products_name ON products(name)",
            "CREATE TABLE sales (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date_millis INTEGER NOT NULL," +
                "payment_method TEXT NOT NULL," +
                "total REAL NOT NULL)",
            "CREATE INDEX index_sales_date ON sales(date_millis)",
            "CREATE TABLE sale_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "sale_id INTEGER NOT NULL REFERENCES sales(id) ON DELETE CASCADE," +
                "product_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "barcode TEXT NOT NULL DEFAULT ''," +
                "unit_price REAL NOT NULL," +
                "unit_cost REAL NOT NULL DEFAULT 0," +
                "qty INTEGER NOT NULL)",
            "CREATE INDEX index_sale_items_sale ON sale_items(sale_id)",
            "CREATE TABLE purchases (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date_millis INTEGER NOT NULL," +
                "supplier TEXT NOT NULL DEFAULT ''," +
                "total REAL NOT NULL)",
            "CREATE INDEX index_purchases_date ON purchases(date_millis)",
            "CREATE TABLE purchase_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "purchase_id INTEGER NOT NULL REFERENCES purchases(id) ON DELETE CASCADE," +
                "product_id INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "barcode TEXT NOT NULL DEFAULT ''," +
                "unit_cost REAL NOT NULL," +
                "qty INTEGER NOT NULL)",
            "CREATE INDEX index_purchase_items_purchase ON purchase_items(purchase_id)",
            "CREATE TABLE movements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date_millis INTEGER NOT NULL," +
                "product_id INTEGER NOT NULL," +
                "product_name TEXT NOT NULL," +
                "type TEXT NOT NULL," +
                "qty_change INTEGER NOT NULL," +
                "stock_after INTEGER NOT NULL," +
                "reference TEXT NOT NULL DEFAULT '')",
            "CREATE INDEX index_movements_date ON movements(date_millis)",
            "CREATE TABLE cash_movements (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "date_millis INTEGER NOT NULL," +
                "is_income INTEGER NOT NULL," +
                "concept TEXT NOT NULL," +
                "amount REAL NOT NULL)",
            "CREATE INDEX index_cash_date ON cash_movements(date_millis)",
        )

        /**
         * Starter catalogue of a school-supplies shop. It is inserted only once,
         * when the database file is created, so a real shop can edit or delete it
         * without it ever coming back.
         */
        private fun seedDemoCatalog(db: SQLiteDatabase) {
            demoCatalog().forEach { product ->
                db.insert("products", null, product.toValues())
            }
        }

        fun demoCatalog(): List<Product> = listOf(
            Product(0, "Cuaderno universitario", "7501234567893", "Cuadernos", 32, 5, 38.0, 55.0),
            Product(0, "Lápiz HB", "7509873214567", "Escritura", 120, 20, 4.0, 12.0),
            Product(0, "Colores 12 uds", "7504567891234", "Escritura", 8, 10, 42.0, 75.0),
            Product(0, "Borrador blanco", "7501112223334", "Escritura", 45, 10, 3.0, 8.0),
            Product(0, "Regla 30 cm", "7503334445556", "Geometría", 18, 5, 10.0, 18.0),
            Product(0, "Papel bond carta", "7509998887776", "Papelería", 15, 5, 95.0, 125.0),
        )
    }
}

internal fun Product.toValues(): ContentValues = ContentValues().apply {
    if (id > 0) put("id", id)
    put("name", name.trim())
    put("barcode", barcode.trim())
    put("category", category.trim())
    put("stock", stock)
    put("min_stock", minStock)
    put("buy_price", buyPrice)
    put("sell_price", sellPrice)
}