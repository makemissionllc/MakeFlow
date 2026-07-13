package com.makemission.makeflow.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationsTest {

    private val TEST_DB_NAME = "migration_test_v2_to_v3"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateV2ToV3_preservesData() {
        val db = helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, budgetLimit REAL NOT NULL)")
            execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, type TEXT NOT NULL, categoryId INTEGER, note TEXT, timestamp INTEGER NOT NULL, accountId INTEGER NOT NULL)")

            execSQL("INSERT INTO categories (name, budgetLimit) VALUES ('Groceries', 500.0)")
            execSQL("INSERT INTO categories (name, budgetLimit) VALUES ('Rent', 1000.0)")
            execSQL("INSERT INTO categories (name, budgetLimit) VALUES ('Fun', 0.0)")

            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp, accountId) VALUES (45.0, 'EXPENSE', 1, 'weekly shop', 1000, 1)")
            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp, accountId) VALUES (1200.0, 'EXPENSE', 2, 'rent', 2000, 1)")
            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp, accountId) VALUES (30.0, 'EXPENSE', NULL, NULL, 3000, 1)")
            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp, accountId) VALUES (200.0, 'INCOME', 1, 'gift', 4000, 1)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, AppDatabase.MIGRATION_2_3)

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Should have 3 categories", 3, cursor.getInt(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Should have 4 transactions", 4, cursor.getInt(0))
        }

        migratedDb.query("SELECT id, name, iconName, colorHex, monthlyBudget FROM categories ORDER BY name").use { cursor ->
            assertTrue("Should have first row", cursor.moveToFirst())

            assertEquals("Groceries", cursor.getString(1))
            assertEquals("", cursor.getString(2))
            assertEquals("", cursor.getString(3))
            assertEquals(500.0, cursor.getDouble(4), 0.001)

            assertTrue("Should have second row", cursor.moveToNext())
            assertEquals("Fun", cursor.getString(1))
            assertEquals(0.0, cursor.getDouble(4), 0.001)

            assertTrue("Should have third row", cursor.moveToNext())
            assertEquals("Rent", cursor.getString(1))
            assertEquals(1000.0, cursor.getDouble(4), 0.001)
        }

        migratedDb.query("SELECT t.id, t.amount, t.type, t.categoryId, t.note, t.timestamp, c.name AS catName FROM transactions t LEFT JOIN categories c ON t.categoryId = c.id ORDER BY t.timestamp").use { cursor ->
            assertTrue("Should have first transaction", cursor.moveToFirst())

            assertEquals(45.0, cursor.getDouble(1), 0.001)
            assertEquals("EXPENSE", cursor.getString(2))
            assertNotNull("categoryId should be remapped", cursor.getString(3))
            assertEquals("weekly shop", cursor.getString(4))
            assertEquals(1000L, cursor.getLong(5))
            assertEquals("Groceries", cursor.getString(6))

            assertTrue("Should have second transaction", cursor.moveToNext())
            assertEquals(1200.0, cursor.getDouble(1), 0.001)
            assertEquals("EXPENSE", cursor.getString(2))
            assertNotNull("categoryId should be remapped", cursor.getString(3))
            assertEquals("rent", cursor.getString(4))
            assertEquals("Rent", cursor.getString(6))

            assertTrue("Should have third transaction (null note, null category)", cursor.moveToNext())
            assertEquals(30.0, cursor.getDouble(1), 0.001)
            assertNull("categoryId should be null", cursor.getString(3))
            assertEquals("null note should be coalesced to empty", "", cursor.getString(4))

            assertTrue("Should have fourth transaction", cursor.moveToNext())
            assertEquals(200.0, cursor.getDouble(1), 0.001)
            assertEquals("INCOME", cursor.getString(2))
            assertNotNull("categoryId should be remapped", cursor.getString(3))
            assertEquals("gift", cursor.getString(4))
            assertEquals("Groceries", cursor.getString(6))
        }

        migratedDb.query("PRAGMA index_list('transactions')").use { cursor ->
            val indexNames = mutableListOf<String>()
            while (cursor.moveToNext()) {
                indexNames.add(cursor.getString(1))
            }
            assertTrue("index_transactions_categoryId should exist", indexNames.contains("index_transactions_categoryId"))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV13ToV14_seedsNewCategories() {
        val testDb = "migration_test_v13_to_v14"
        val db = helper.createDatabase(testDb, 13).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, parentCategoryId TEXT, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget) VALUES ('bill_parent', 'Bills', 'EXPENSE', NULL, 'receipt_long', '#26C6DA', 0.0)")
            execSQL("INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget) VALUES ('gen_parent', 'Generosity', 'EXPENSE', NULL, 'volunteer_activism', '#EC407A', 0.0)")
            execSQL("INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget) VALUES ('phone', 'Phone', 'EXPENSE', 'bill_parent', 'phone_android', '#4DD0E1', 0.0)")
            execSQL("INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget) VALUES ('gifts', 'Gifts', 'EXPENSE', 'gen_parent', 'card_giftcard', '#F06292', 0.0)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 14, true, AppDatabase.MIGRATION_13_14)

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Should have 7 categories (4 original + 3 new)", 7, cursor.getInt(0))
        }

        migratedDb.query("SELECT id FROM categories WHERE name = 'Services/Errands' AND parentCategoryId = 'bill_parent'").use { cursor ->
            assertTrue("Services/Errands must exist under Bills", cursor.moveToFirst())
        }

        migratedDb.query("SELECT id FROM categories WHERE name = 'Tithe' AND parentCategoryId = 'gen_parent'").use { cursor ->
            assertTrue("Tithe must exist under Generosity", cursor.moveToFirst())
        }

        migratedDb.query("SELECT id FROM categories WHERE name = 'Offertory' AND parentCategoryId = 'gen_parent'").use { cursor ->
            assertTrue("Offertory must exist under Generosity", cursor.moveToFirst())
        }

        migratedDb.close()
    }

    @Test
    fun categoriesSeededSynchronouslyOnFreshInstall() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        runBlocking(Dispatchers.IO) {
            DatabaseInitializer.populate(db)
        }

        val count = db.categoryDao().getCount()
        assertTrue("Categories must be seeded and queryable immediately after build() + populate()", count > 0)

        db.close()
    }
}