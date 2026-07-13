package com.makemission.makeflow.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.makemission.makeflow.data.local.dao.AccountDao
import com.makemission.makeflow.data.local.dao.BudgetDao
import com.makemission.makeflow.data.local.dao.CardInteractionDao
import com.makemission.makeflow.data.local.dao.CategoryDao
import com.makemission.makeflow.data.local.dao.QueuedNotificationDao
import com.makemission.makeflow.data.local.dao.RecurringPatternDao
import com.makemission.makeflow.data.local.dao.RecurringTransactionDao
import com.makemission.makeflow.data.local.dao.TaskDao
import com.makemission.makeflow.data.local.dao.ExchangeRateDao
import com.makemission.makeflow.data.local.dao.TransactionTemplateDao
import com.makemission.makeflow.data.local.dao.TransactionDao
import com.makemission.makeflow.data.local.entity.AccountEntity
import com.makemission.makeflow.data.local.entity.BudgetEntity
import com.makemission.makeflow.data.local.entity.CardInteractionEntity
import com.makemission.makeflow.data.local.entity.CategoryEntity
import com.makemission.makeflow.data.local.entity.QueuedNotificationEntity
import com.makemission.makeflow.data.local.entity.RecurringPatternEntity
import com.makemission.makeflow.data.local.entity.RecurringTransactionEntity
import com.makemission.makeflow.data.local.entity.TaskEntity
import com.makemission.makeflow.data.local.entity.ExchangeRateEntity
import com.makemission.makeflow.data.local.entity.TransactionTemplateEntity
import com.makemission.makeflow.data.local.entity.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TaskEntity::class,
        BudgetEntity::class,
        RecurringPatternEntity::class,
        CardInteractionEntity::class,
        QueuedNotificationEntity::class,
        RecurringTransactionEntity::class,
        TransactionTemplateEntity::class,
        ExchangeRateEntity::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun taskDao(): TaskDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringPatternDao(): RecurringPatternDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun cardInteractionDao(): CardInteractionDao
    abstract fun queuedNotificationDao(): QueuedNotificationDao

    @androidx.room.Transaction
    open suspend fun atomicRestore(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        tasks: List<TaskEntity>,
        recurringPatterns: List<RecurringPatternEntity>,
        recurringTransactions: List<RecurringTransactionEntity>,
        transactionTemplates: List<TransactionTemplateEntity>,
        exchangeRates: List<ExchangeRateEntity>
    ) {
        // NOTE: card_interactions and queued_notifications are deliberately NOT
        // backed up or restored. card_interactions is device-local clickstream
        // data used to personalize dashboard card ordering (DashboardCardRanker);
        // restoring it would transplant stale usage patterns. queued_notifications
        // is a bounded DND notification queue whose entries are time-sensitive;
        // restoring stale notifications would fire outdated alerts.
        accountDao().clearAll()
        categoryDao().clearAll()
        transactionDao().clearAll()
        budgetDao().clearAll()
        taskDao().clearAll()
        recurringPatternDao().deleteAll()
        recurringTransactionDao().clearAll()
        transactionTemplateDao().clearAll()
        exchangeRateDao().clearAll()

        accounts.forEach { accountDao().insertAccount(it) }
        categories.forEach { categoryDao().insertCategory(it) }
        transactions.forEach { transactionDao().insertTransaction(it) }
        budgets.forEach { budgetDao().insertBudget(it) }
        tasks.forEach { taskDao().insert(it) }
        recurringPatternDao().insertAll(recurringPatterns)
        recurringTransactions.forEach { recurringTransactionDao().insert(it) }
        transactionTemplates.forEach { transactionTemplateDao().insert(it) }
        exchangeRates.forEach { exchangeRateDao().upsertRate(it) }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        categoryId TEXT,
                        timestamp INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys = OFF")

                db.execSQL(
                    """CREATE TEMPORARY TABLE category_id_map AS
                        SELECT id AS old_id, hex(randomblob(16)) AS new_id FROM categories"""
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS categories_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        iconName TEXT NOT NULL DEFAULT '',
                        colorHex TEXT NOT NULL DEFAULT '',
                        monthlyBudget REAL NOT NULL DEFAULT 0.0
                    )"""
                )

                db.execSQL(
                    """INSERT INTO categories_new (id, name, iconName, colorHex, monthlyBudget)
                        SELECT m.new_id, c.name, '', '', c.budgetLimit
                        FROM categories c
                        INNER JOIN category_id_map m ON c.id = m.old_id"""
                )

                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS transactions_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId TEXT,
                        note TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY (categoryId) REFERENCES categories(id) ON DELETE SET NULL
                    )"""
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions_new(categoryId)")

                db.execSQL(
                    """INSERT INTO transactions_new (id, amount, type, categoryId, note, timestamp)
                        SELECT hex(randomblob(16)), t.amount, t.type, m.new_id, COALESCE(t.note, ''), t.timestamp
                        FROM transactions t
                        LEFT JOIN category_id_map m ON t.categoryId = m.old_id"""
                )

                db.execSQL("DROP TABLE categories")
                db.execSQL("DROP TABLE transactions")
                db.execSQL("ALTER TABLE categories_new RENAME TO categories")
                db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")

                db.execSQL("PRAGMA foreign_keys = ON")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS budgets (
                        id TEXT PRIMARY KEY NOT NULL,
                        categoryId TEXT NOT NULL,
                        categoryName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        spentAmount REAL NOT NULL,
                        period TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN accountId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_patterns (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        normalizedDescription TEXT NOT NULL,
                        avgAmount REAL NOT NULL,
                        intervalDays INTEGER NOT NULL,
                        lastSeenDate INTEGER NOT NULL,
                        nextExpectedDate INTEGER NOT NULL,
                        confidence REAL NOT NULL,
                        categoryId TEXT,
                        transactionType TEXT NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS card_interactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardType TEXT NOT NULL,
                        hourOfDay INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS queued_notifications (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        body TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        payload TEXT
                    )"""
                )
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recurring_patterns ADD COLUMN lastNotifiedNextDate INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN fromAccountId TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toAccountId TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS recurring_transactions (
                        id TEXT PRIMARY KEY NOT NULL,
                        description TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId TEXT,
                        accountId TEXT NOT NULL,
                        frequency TEXT NOT NULL,
                        intervalDays INTEGER NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        nextDueDate INTEGER NOT NULL,
                        autoConfirm INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1
                    )"""
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS transaction_templates (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        amount REAL NOT NULL,
                        type TEXT NOT NULL,
                        categoryId TEXT,
                        accountId TEXT NOT NULL DEFAULT '',
                        note TEXT NOT NULL DEFAULT '',
                        iconOrColor TEXT
                    )"""
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN currencyCode TEXT")
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS exchange_rates (
                        currencyCode TEXT PRIMARY KEY NOT NULL,
                        rateToDisplayCurrency REAL NOT NULL,
                        lastUpdated INTEGER NOT NULL
                    )"""
                )
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget)
                        SELECT hex(randomblob(16)), 'Services/Errands', 'EXPENSE', id, 'assignment', '#FFA726', 0.0
                        FROM categories WHERE name = 'Bills' AND parentCategoryId IS NULL AND type = 'EXPENSE'
                        AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Services/Errands' AND parentCategoryId IS NOT NULL)"""
                )
                db.execSQL(
                    """INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget)
                        SELECT hex(randomblob(16)), 'Tithe', 'EXPENSE', id, 'church', '#7B1FA2', 0.0
                        FROM categories WHERE name = 'Generosity' AND parentCategoryId IS NULL AND type = 'EXPENSE'
                        AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Tithe' AND parentCategoryId IS NOT NULL)"""
                )
                db.execSQL(
                    """INSERT INTO categories (id, name, type, parentCategoryId, iconName, colorHex, monthlyBudget)
                        SELECT hex(randomblob(16)), 'Offertory', 'EXPENSE', id, 'redeem', '#E040FB', 0.0
                        FROM categories WHERE name = 'Generosity' AND parentCategoryId IS NULL AND type = 'EXPENSE'
                        AND NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Offertory' AND parentCategoryId IS NOT NULL)"""
                )
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN name TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "makeflow_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    INSTANCE = instance
                    runBlocking(Dispatchers.IO) {
                        DatabaseInitializer.populate(instance)
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        val cutoff = System.currentTimeMillis() - 90L * 24 * 60 * 60 * 1000
                        instance.cardInteractionDao().deleteOlderThan(cutoff)
                    }
                    instance
                } catch (e: Exception) {
                    Log.e("MakeFlowDebug", "Failed to initialize AppDatabase", e)
                    throw e
                }
            }
        }
    }
}
