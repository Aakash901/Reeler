package com.whatsapp.testing.ui.activity.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.whatsapp.testing.database.DailyStats
import com.whatsapp.testing.ui.activity.Fragment.ArearangeFragment
import com.whatsapp.testing.ui.activity.Fragment.BarChartFragment
import com.whatsapp.testing.ui.activity.Fragment.BaseChartFragment
import com.whatsapp.testing.ui.activity.Fragment.ColumnChartFragment
import com.whatsapp.testing.ui.activity.Fragment.PieChartFragment
import com.whatsapp.testing.ui.activity.Fragment.PolarChartFragment
import com.whatsapp.testing.ui.activity.Fragment.RadarChartFragment
import com.whatsapp.testing.ui.activity.Fragment.ScatterFragment

// StatsChartPagerAdapter.kt
class StatsChartPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    // Make fragments accessible through a getter
    private val _fragments: List<BaseChartFragment> = listOf(
        PieChartFragment(),
        ColumnChartFragment(),
        PolarChartFragment(),
        BarChartFragment(),
        ArearangeFragment(),
        ScatterFragment(),
        RadarChartFragment()
    )

    // Public getter for fragments
    fun getFragments(): List<BaseChartFragment> = _fragments

    override fun getItemCount(): Int = _fragments.size

    override fun createFragment(position: Int): Fragment = _fragments[position]

    // Add method to update stats in all fragments
    fun updateAllFragmentsStats(stats: List<DailyStats>) {
        _fragments.forEach { fragment ->
            fragment.updateStats(stats)
        }
    }
}