package com.muhasibtech.app.data.service

/**
 * Prevents stock-decreasing operations from silently creating negative inventory.
 * Tolerance is only used for floating-point rounding, not to permit a real shortage.
 */
object StockAvailabilityValidator {
    fun validate(availableQty: Double, requestedQty: Double) {
        if (!availableQty.isFinite() || !requestedQty.isFinite() ||
            availableQty < 0.0 || requestedQty < 0.0
        ) {
            throw IllegalStateException("كمية المخزون المطلوبة أو المتاحة غير صالحة")
        }
        if (requestedQty - availableQty > 0.000001) {
            throw IllegalStateException("الكمية المطلوبة أكبر من المتوفر في المخزون")
        }
    }
}
