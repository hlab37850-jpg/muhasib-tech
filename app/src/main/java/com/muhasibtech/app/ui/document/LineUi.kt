package com.muhasibtech.app.ui.document

data class LineUi(
    var itemId: Long = 0,
    var itemName: String = "",
    var accountId: Long = 0,
    var accountName: String = "",
    var qty: Double = 0.0,
    var price: Double = 0.0,
    var total: Double = 0.0,
    var debit: Double = 0.0,
    var credit: Double = 0.0,
    var notes: String = ""
)
