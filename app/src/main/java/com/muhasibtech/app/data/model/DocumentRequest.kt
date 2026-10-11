package com.muhasibtech.app.data.model

data class DocumentRequest(
    val documentId: Long = 0,
    val docType: String,
    val date: String = "",
    val reference: String = "",
    val partyAccountId: Long = 0,
    val warehouseId: Long = 0,
    val targetWarehouseId: Long = 0,
    val currencyId: Long = 0,
    val cashboxId: Long = 0,
    val currencyRate: Double = 1.0,
    val isCredit: Boolean = false,
    val settlementType: String = "",
    val notes: String = "",
    val discountType: String = DiscountType.AMOUNT,
    val discountValue: Double = 0.0,
    val taxRate: Double = 0.0,
    val feesAmount: Double = 0.0,
    val feesNote: String = "",
    val paidAmount: Double = 0.0,
    val lines: List<Line> = emptyList()
) {

    data class Line(
        val itemId: Long = 0,
        val accountId: Long = 0,
        val qty: Double = 0.0,
        val price: Double = 0.0,
        val total: Double = 0.0,
        val debit: Double = 0.0,
        val credit: Double = 0.0,
        val notes: String = "",
        val sortOrder: Int = 0
    )
}
