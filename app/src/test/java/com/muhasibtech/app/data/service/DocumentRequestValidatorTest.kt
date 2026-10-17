package com.muhasibtech.app.data.service

import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DocumentRequest
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentRequestValidatorTest {

    private fun validRequest() = DocumentRequest(
        docType = "SALE_CASH",
        lines = listOf(DocumentRequest.Line(itemId = 1L, qty = 2.0, price = 5.0))
    )

    @Test
    fun acceptsValidDocumentRequest() {
        DocumentRequestValidator.validate(validRequest())
    }

    @Test
    fun rejectsDocumentWithoutLines() {
        val error = runCatching {
            DocumentRequestValidator.validate(validRequest().copy(lines = emptyList()))
        }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("لا توجد بنود") == true)
    }

    @Test
    fun rejectsNegativeLineQuantity() {
        val request = validRequest().copy(
            lines = listOf(DocumentRequest.Line(itemId = 1L, qty = -1.0, price = 5.0))
        )

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun rejectsNonFiniteAmountsAndExchangeRate() {
        val request = validRequest().copy(currencyRate = Double.NaN)

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun rejectsPercentageDiscountAboveOneHundred() {
        val request = validRequest().copy(
            discountType = DiscountType.PERCENT,
            discountValue = 100.01
        )

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }

    @Test
    fun rejectsUnknownDocumentType() {
        val request = validRequest().copy(docType = "UNKNOWN_TYPE")

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("غير مدعوم") == true)
    }

    @Test
    fun rejectsUnknownDiscountType() {
        val request = validRequest().copy(discountType = "UNKNOWN_DISCOUNT")

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("نوع الخصم") == true)
    }

    @Test
    fun rejectsJournalLineWithoutAccount() {
        val request = validRequest().copy(
            docType = DocType.JOURNAL_DAILY,
            lines = listOf(DocumentRequest.Line(debit = 10.0, credit = 10.0))
        )

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error?.message?.contains("حسابًا") == true)
    }

    @Test
    fun rejectsJournalLineWithDebitAndCreditTogether() {
        val request = validRequest().copy(
            docType = DocType.JOURNAL_DAILY,
            lines = listOf(DocumentRequest.Line(accountId = 1L, debit = 10.0, credit = 10.0))
        )

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun acceptsJournalLineWithSingleSideAndAccount() {
        val request = validRequest().copy(
            docType = DocType.JOURNAL_DAILY,
            lines = listOf(
                DocumentRequest.Line(accountId = 1L, debit = 10.0),
                DocumentRequest.Line(accountId = 2L, credit = 10.0)
            )
        )

        DocumentRequestValidator.validate(request)
    }

    @Test
    fun rejectsNegativeFees() {
        val request = validRequest().copy(feesAmount = -0.01)

        val error = runCatching { DocumentRequestValidator.validate(request) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
    }
}
