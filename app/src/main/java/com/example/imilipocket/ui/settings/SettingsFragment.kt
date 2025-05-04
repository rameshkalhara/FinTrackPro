package com.example.imilipocket.ui.settings

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.imilipocket.R
import com.example.imilipocket.databinding.FragmentSettingsBinding
import com.example.imilipocket.data.PreferenceManager

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        preferenceManager = PreferenceManager(requireContext())
        viewModel = ViewModelProvider(this, SettingsViewModelFactory(preferenceManager))
            .get(SettingsViewModel::class.java)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupUI() {
        // Setup currency spinner
        val currencies = listOf(
            getString(R.string.currency_usd),
            getString(R.string.currency_lkr),
        )
        
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            currencies
        )
        
        binding.spinnerCurrency.apply {
            setAdapter(adapter)
            threshold = 1
            
            // Set current currency
            val currentCurrency = preferenceManager.getSelectedCurrency()
            val currencyIndex = currencies.indexOfFirst { it.startsWith(currentCurrency) }
            if (currencyIndex != -1) {
                setText(currencies[currencyIndex], false)
            }
            
            setOnItemClickListener { _, _, position, _ ->
                val selectedCurrency = adapter.getItem(position).toString()
                val currencyCode = selectedCurrency.substring(0, 3)
                preferenceManager.setSelectedCurrency(currencyCode)
                Toast.makeText(requireContext(), "Currency updated to $selectedCurrency", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnSaveCurrency.setOnClickListener {
                val selectedCurrency = spinnerCurrency.text.toString()
                viewModel.setSelectedCurrency(selectedCurrency)
                Toast.makeText(requireContext(), "Currency saved", Toast.LENGTH_SHORT).show()
            }

            btnBackup.setOnClickListener {
                try {
                    val transactions = preferenceManager.getTransactions()
                    val budget = preferenceManager.getMonthlyBudget()
                    val currency = preferenceManager.getSelectedCurrency()
                    
                    val backupData = mapOf(
                        "transactions" to transactions,
                        "budget" to budget,
                        "currency" to currency
                    )
                    
                    val gson = com.google.gson.Gson()
                    val jsonData = gson.toJson(backupData)
                    
                    if (preferenceManager.createBackup(jsonData)) {
                        Toast.makeText(requireContext(), R.string.msg_backup_success, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                }
            }

            btnRestore.setOnClickListener {
                val backupFiles = preferenceManager.getBackupFiles()
                if (backupFiles.isEmpty()) {
                    Toast.makeText(requireContext(), "No backup files found", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val fileNames = backupFiles.map { it.name }.toTypedArray()
                
                AlertDialog.Builder(requireContext())
                    .setTitle("Select Backup to Restore")
                    .setItems(fileNames) { _, which ->
                        val selectedFile = backupFiles[which]
                        AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Restore")
                            .setMessage("Are you sure you want to restore from ${selectedFile.name}? This will overwrite your current data.")
                            .setPositiveButton("Restore") { _, _ ->
                                if (preferenceManager.restoreFromBackup(selectedFile)) {
                                    Toast.makeText(requireContext(), R.string.msg_restore_success, Toast.LENGTH_SHORT).show()
                                    // Refresh UI after restore
                                    setupUI()
                                } else {
                                    Toast.makeText(requireContext(), R.string.msg_error, Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                    .show()
            }
        }
    }

    private fun observeViewModel() {
        // Implement the logic to observe the ViewModel
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 