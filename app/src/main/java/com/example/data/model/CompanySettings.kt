package com.example.data.model

data class CompanySettings(
    val companyName: String = "اسم شركتك",
    val companyPhone: String = "0555 55 55 55",
    val companyAddress: String = "العنوان",
    val currencySymbol: String = "د.ج",
    val themeMode: String = "light",
    val primaryColorIndex: Int = 0
)
