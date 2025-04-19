package com.money.moneymoney

data class Goal(
    val id: Long = 0,
    val name: String,
    val targetValue: Double,
    val currency: String?,
    val creationDate: Long?,
    val status: String = "Active",
    val completionDate: Long? = null
)