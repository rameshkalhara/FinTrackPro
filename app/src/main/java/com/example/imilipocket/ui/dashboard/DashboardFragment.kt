package com.example.imilipocket.ui.dashboard

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.imilipocket.R
import com.example.imilipocket.data.PreferenceManager
import com.example.imilipocket.data.Transaction
import com.example.imilipocket.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DashboardViewModel
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        try {
            _binding = FragmentDashboardBinding.inflate(inflater, container, false)
            return binding.root
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in onCreateView: ${e.message}")
            throw e
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            setupViewModel()
            setupUI()
            observeViewModel()
        } catch (e: Exception) {
            Log.e("DashboardFragment", "Error in onViewCreated: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupViewModel() {
        val preferenceManager = PreferenceManager(requireContext())
        val factory = object : ViewModelProvider.NewInstanceFactory() {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(preferenceManager) as T
            }
        }
        viewModel = ViewModelProvider(this, factory).get(DashboardViewModel::class.java)
    }

    private fun setupUI() {
        setupPieChart()
    }

    private fun observeViewModel() {
        setupObservers()
    }

    override fun onResume() {
        super.onResume()
        try {
            viewModel.loadDashboardData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.totalBalance.observe(viewLifecycleOwner) { balance ->
            try {
                binding.tvTotalBalance.text = formatCurrency(balance ?: 0.0)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvTotalBalance.text = formatCurrency(0.0)
            }
        }

        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            try {
                binding.tvTotalIncome.text = formatCurrency(income ?: 0.0)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvTotalIncome.text = formatCurrency(0.0)
            }
        }

        viewModel.totalExpense.observe(viewLifecycleOwner) { expense ->
            try {
                binding.tvTotalExpense.text = formatCurrency(expense ?: 0.0)
            } catch (e: Exception) {
                e.printStackTrace()
                binding.tvTotalExpense.text = formatCurrency(0.0)
            }
        }

        viewModel.categorySpending.observe(viewLifecycleOwner) { spending ->
            try {
                updatePieChart(spending ?: emptyMap())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupPieChart() {
        try {
            binding.pieChart.apply {
                description.isEnabled = false
                legend.isEnabled = true
                setHoleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                setTransparentCircleColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))
                setEntryLabelColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                setEntryLabelTextSize(12f)
                setUsePercentValues(true)
                setDrawEntryLabels(true)
                setDrawHoleEnabled(true)
                setHoleRadius(50f)
                setTransparentCircleRadius(55f)
                setRotationEnabled(true)
                setHighlightPerTapEnabled(true)
                animateY(1000)
                setNoDataText("No transactions yet")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updatePieChart(spending: Map<String, Double>) {
        try {
            if (spending.isEmpty()) {
                binding.pieChart.setNoDataText("No transactions yet")
                binding.pieChart.invalidate()
                return
            }

            val entries = spending.map { (category, amount) ->
                PieEntry(amount.toFloat(), category)
            }

            val dataSet = PieDataSet(entries, "Categories").apply {
                colors = entries.map { entry ->
                    if (entry.label.startsWith("Income:")) {
                        ContextCompat.getColor(requireContext(), R.color.green_500)
                    } else {
                        ContextCompat.getColor(requireContext(), R.color.red_500)
                    }
                }
                valueFormatter = PercentFormatter(binding.pieChart)
                valueTextSize = 12f
                valueTextColor = ContextCompat.getColor(requireContext(), android.R.color.black)
            }

            binding.pieChart.data = PieData(dataSet)
            binding.pieChart.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
            binding.pieChart.setNoDataText("Error loading data")
            binding.pieChart.invalidate()
        }
    }

    private fun formatCurrency(amount: Double): String {
        return try {
            val format = NumberFormat.getCurrencyInstance()
            format.currency = Currency.getInstance(viewModel.getCurrency())
            format.format(amount)
        } catch (e: Exception) {
            "$$amount"
        }
    }
} 