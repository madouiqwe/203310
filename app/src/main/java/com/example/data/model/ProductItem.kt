package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val defaultPrice: Double,
    val category: String = "عام",
    val barcode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
