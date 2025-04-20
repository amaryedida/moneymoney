package com.money.moneymoney

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class CurrencyPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

    private val currencies = listOf("INR", "AED")

    override fun getItemCount(): Int = currencies.size

    override fun createFragment(position: Int): Fragment {
        return CurrencyReportFragment.newInstance(currencies[position])
    }
} 