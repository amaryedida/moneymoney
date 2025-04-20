package com.money.moneymoney

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout

class CurrencyReportFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tabLayout: TabLayout
    private var currency: String? = null
    private lateinit var adapter: CurrencyReportAdapter

    companion object {
        private const val ARG_CURRENCY = "currency"

        fun newInstance(currency: String): CurrencyReportFragment {
            return CurrencyReportFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CURRENCY, currency)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currency = arguments?.getString(ARG_CURRENCY)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_currency_report, container, false)
        
        recyclerView = view.findViewById(R.id.recyclerView)
        tabLayout = view.findViewById(R.id.tabLayout)

        setupRecyclerView()
        setupTabLayout()

        return view
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = CurrencyReportAdapter()
        recyclerView.adapter = adapter
        
        // Add some sample data for testing
        val sampleData = listOf(
            CurrencyReportAdapter.ReportItem("Today", 1000.0, 500.0, 500.0),
            CurrencyReportAdapter.ReportItem("Yesterday", 800.0, 600.0, 200.0),
            CurrencyReportAdapter.ReportItem("Last Week", 5000.0, 3000.0, 2000.0)
        )
        adapter.updateData(sampleData)
    }

    private fun setupTabLayout() {
        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection - update data based on selected tab
                when (tab?.position) {
                    0 -> loadDailyData()
                    1 -> loadWeeklyData()
                    2 -> loadMonthlyData()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not needed
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not needed
            }
        })
    }
    
    private fun loadDailyData() {
        // TODO: Load daily data from database
        val dailyData = listOf(
            CurrencyReportAdapter.ReportItem("Today", 1000.0, 500.0, 500.0),
            CurrencyReportAdapter.ReportItem("Yesterday", 800.0, 600.0, 200.0)
        )
        adapter.updateData(dailyData)
    }
    
    private fun loadWeeklyData() {
        // TODO: Load weekly data from database
        val weeklyData = listOf(
            CurrencyReportAdapter.ReportItem("This Week", 5000.0, 3000.0, 2000.0),
            CurrencyReportAdapter.ReportItem("Last Week", 4500.0, 2800.0, 1700.0)
        )
        adapter.updateData(weeklyData)
    }
    
    private fun loadMonthlyData() {
        // TODO: Load monthly data from database
        val monthlyData = listOf(
            CurrencyReportAdapter.ReportItem("This Month", 20000.0, 15000.0, 5000.0),
            CurrencyReportAdapter.ReportItem("Last Month", 18000.0, 14000.0, 4000.0)
        )
        adapter.updateData(monthlyData)
    }
} 