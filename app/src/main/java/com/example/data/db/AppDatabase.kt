package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.InvoiceEntity
import com.example.data.model.ProductItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ProductItem::class, InvoiceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quick_invoice.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val productDao = getInstance(context).productDao()
                            val initialProducts = listOf(
                                ProductItem(name = "قهوة عربية", defaultPrice = 450.0),
                                ProductItem(name = "قهوة تركية", defaultPrice = 300.0),
                                ProductItem(name = "شاي أسود فاخر", defaultPrice = 200.0),
                                ProductItem(name = "شاي أخضر بالنعناع", defaultPrice = 250.0),
                                ProductItem(name = "سكر أبيض (1 كغ)", defaultPrice = 110.0),
                                ProductItem(name = "زيت زيتون بكر (1 لتر)", defaultPrice = 1200.0),
                                ProductItem(name = "حليب معقم (1 لتر)", defaultPrice = 130.0),
                                ProductItem(name = "تمر ممتاز", defaultPrice = 600.0),
                                ProductItem(name = "عسل طبيعي (500 غ)", defaultPrice = 1800.0),
                                ProductItem(name = "مياه معدنية (1.5 لتر)", defaultPrice = 50.0)
                            )
                            productDao.insertAll(initialProducts)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
