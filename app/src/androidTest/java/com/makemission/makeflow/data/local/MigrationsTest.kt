package com.makemission.makeflow.data.local

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
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

    @get:Rule
    private val helper = InstrumentedMigrationHelper(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun migrateV20ToV21_addsRateBaseMetadataAndPreservesCachedRates() {
        val testDb = "migration_test_v20_to_v21_rates"
        helper.createDatabase(testDb, 20).apply {
            execSQL("CREATE TABLE exchange_rates (currencyCode TEXT PRIMARY KEY NOT NULL, rateToDisplayCurrency REAL NOT NULL, lastUpdated INTEGER NOT NULL)")
            execSQL("INSERT INTO exchange_rates (currencyCode, rateToDisplayCurrency, lastUpdated) VALUES ('USD', 0.0077, 1234)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 21, true, AppDatabase.MIGRATION_20_21)
        migratedDb.query("PRAGMA table_info('exchange_rates')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) columns += cursor.getString(1)
            assertTrue(columns.contains("baseCurrencyCode"))
            assertTrue(columns.contains("rateDate"))
        }
        migratedDb.query("SELECT currencyCode, rateToDisplayCurrency, lastUpdated FROM exchange_rates").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("USD", cursor.getString(0))
            assertEquals(0.0077, cursor.getDouble(1), 0.000001)
            assertEquals(1234L, cursor.getLong(2))
        }
        migratedDb.close()
    }

    @Test
    fun migrateV21ToV22_addsOptionalTransferDestinationAmount() {
        val testDb = "migration_test_v21_to_v22_transfer_amount"
        helper.createDatabase(testDb, 21).apply {
            execSQL("CREATE TABLE transactions (id TEXT PRIMARY KEY NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, categoryId TEXT, accountId TEXT NOT NULL DEFAULT '', fromAccountId TEXT, toAccountId TEXT, note TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL, externalReference TEXT)")
            execSQL("INSERT INTO transactions (id, amount, type, accountId, note, timestamp) VALUES ('tx1', 10.0, 'EXPENSE', 'acct1', '', 1234)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 22, true, AppDatabase.MIGRATION_21_22)
        migratedDb.query("PRAGMA table_info('transactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) columns += cursor.getString(1)
            assertTrue(columns.contains("destinationAmount"))
        }
        migratedDb.query("SELECT id, amount, destinationAmount FROM transactions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("tx1", cursor.getString(0))
            assertEquals(10.0, cursor.getDouble(1), 0.001)
            assertTrue(cursor.isNull(2))
        }
        migratedDb.close()
    }

    // ── Existing tests (kept as-is) ───────────────────────────────────

    @Test
    fun migrateV2ToV3_preservesData() {
        val testDb = "migration_test_v2_to_v3"
        val db = helper.createDatabase(testDb, 2).apply {
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

        val migratedDb = helper.runMigrationsAndValidate(testDb, 3, true, AppDatabase.MIGRATION_2_3)

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

            assertEquals("Fun", cursor.getString(1))
            assertEquals("", cursor.getString(2))
            assertEquals("", cursor.getString(3))
            assertEquals(0.0, cursor.getDouble(4), 0.001)

            assertTrue("Should have second row", cursor.moveToNext())
            assertEquals("Groceries", cursor.getString(1))
            assertEquals(500.0, cursor.getDouble(4), 0.001)

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
    fun migrateV17ToV18_createsCorrectionsAndMemoryTables() {
        val testDb = "migration_test_v17_to_v18"
        val db = helper.createDatabase(testDb, 17).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, parentCategoryId TEXT, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget) VALUES ('cat1', 'Fuel', 'EXPENSE', NULL, 'local_gas_station', '#EF5350', 0.0)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 18, true, AppDatabase.MIGRATION_17_18)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='corrections'").use { cursor ->
            assertTrue("corrections table must exist after migration 17->18", cursor.moveToFirst())
        }

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='correction_memory'").use { cursor ->
            assertTrue("correction_memory table must exist after migration 17->18", cursor.moveToFirst())
        }

        migratedDb.query("SELECT COUNT(*) FROM corrections").use { cursor ->
            cursor.moveToFirst()
            assertEquals("corrections should be empty after migration", 0, cursor.getInt(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM correction_memory").use { cursor ->
            cursor.moveToFirst()
            assertEquals("correction_memory should be empty after migration", 0, cursor.getInt(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    // ════════════════════════════════════════════════════════════════════
    //  NEW: 14 untested migrations
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun migrateV1ToV2_createsTasksTable() {
        val testDb = "migration_test_v1_to_v2"
        helper.createDatabase(testDb, 1).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, budgetLimit REAL NOT NULL)")
            execSQL("INSERT INTO categories (id, name, budgetLimit) VALUES ('cat1', 'Groceries', 500.0)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 2, true, AppDatabase.MIGRATION_1_2)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='tasks'").use { cursor ->
            assertTrue("tasks table must exist after migration 1->2", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('tasks')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("tasks must have id column", columns.contains("id"))
            assertTrue("tasks must have title column", columns.contains("title"))
            assertTrue("tasks must have description column", columns.contains("description"))
            assertTrue("tasks must have isCompleted column", columns.contains("isCompleted"))
            assertTrue("tasks must have categoryId column", columns.contains("categoryId"))
            assertTrue("tasks must have timestamp column", columns.contains("timestamp"))
        }

        migratedDb.query("SELECT COUNT(*) FROM tasks").use { cursor ->
            cursor.moveToFirst()
            assertEquals("tasks should be empty after fresh migration", 0, cursor.getInt(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing categories must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV3ToV4_createsBudgetsTable() {
        val testDb = "migration_test_v3_to_v4"
        helper.createDatabase(testDb, 3).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name, iconName, colorHex, monthlyBudget) VALUES ('cat_a', 'Food', 'restaurant', '#FF5722', 0.0)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 4, true, AppDatabase.MIGRATION_3_4)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='budgets'").use { cursor ->
            assertTrue("budgets table must exist after migration 3->4", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('budgets')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("budgets must have id", columns.contains("id"))
            assertTrue("budgets must have categoryId", columns.contains("categoryId"))
            assertTrue("budgets must have categoryName", columns.contains("categoryName"))
            assertTrue("budgets must have amount", columns.contains("amount"))
            assertTrue("budgets must have spentAmount", columns.contains("spentAmount"))
            assertTrue("budgets must have period", columns.contains("period"))
            assertTrue("budgets must have createdAt", columns.contains("createdAt"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing categories must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV4ToV5_addsAccountIdToTransactions() {
        val testDb = "migration_test_v4_to_v5"
        helper.createDatabase(testDb, 4).apply {
            execSQL("CREATE TABLE IF NOT EXISTS transactions (id TEXT PRIMARY KEY NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, categoryId TEXT, note TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL)")
            execSQL("INSERT INTO transactions (id, amount, type, categoryId, note, timestamp) VALUES ('tx1', 25.0, 'EXPENSE', 'cat1', 'lunch', 1000)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 5, true, AppDatabase.MIGRATION_4_5)

        migratedDb.query("PRAGMA table_info('transactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("transactions must have accountId column after 4->5", columns.contains("accountId"))
        }

        migratedDb.query("SELECT accountId FROM transactions WHERE id = 'tx1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing row must have default empty accountId", "", cursor.getString(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV5ToV6_createsRecurringPatternsTable() {
        val testDb = "migration_test_v5_to_v6"
        helper.createDatabase(testDb, 5).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Transport')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 6, true, AppDatabase.MIGRATION_5_6)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recurring_patterns'").use { cursor ->
            assertTrue("recurring_patterns table must exist after 5->6", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('recurring_patterns')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have normalizedDescription", columns.contains("normalizedDescription"))
            assertTrue("must have avgAmount", columns.contains("avgAmount"))
            assertTrue("must have intervalDays", columns.contains("intervalDays"))
            assertTrue("must have lastSeenDate", columns.contains("lastSeenDate"))
            assertTrue("must have nextExpectedDate", columns.contains("nextExpectedDate"))
            assertTrue("must have confidence", columns.contains("confidence"))
            assertTrue("must have categoryId", columns.contains("categoryId"))
            assertTrue("must have transactionType", columns.contains("transactionType"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV6ToV7_createsCardInteractionsTable() {
        val testDb = "migration_test_v6_to_v7"
        helper.createDatabase(testDb, 6).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Utilities')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 7, true, AppDatabase.MIGRATION_6_7)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='card_interactions'").use { cursor ->
            assertTrue("card_interactions table must exist after 6->7", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('card_interactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have cardType", columns.contains("cardType"))
            assertTrue("must have hourOfDay", columns.contains("hourOfDay"))
            assertTrue("must have timestamp", columns.contains("timestamp"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV7ToV8_createsQueuedNotificationsTable() {
        val testDb = "migration_test_v7_to_v8"
        helper.createDatabase(testDb, 7).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Health')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 8, true, AppDatabase.MIGRATION_7_8)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='queued_notifications'").use { cursor ->
            assertTrue("queued_notifications table must exist after 7->8", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('queued_notifications')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have type", columns.contains("type"))
            assertTrue("must have title", columns.contains("title"))
            assertTrue("must have body", columns.contains("body"))
            assertTrue("must have createdAt", columns.contains("createdAt"))
            assertTrue("must have payload", columns.contains("payload"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV8ToV9_addsLastNotifiedNextDateToRecurringPatterns() {
        val testDb = "migration_test_v8_to_v9"
        helper.createDatabase(testDb, 8).apply {
            execSQL("""CREATE TABLE IF NOT EXISTS recurring_patterns (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                normalizedDescription TEXT NOT NULL,
                avgAmount REAL NOT NULL,
                intervalDays INTEGER NOT NULL,
                lastSeenDate INTEGER NOT NULL,
                nextExpectedDate INTEGER NOT NULL,
                confidence REAL NOT NULL,
                categoryId TEXT,
                transactionType TEXT NOT NULL
            )""")
            execSQL("INSERT INTO recurring_patterns (normalizedDescription, avgAmount, intervalDays, lastSeenDate, nextExpectedDate, confidence, transactionType) VALUES ('salary', 50000.0, 30, 1000, 2000, 0.95, 'INCOME')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 9, true, AppDatabase.MIGRATION_8_9)

        migratedDb.query("PRAGMA table_info('recurring_patterns')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have lastNotifiedNextDate after 8->9", columns.contains("lastNotifiedNextDate"))
        }

        migratedDb.query("SELECT lastNotifiedNextDate FROM recurring_patterns WHERE normalizedDescription = 'salary'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing row must have default 0", 0L, cursor.getLong(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM recurring_patterns").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV9ToV10_addsFromAccountIdAndToAccountId() {
        val testDb = "migration_test_v9_to_v10"
        helper.createDatabase(testDb, 9).apply {
            execSQL("CREATE TABLE IF NOT EXISTS transactions (id TEXT PRIMARY KEY NOT NULL, amount REAL NOT NULL, type TEXT NOT NULL, categoryId TEXT, accountId TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL)")
            execSQL("INSERT INTO transactions (id, amount, type, note, timestamp, accountId) VALUES ('tx1', 100.0, 'INCOME', 'freelance', 1000, 'acct1')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 10, true, AppDatabase.MIGRATION_9_10)

        migratedDb.query("PRAGMA table_info('transactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have fromAccountId after 9->10", columns.contains("fromAccountId"))
            assertTrue("must have toAccountId after 9->10", columns.contains("toAccountId"))
        }

        migratedDb.query("SELECT fromAccountId, toAccountId FROM transactions WHERE id = 'tx1'").use { cursor ->
            cursor.moveToFirst()
            assertNull("fromAccountId defaults to null", cursor.getString(0))
            assertNull("toAccountId defaults to null", cursor.getString(1))
        }

        migratedDb.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data must be preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV10ToV11_createsRecurringTransactionsTable() {
        val testDb = "migration_test_v10_to_v11"
        helper.createDatabase(testDb, 10).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Rent')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 11, true, AppDatabase.MIGRATION_10_11)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='recurring_transactions'").use { cursor ->
            assertTrue("recurring_transactions table must exist after 10->11", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('recurring_transactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have description", columns.contains("description"))
            assertTrue("must have amount", columns.contains("amount"))
            assertTrue("must have type", columns.contains("type"))
            assertTrue("must have categoryId", columns.contains("categoryId"))
            assertTrue("must have accountId", columns.contains("accountId"))
            assertTrue("must have frequency", columns.contains("frequency"))
            assertTrue("must have intervalDays", columns.contains("intervalDays"))
            assertTrue("must have startDate", columns.contains("startDate"))
            assertTrue("must have endDate", columns.contains("endDate"))
            assertTrue("must have nextDueDate", columns.contains("nextDueDate"))
            assertTrue("must have autoConfirm", columns.contains("autoConfirm"))
            assertTrue("must have isActive", columns.contains("isActive"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV11ToV12_createsTransactionTemplatesTable() {
        val testDb = "migration_test_v11_to_v12"
        helper.createDatabase(testDb, 11).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Shopping')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 12, true, AppDatabase.MIGRATION_11_12)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='transaction_templates'").use { cursor ->
            assertTrue("transaction_templates table must exist after 11->12", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('transaction_templates')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have name", columns.contains("name"))
            assertTrue("must have amount", columns.contains("amount"))
            assertTrue("must have type", columns.contains("type"))
            assertTrue("must have categoryId", columns.contains("categoryId"))
            assertTrue("must have accountId", columns.contains("accountId"))
            assertTrue("must have note", columns.contains("note"))
            assertTrue("must have iconOrColor", columns.contains("iconOrColor"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV12ToV13_addsCurrencyCodeAndExchangeRates() {
        val testDb = "migration_test_v12_to_v13"
        helper.createDatabase(testDb, 12).apply {
            execSQL("CREATE TABLE IF NOT EXISTS accounts (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, balance REAL NOT NULL, initialBalance REAL NOT NULL, colorHex TEXT NOT NULL)")
            execSQL("INSERT INTO accounts (id, name, balance, initialBalance, colorHex) VALUES ('acct1', 'Cash', 1000.0, 1000.0, '#4CAF50')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 13, true, AppDatabase.MIGRATION_12_13)

        migratedDb.query("PRAGMA table_info('accounts')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("accounts must have currencyCode after 12->13", columns.contains("currencyCode"))
        }

        migratedDb.query("SELECT currencyCode FROM accounts WHERE id = 'acct1'").use { cursor ->
            cursor.moveToFirst()
            assertNull("existing account currencyCode defaults to null", cursor.getString(0))
        }

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='exchange_rates'").use { cursor ->
            assertTrue("exchange_rates table must exist after 12->13", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('exchange_rates')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have currencyCode", columns.contains("currencyCode"))
            assertTrue("must have rateToDisplayCurrency", columns.contains("rateToDisplayCurrency"))
            assertTrue("must have lastUpdated", columns.contains("lastUpdated"))
        }

        migratedDb.query("SELECT COUNT(*) FROM accounts").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing account data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV14ToV15_addsNameToBudgets() {
        val testDb = "migration_test_v14_to_v15"
        helper.createDatabase(testDb, 14).apply {
            execSQL("""CREATE TABLE IF NOT EXISTS budgets (
                id TEXT PRIMARY KEY NOT NULL,
                categoryId TEXT NOT NULL,
                categoryName TEXT NOT NULL,
                amount REAL NOT NULL,
                spentAmount REAL NOT NULL,
                period TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )""")
            execSQL("INSERT INTO budgets (id, categoryId, categoryName, amount, spentAmount, period, createdAt) VALUES ('b1', 'cat1', 'Groceries', 500.0, 200.0, 'Monthly', 1000)")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 15, true, AppDatabase.MIGRATION_14_15)

        migratedDb.query("PRAGMA table_info('budgets')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("budgets must have name column after 14->15", columns.contains("name"))
        }

        migratedDb.query("SELECT name FROM budgets WHERE id = 'b1'").use { cursor ->
            cursor.moveToFirst()
            assertNull("existing budget name defaults to null", cursor.getString(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM budgets").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing budgets preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV15ToV16_addsExternalReferenceToTransactions() {
        val testDb = "migration_test_v15_to_v16"
        helper.createDatabase(testDb, 15).apply {
            execSQL("""CREATE TABLE IF NOT EXISTS transactions (
                id TEXT PRIMARY KEY NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                categoryId TEXT,
                accountId TEXT NOT NULL DEFAULT '',
                fromAccountId TEXT,
                toAccountId TEXT,
                note TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL
            )""")
            execSQL("INSERT INTO transactions (id, amount, type, note, timestamp, accountId) VALUES ('tx1', 2500.0, 'INCOME', 'salary', 1000, 'acct1')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 16, true, AppDatabase.MIGRATION_15_16)

        migratedDb.query("PRAGMA table_info('transactions')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("transactions must have externalReference after 15->16", columns.contains("externalReference"))
        }

        migratedDb.query("SELECT externalReference FROM transactions WHERE id = 'tx1'").use { cursor ->
            cursor.moveToFirst()
            assertNull("existing transaction externalReference defaults to null", cursor.getString(0))
        }

        migratedDb.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing transactions preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    @Test
    fun migrateV16ToV17_createsSavingsGoalsTable() {
        val testDb = "migration_test_v16_to_v17"
        helper.createDatabase(testDb, 16).apply {
            execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, iconName TEXT NOT NULL DEFAULT '', colorHex TEXT NOT NULL DEFAULT '', monthlyBudget REAL NOT NULL DEFAULT 0.0)")
            execSQL("INSERT INTO categories (id, name) VALUES ('cat1', 'Travel')")
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(testDb, 17, true, AppDatabase.MIGRATION_16_17)

        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name='savings_goals'").use { cursor ->
            assertTrue("savings_goals table must exist after 16->17", cursor.moveToFirst())
        }

        migratedDb.query("PRAGMA table_info('savings_goals')").use { cursor ->
            val columns = mutableListOf<String>()
            while (cursor.moveToNext()) {
                columns.add(cursor.getString(1))
            }
            assertTrue("must have id", columns.contains("id"))
            assertTrue("must have name", columns.contains("name"))
            assertTrue("must have targetAmount", columns.contains("targetAmount"))
            assertTrue("must have targetDate", columns.contains("targetDate"))
            assertTrue("must have linkedAccountId", columns.contains("linkedAccountId"))
            assertTrue("must have createdDate", columns.contains("createdDate"))
            assertTrue("must have status", columns.contains("status"))
        }

        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst()
            assertEquals("existing data preserved", 1, cursor.getInt(0))
        }

        migratedDb.close()
    }

    // ════════════════════════════════════════════════════════════════════
    //  END-TO-END: v1 -> v18 sequential chain
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun endToEnd_v1ThroughV18_producesCompleteSchema() {
        val testDb = "migration_test_v1_to_v18_chain"

        // Create v1 with accounts, categories (INTEGER ids), transactions (INTEGER ids)
        helper.createDatabase(testDb, 1).apply {
            execSQL("CREATE TABLE IF NOT EXISTS accounts (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, balance REAL NOT NULL, initialBalance REAL NOT NULL, colorHex TEXT NOT NULL)")
            execSQL("CREATE TABLE IF NOT EXISTS categories (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, budgetLimit REAL NOT NULL)")
            execSQL("CREATE TABLE IF NOT EXISTS transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, type TEXT NOT NULL, categoryId INTEGER, note TEXT, timestamp INTEGER NOT NULL)")

            execSQL("INSERT INTO accounts (id, name, balance, initialBalance, colorHex) VALUES ('acct1', 'Cash', 5000.0, 5000.0, '#4CAF50')")
            execSQL("INSERT INTO accounts (id, name, balance, initialBalance, colorHex) VALUES ('acct2', 'Bank', 10000.0, 10000.0, '#2196F3')")
            execSQL("INSERT INTO categories (name, budgetLimit) VALUES ('Food', 300.0)")
            execSQL("INSERT INTO categories (name, budgetLimit) VALUES ('Transport', 200.0)")
            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp) VALUES (45.0, 'EXPENSE', 1, 'groceries', 1000)")
            execSQL("INSERT INTO transactions (amount, type, categoryId, note, timestamp) VALUES (5000.0, 'INCOME', NULL, 'salary', 2000)")
            close()
        }

        // Apply ALL 17 migrations sequentially
        val migratedDb = helper.runMigrationsAndValidate(
            testDb, 18, true,
            AppDatabase.MIGRATION_1_2,
            AppDatabase.MIGRATION_2_3,
            AppDatabase.MIGRATION_3_4,
            AppDatabase.MIGRATION_4_5,
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_6_7,
            AppDatabase.MIGRATION_7_8,
            AppDatabase.MIGRATION_8_9,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_10_11,
            AppDatabase.MIGRATION_11_12,
            AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16,
            AppDatabase.MIGRATION_16_17,
            AppDatabase.MIGRATION_17_18
        )

        // ── Verify all 14 expected tables exist ──
        val expectedTables = listOf(
            "accounts", "categories", "transactions", "tasks", "budgets",
            "recurring_patterns", "card_interactions", "queued_notifications",
            "recurring_transactions", "transaction_templates", "exchange_rates",
            "savings_goals", "corrections", "correction_memory"
        )
        migratedDb.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata' ORDER BY name").use { cursor ->
            val actualTables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                actualTables.add(cursor.getString(0))
            }
            for (table in expectedTables) {
                assertTrue("Expected table '$table' to exist after full chain migration", actualTables.contains(table))
            }
            assertEquals("Must have exactly $expectedTables tables", expectedTables.size, actualTables.size)
        }

        // ── Verify accounts table columns ──
        val accountCols = listOf("id", "name", "balance", "initialBalance", "colorHex", "currencyCode")
        migratedDb.query("PRAGMA table_info('accounts')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in accountCols) assertTrue("accounts must have '$col'", cols.contains(col))
        }

        // ── Verify categories table columns ──
        migratedDb.query("PRAGMA table_info('categories')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "name", "iconName", "colorHex", "monthlyBudget", "type", "parentCategoryId"))
                assertTrue("categories must have '$col'", cols.contains(col))
        }

        // ── Verify transactions table columns + FK + index ──
        migratedDb.query("PRAGMA table_info('transactions')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "amount", "type", "categoryId", "accountId", "fromAccountId", "toAccountId", "note", "timestamp", "externalReference"))
                assertTrue("transactions must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA index_list('transactions')").use { cursor ->
            val idxs = mutableListOf<String>()
            while (cursor.moveToNext()) { idxs.add(cursor.getString(1)) }
            assertTrue("transactions must have index on categoryId", idxs.any { it.contains("categoryId") })
        }

        migratedDb.query("PRAGMA foreign_key_list('transactions')").use { cursor ->
            assertTrue("transactions must have foreign key on categoryId", cursor.moveToFirst())
        }

        // ── Verify tasks, budgets, recurring_patterns, etc. full column lists ──
        migratedDb.query("PRAGMA table_info('tasks')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "title", "description", "isCompleted", "categoryId", "timestamp"))
                assertTrue("tasks must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('budgets')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "categoryId", "categoryName", "name", "amount", "spentAmount", "period", "createdAt"))
                assertTrue("budgets must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('recurring_patterns')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "normalizedDescription", "avgAmount", "intervalDays", "lastSeenDate", "nextExpectedDate", "confidence", "categoryId", "transactionType", "lastNotifiedNextDate"))
                assertTrue("recurring_patterns must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('card_interactions')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "cardType", "hourOfDay", "timestamp"))
                assertTrue("card_interactions must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('queued_notifications')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "type", "title", "body", "createdAt", "payload"))
                assertTrue("queued_notifications must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('recurring_transactions')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "description", "amount", "type", "categoryId", "accountId", "frequency", "intervalDays", "startDate", "endDate", "nextDueDate", "autoConfirm", "isActive"))
                assertTrue("recurring_transactions must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('transaction_templates')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "name", "amount", "type", "categoryId", "accountId", "note", "iconOrColor"))
                assertTrue("transaction_templates must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('exchange_rates')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("currencyCode", "rateToDisplayCurrency", "lastUpdated"))
                assertTrue("exchange_rates must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('savings_goals')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "name", "targetAmount", "targetDate", "linkedAccountId", "createdDate", "status"))
                assertTrue("savings_goals must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('corrections')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("id", "description", "correctedLabelName", "correctedLabelIndex", "originalSuggestedLabelName", "isTrained", "createdAt"))
                assertTrue("corrections must have '$col'", cols.contains(col))
        }

        migratedDb.query("PRAGMA table_info('correction_memory')").use { cursor ->
            val cols = mutableListOf<String>()
            while (cursor.moveToNext()) { cols.add(cursor.getString(1)) }
            for (col in listOf("normalizedDescription", "categoryId", "updatedAt"))
                assertTrue("correction_memory must have '$col'", cols.contains(col))
        }

        // ── Verify data survived the full chain ──
        migratedDb.query("SELECT COUNT(*) FROM accounts").use { cursor ->
            cursor.moveToFirst(); assertEquals("accounts data preserved", 2, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM categories").use { cursor ->
            cursor.moveToFirst(); assertEquals("categories data preserved", 2, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM transactions").use { cursor ->
            cursor.moveToFirst(); assertEquals("transactions data preserved", 2, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM tasks").use { cursor ->
            cursor.moveToFirst(); assertEquals("tasks should be empty (none were created)", 0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM budgets").use { cursor ->
            cursor.moveToFirst(); assertEquals("budgets should be empty", 0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM corrections").use { cursor ->
            cursor.moveToFirst(); assertEquals("corrections should be empty", 0, cursor.getInt(0))
        }

        // ── Verify account data values survived ──
        migratedDb.query("SELECT name, balance FROM accounts ORDER BY id").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Cash", cursor.getString(0))
            assertEquals(5000.0, cursor.getDouble(1), 0.001)
            cursor.moveToNext()
            assertEquals("Bank", cursor.getString(0))
            assertEquals(10000.0, cursor.getDouble(1), 0.001)
        }

        migratedDb.close()
    }

    @Test
    fun categoriesSeededSynchronouslyOnFreshInstall() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

        val count = runBlocking(Dispatchers.IO) {
            DatabaseInitializer.populate(db)
            db.categoryDao().getCount()
        }
        assertTrue("Categories must be seeded and queryable immediately after build() + populate()", count > 0)

        db.close()
    }
}

/**
 * Migration tests define each historical starting schema inline. Using Room's
 * MigrationTestHelper here would also require a checked-in JSON snapshot for
 * every historical version, which this project has never exported.
 */
private class InstrumentedMigrationHelper(private val context: android.content.Context) {
    fun createDatabase(name: String, version: Int): SupportSQLiteDatabase {
        context.deleteDatabase(name)
        return open(name, version)
    }

    fun runMigrationsAndValidate(
        name: String,
        version: Int,
        @Suppress("UNUSED_PARAMETER") validateDroppedTables: Boolean,
        vararg migrations: Migration
    ): SupportSQLiteDatabase {
        val database = open(name, version)
        migrations.forEach { migration -> migration.migrate(database) }
        return database
    }

    private fun open(name: String, version: Int): SupportSQLiteDatabase {
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) = Unit
            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            override fun onDowngrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(callback)
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
    }
}
