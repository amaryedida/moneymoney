package com.money.moneymoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GoalObject(
    val id: Long = 0,
    val name: String,
    val targetValue: Double,
    val currency: String?,
    val creationDate: Long?,
    val status: String = "Active",
    val completionDate: Long? = null
) : Parcelable 