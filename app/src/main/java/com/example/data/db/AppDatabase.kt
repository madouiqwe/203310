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
    version = 2,
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
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                val productDao = getInstance(context).productDao()
                                val initialProducts = listOf(
                                    ProductItem(name = "قهوة عربية فاخرة", defaultPrice = 450.0, category = "مشروبات", barcode = "619001"),
                                    ProductItem(name = "قهوة تركية مطحونة", defaultPrice = 300.0, category = "مشروبات", barcode = "619002"),
                                    ProductItem(name = "شاي أسود سيلاني", defaultPrice = 200.0, category = "مشروبات", barcode = "619003"),
                                    ProductItem(name = "شاي أخضر بالنعناع", defaultPrice = 250.0, category = "مشروبات", barcode = "619004"),
                                    ProductItem(name = "سكر أبيض ناعم (1 كغ)", defaultPrice = 110.0, category = "مواد غذائية", barcode = "619005"),
                                    ProductItem(name = "زيت زيتون بكر ممتاز (1 لتر)", defaultPrice = 1200.0, category = "مواد غذائية", barcode = "619006"),
                                    ProductItem(name = "حليب معقم كامل الدسم (1 لتر)", defaultPrice = 130.0, category = "ألبان", barcode = "619007"),
                                    ProductItem(name = "تمر دقلة نور ممتاز", defaultPrice = 600.0, category = "مواد غذائية", barcode = "619008"),
                                    ProductItem(name = "عسل سدر طبيعي (500 غ)", defaultPrice = 1800.0, category = "مواد غذائية", barcode = "619009"),
                                    ProductItem(name = "مياه معدنية طبيعية (1.5 لتر)", defaultPrice = 50.0, category = "مشروبات", barcode = "619010"),
                                    ProductItem(name = "أرز بسمتي درجة أولى (1 كغ)", defaultPrice = 380.0, category = "مواد غذائية", barcode = "619011"),
                                    ProductItem(name = "جبن مثلثات (16 قطعة)", defaultPrice = 220.0, category = "ألبان", barcode = "619012")
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
