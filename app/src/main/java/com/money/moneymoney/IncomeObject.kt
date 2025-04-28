package com.money.moneymoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class IncomeObject(
    val id: Long,
    val currency: String,
    val category: String,
    val value: Double,
    val comment: String?,
    val date: Long
) : Parcelable 