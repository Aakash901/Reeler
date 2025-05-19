package com.reeler.app.ui.activity.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.reeler.app.database.DailyStats
import com.reeler.app.ui.activity.Fragment.ArearangeFragment
import com.reeler.app.ui.activity.Fragment.BarChartFragment
import com.reeler.app.ui.activity.Fragment.BaseChartFragment
import com.reeler.app.ui.activity.Fragment.ColumnChartFragment
import com.reeler.app.ui.activity.Fragment.PieChartFragment
import com.reeler.app.ui.activity.Fragment.PolarChartFragment
import com.reeler.app.ui.activity.Fragment.RadarChartFragment
import com.reeler.app.ui.activity.Fragment.ScatterFragment

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