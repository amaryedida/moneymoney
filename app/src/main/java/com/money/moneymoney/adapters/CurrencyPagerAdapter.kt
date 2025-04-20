package com.money.moneymoney.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.money.moneymoney.fragments.CurrencyReportFragment

class CurrencyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2 // For INR and AED

    override fun createFragment(position: Int): Fragment {
        return CurrencyReportFragment.newInstance(
            when (position) {
                0 -> "INR"
                1 -> "AED"
                else -> "INR"
            }
        )
    }
} 