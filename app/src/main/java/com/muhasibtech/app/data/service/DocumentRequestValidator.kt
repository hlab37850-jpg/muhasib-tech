package com.muhasibtech.app.data.service

import com.muhasibtech.app.data.model.DiscountType
import com.muhasibtech.app.data.model.DocType
import com.muhasibtech.app.data.model.DocumentRequest

/**
 * Pure request validation shared by the document service and unit tests.
 * It intentionally performs no database or Android operations.
 */
object DocumentRequestValidator {

    fun validate(request: DocumentRequest) {
        val supportedTypes = setOf(
            DocType.SALE_CASH, DocType.SALE_CREDIT,
            DocType.PURCHASE_CASH, DocType.PURCHASE_CREDIT,
            DocType.RECEIPT, DocType.PAYMENT,
            DocType.JOURNAL_DAILY, DocType.JOURNAL_OPENING,
            DocType.STOCK_OUT, DocType.STOCK_IN, DocType.STOCK_TRANSFER,
            DocType.STOCK_ADJUSTMENT, DocType.STOCKTAKE,
            DocType.QUOTATION, DocType.PURCHASE_REQUEST
        )
        if (request.docType !in supportedTypes) {
            throw IllegalStateException("نوع المستند غير مدعوم")
        }

        if (request.discountType != DiscountType.AMOUNT &&
            request.discountType != DiscountType.PERCENT
        ) {
            throw IllegalStateException("نوع الخصم غير صالح")
        }

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

        if ((request.docType == DocType.JOURNAL_DAILY ||
                request.docType == DocType.JOURNAL_OPENING) &&
            request.lines.any { line ->
                line.accountId <= 0L ||
                    (line.debit > 0.001 && line.credit > 0.001)
            }
        ) {
            throw IllegalStateException("كل سطر في القيد يجب أن يحدد حسابًا وطرفًا واحدًا فقط")
        }

        if (request.discountType == DiscountType.PERCENT && request.discountValue > 100.0) {
            throw IllegalStateException("لا يمكن أن تتجاوز نسبة الخصم 100٪")
        }
    }
}
