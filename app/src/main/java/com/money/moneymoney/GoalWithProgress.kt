package com.money.moneymoney

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class GoalWithProgress(
    val goal: GoalObject,
    val amountInvested: Double,
    val percentageProgress: Int,
    val remainingAmount: Double
) : Parcelable 