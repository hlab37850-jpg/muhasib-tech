package com.muhasibtech.app.ui.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.datepicker.MaterialDatePicker
import com.muhasibtech.app.MuhasibApp
import com.muhasibtech.app.R
import com.muhasibtech.app.data.local.entity.AccountEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DocumentRequest
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivityTransferBinding
import com.muhasibtech.app.util.AppDate
import com.muhasibtech.app.util.CurrencyPicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class TransferActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, TransferActivity::class.java))
        }
    }

    private lateinit var binding: ActivityTransferBinding
    private lateinit var repo: AppRepository

    private var accounts: List<AccountEntity> = emptyList()
    private var currencies: List<CurrencyEntity> = emptyList()

    private var fromId: Long = 0
    private var toId: Long = 0
    private var currencyId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        binding.toolbar.title = getString(R.string.transfer_title)
        binding.etDate.setText(AppDate.today())
        binding.etDate.setOnClickListener { openDatePicker() }

        binding.btnSave.setOnClickListener { save() }

        lifecycleScope.launch {
            accounts = repo.accounts.observeActive().first()
            currencies = repo.currencies.observeActive().first()
            currencyId = repo.settings.getCurrentCurrencyId()

            val names = accounts.map { it.name }
            val adapter = ArrayAdapter(this@TransferActivity, android.R.layout.simple_dropdown_item_1line, names)

            binding.etFrom.setAdapter(adapter)
            binding.etTo.setAdapter(adapter)

            binding.etFrom.setOnItemClickListener { _, _, position, _ -> fromId = accounts[position].id }
            binding.etTo.setOnItemClickListener { _, _, position, _ -> toId = accounts[position].id }

            CurrencyPicker.build(this@TransferActivity, binding.rgCurrency, currencies, currencyId) { id ->
                currencyId = id
            }
        }
    }

    private fun openDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date))
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            binding.etDate.setText(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time))
        }

        picker.show(supportFragmentManager, "transfer_date")
    }

    private fun save() {
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (fromId == 0L || toId == 0L || amount == 0.0 || fromId == toId) {
            Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
            return
        }

        val notes = binding.etNotes.text.toString().trim()

        val request = DocumentRequest(
            docType = DocType.JOURNAL_DAILY,
            date = binding.etDate.text.toString().trim(),
            currencyId = currencyId,
            notes = notes,
            lines = listOf(
                DocumentRequest.Line(accountId = toId, debit = amount, notes = notes),
                DocumentRequest.Line(accountId = fromId, credit = amount, notes = notes)
            )
        )

        lifecycleScope.launch {
            try {
                repo.documentService.saveDocument(request)
                Toast.makeText(this@TransferActivity, R.string.saved_success, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@TransferActivity, e.message ?: getString(R.string.save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
