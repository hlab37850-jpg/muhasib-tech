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
import com.muhasibtech.app.data.local.entity.CashboxEntity
import com.muhasibtech.app.data.local.entity.CurrencyEntity
import com.muhasibtech.app.data.model.AccountNature
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DocumentRequest
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.databinding.ActivitySettleBinding
import com.muhasibtech.app.util.AppDate
import com.muhasibtech.app.util.CurrencyPicker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class SettleActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_PARTY_ID = "extra_party_id"

        fun start(context: Context, partyId: Long) {
            val intent = Intent(context, SettleActivity::class.java)
            intent.putExtra(EXTRA_PARTY_ID, partyId)
            context.startActivity(intent)
        }
    }

    private lateinit var binding: ActivitySettleBinding
    private lateinit var repo: AppRepository

    private var accounts: List<AccountEntity> = emptyList()
    private var cashboxes: List<CashboxEntity> = emptyList()
    private var currencies: List<CurrencyEntity> = emptyList()

    private var partyAccountId: Long = 0
    private var cashboxId: Long = 0
    private var currencyId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repo = (application as MuhasibApp).repository

        binding.toolbar.title = getString(R.string.settle)
        binding.etDate.setText(AppDate.today())

        binding.etDate.setOnClickListener { openDatePicker() }

        binding.btnDebit.setOnClickListener { save(true) }
        binding.btnCredit.setOnClickListener { save(false) }

        lifecycleScope.launch {
            accounts = repo.accounts.observeActive().first()
            cashboxes = repo.cashboxes.observeActive().first()
            currencies = repo.currencies.observeActive().first()

            cashboxId = repo.cashboxes.getDefault()?.id ?: 0
            currencyId = repo.settings.getCurrentCurrencyId()
            partyAccountId = intent.getLongExtra(EXTRA_PARTY_ID, 0)

            val partyAdapter = ArrayAdapter(
                this@SettleActivity,
                android.R.layout.simple_dropdown_item_1line,
                accounts.map { it.name }
            )
            binding.etParty.setAdapter(partyAdapter)
            binding.etParty.setOnItemClickListener { _, _, position, _ ->
                partyAccountId = accounts[position].id
            }

            if (partyAccountId != 0L) {
                binding.etParty.setText(accounts.firstOrNull { it.id == partyAccountId }?.name ?: "")
            }

            val cashboxAdapter = ArrayAdapter(
                this@SettleActivity,
                android.R.layout.simple_spinner_item,
                cashboxes.map { it.name }
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            binding.spCashbox.adapter = cashboxAdapter

            binding.spCashbox.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                    cashboxId = cashboxes[pos].id
                }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }

            val index = cashboxes.indexOfFirst { it.id == cashboxId }
            if (index >= 0) binding.spCashbox.setSelection(index)

            CurrencyPicker.build(this@SettleActivity, binding.rgCurrency, currencies, currencyId) { id ->
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

        picker.show(supportFragmentManager, "settle_date")
    }

    private fun save(partyDebit: Boolean) {
        val partyText = binding.etParty.text.toString().trim()
        val amount = binding.etAmount.text.toString().toDoubleOrNull() ?: 0.0

        if (partyText.isEmpty() || amount == 0.0) {
            Toast.makeText(this, R.string.field_required, Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            var partyId = partyAccountId

            val existing = accounts.firstOrNull { it.name == partyText }
            if (existing != null) {
                partyId = existing.id
            } else if (partyId == 0L) {
                partyId = repo.accounts.insert(
                    AccountEntity(name = partyText, nature = AccountNature.ASSET)
                )
            }

            val docType = if (partyDebit) DocType.PAYMENT else DocType.RECEIPT
            val notes = binding.etNotes.text.toString().trim()

            val request = DocumentRequest(
                docType = docType,
                date = binding.etDate.text.toString().trim(),
                partyAccountId = partyId,
                currencyId = currencyId,
                cashboxId = cashboxId,
                notes = notes,
                lines = listOf(
                    DocumentRequest.Line(
                        accountId = partyId,
                        debit = if (partyDebit) amount else 0.0,
                        credit = if (partyDebit) 0.0 else amount,
                        notes = notes
                    )
                )
            )

            try {
                repo.documentService.saveDocument(request)
                Toast.makeText(this@SettleActivity, R.string.saved_success, Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@SettleActivity, e.message ?: getString(R.string.save_failed), Toast.LENGTH_LONG).show()
            }
        }
    }
}
