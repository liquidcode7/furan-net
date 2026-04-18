package com.liquidfuran.furan.model

data class AppInfo(
    val name: String,
    val packageName: String,
    val isAllowlisted: Boolean = false
)
