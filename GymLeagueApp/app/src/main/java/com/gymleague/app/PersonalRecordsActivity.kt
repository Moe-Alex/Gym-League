package com.gymleague.app

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gymleague.app.adapter.PersonalRecordAdapter
import com.gymleague.app.util.OneRepMaxUtils
import com.gymleague.app.viewmodel.PersonalRecordsViewModel
import kotlinx.coroutines.launch

class PersonalRecordsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: PersonalRecordsViewModel
    private lateinit var adapter: PersonalRecordAdapter

    private lateinit var rvPersonalRecords: RecyclerView
    private lateinit var prProgressBar: ProgressBar
    private lateinit var tvPrError: TextView
    private lateinit var tvPrEmpty: TextView
    private lateinit var tvTotalCount: TextView
    private lateinit var tvTopValue: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_records)

        sessionManager = SessionManager(this)

        val userId = sessionManager.getUserId()

        if (userId == -1) {
            finish()
            return
        }

        viewModel = PersonalRecordsViewModel()

        bindViews()
        setupRecyclerView()
        observeViewModel()

        viewModel.loadPersonalRecords(userId)
    }

    private fun bindViews() {
        rvPersonalRecords = findViewById(R.id.rvPersonalRecords)
        prProgressBar = findViewById(R.id.prProgressBar)
        tvPrError = findViewById(R.id.tvPrError)
        tvPrEmpty = findViewById(R.id.tvPrEmpty)
        tvTotalCount = findViewById(R.id.tvPrTotalCount)
        tvTopValue = findViewById(R.id.tvPrTopValue)
    }

    private fun setupRecyclerView() {
        adapter = PersonalRecordAdapter(emptyList())
        rvPersonalRecords.layoutManager = LinearLayoutManager(this)
        rvPersonalRecords.adapter = adapter
    }

    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.records.collect { records ->

                adapter.updateRecords(records)

                tvTotalCount.text = records.size.toString()

                tvTopValue.text = if (records.isNotEmpty()) {
                    OneRepMaxUtils.formatWeight(records.first().estimatedOneRepMax)
                } else {
                    "--"
                }

                tvPrEmpty.visibility = if (records.isEmpty()) View.VISIBLE else View.GONE
                rvPersonalRecords.visibility = if (records.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                prProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                if (error != null) {
                    tvPrError.text = error
                    tvPrError.visibility = View.VISIBLE
                } else {
                    tvPrError.visibility = View.GONE
                }
            }
        }
    }
}