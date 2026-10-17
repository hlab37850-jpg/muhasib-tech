package com.muhasibtech.app.data.service

import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocumentRequest

/**
 * Pure request validation shared by the document service and unit tests.
 * It intentionally performs no database or Android operations.
 */
object DocumentRequestValidator {

    fun validate(request: DocumentRequest) {
        if (request.lines.isEmpty()) {
            throw IllegalStateException("لا توجد بنود في المستند")
        }

        if (!request.discountValue.isFinite() || request.discountValue < 0.0 ||
            !request.taxRate.isFinite() || request.taxRate < 0.0 ||
            !request.feesAmount.isFinite() || request.feesAmount < 0.0 ||
            !request.paidAmount.isFinite() || request.paidAmount < 0.0 ||
            !request.currencyRate.isFinite() || request.currencyRate <= 0.0
        ) {
            throw IllegalStateException("تحقق من الخصم والضريبة والرسوم والمبلغ المدفوع وسعر الصرف")
        }

        if (request.lines.any { line ->
                !line.qty.isFinite() || !line.price.isFinite() || !line.total.isFinite() ||
                    !line.debit.isFinite() || !line.credit.isFinite() ||
                    line.qty < 0.0 || line.price < 0.0 || line.total < 0.0 ||
                    line.debit < 0.0 || line.credit < 0.0
            }
        ) {
            throw IllegalStateException("توجد كمية أو قيمة مالية غير صحيحة في أحد البنود")
        }

        if (request.discountType == DiscountType.PERCENT && request.discountValue > 100.0) {
            throw IllegalStateException("لا يمكن أن تتجاوز نسبة الخصم 100٪")
        }
    }
}
