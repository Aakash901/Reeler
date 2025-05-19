package com.reeler.app.ui.activity.Fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.reeler.app.R
import com.reeler.app.database.DailyStats

// BaseChartFragment.kt
abstract class BaseChartFragment : Fragment() {
    protected lateinit var rootView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        rootView = inflater.inflate(R.layout.fragment_base_chart, container, false)
        return rootView
    }

    fun updateStats(stats: List<DailyStats>) {
        // Will implement in child fragments later
    }
}