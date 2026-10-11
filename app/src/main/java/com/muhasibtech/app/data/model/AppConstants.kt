package com.muhasibtech.app.data.model

object DocType {
    const val SALE_CASH = "SALE_CASH"
    const val SALE_CREDIT = "SALE_CREDIT"

    const val PURCHASE_CASH = "PURCHASE_CASH"
    const val PURCHASE_CREDIT = "PURCHASE_CREDIT"

    const val RECEIPT = "RECEIPT"
    const val PAYMENT = "PAYMENT"

    const val JOURNAL_DAILY = "JOURNAL_DAILY"
    const val JOURNAL_OPENING = "JOURNAL_OPENING"

    const val STOCK_OUT = "STOCK_OUT"
    const val STOCK_IN = "STOCK_IN"
    const val STOCK_TRANSFER = "STOCK_TRANSFER"
    const val STOCK_ADJUSTMENT = "STOCK_ADJUSTMENT"
    const val STOCKTAKE = "STOCKTAKE"

    const val QUOTATION = "QUOTATION"
    const val PURCHASE_REQUEST = "PURCHASE_REQUEST"
}

object AccountNature {
    const val ASSET = "ASSET"
    const val LIABILITY = "LIABILITY"
    const val EQUITY = "EQUITY"
    const val REVENUE = "REVENUE"
    const val EXPENSE = "EXPENSE"
}

object AdjustmentType {
    const val OPENING = "OPENING"
    const val DAMAGED = "DAMAGED"
    const val INCREASE = "INCREASE"
    const val SHORTAGE = "SHORTAGE"
}

object DiscountType {
    const val AMOUNT = "AMOUNT"
    const val PERCENT = "PERCENT"
}

object SettingKeys {
    const val SEED_VERSION = "seed_version"

    const val CURRENT_WAREHOUSE_ID = "current_warehouse_id"
    const val CURRENT_CURRENCY_ID = "current_currency_id"
    const val CURRENT_USER_ID = "current_user_id"

    const val COMPANY_NAME = "company_name"
    const val PHONE = "phone"
    const val ADDRESS = "address"

    const val CURRENCY_LOCAL_ID = "currency_local_id"
    const val CURRENCY_USD_ID = "currency_usd_id"

    const val ALLOW_EDIT_PRICE = "allow_edit_price"
    const val ALLOW_DELETE_OPERATION = "allow_delete_operation"
    const val ALLOW_EDIT_DATE = "allow_edit_date"

    const val AUTO_ADD_PURCHASE_PRICE = "auto_add_purchase_price"
    const val USE_LAST_PRICE = "use_last_price"
    const val UPDATE_AVERAGE_COST = "update_average_cost"

    const val ACCOUNT_CASH_ID = "account_cash_id"
    const val ACCOUNT_BANK_ID = "account_bank_id"
    const val ACCOUNT_CUSTOMERS_ID = "account_customers_id"
    const val ACCOUNT_SUPPLIERS_ID = "account_suppliers_id"
    const val ACCOUNT_INVENTORY_ID = "account_inventory_id"
    const val ACCOUNT_CAPITAL_ID = "account_capital_id"
    const val ACCOUNT_OPENING_ID = "account_opening_id"
    const val ACCOUNT_SALES_ID = "account_sales_id"
    const val ACCOUNT_OTHER_REVENUE_ID = "account_other_revenue_id"
    const val ACCOUNT_COGS_ID = "account_cogs_id"
    const val ACCOUNT_DISCOUNT_ID = "account_discount_id"
    const val ACCOUNT_TAX_ID = "account_tax_id"
    const val ACCOUNT_FEES_ID = "account_fees_id"
    const val ACCOUNT_GENERAL_EXPENSES_ID = "account_general_expenses_id"
    const val ACCOUNT_STOCK_LOSS_ID = "account_stock_loss_id"
}

object DefaultData {
    const val WAREHOUSE_MAIN = "المخزن الرئيسي"
    const val CASHBOX_MAIN = "الصندوق الرئيسي"
    const val GROUP_GENERAL = "عام"
    const val CATEGORY_GENERAL = "عام"
    const val USER_ADMIN = "مدير النظام"

    const val UNIT_PIECE = "حبة"
    const val UNIT_KILO = "كيلو"
    const val UNIT_CARTON = "كرتون"
    const val UNIT_BAG = "كيس"

    const val CURRENCY_LOCAL_CODE = "YR"
    const val CURRENCY_LOCAL_NAME = "محلي"
    const val CURRENCY_LOCAL_SYMBOL = "فلس"

    const val CURRENCY_USD_CODE = "USD"
    const val CURRENCY_USD_NAME = "دولار"
    const val CURRENCY_USD_SYMBOL = "سنت"
}
