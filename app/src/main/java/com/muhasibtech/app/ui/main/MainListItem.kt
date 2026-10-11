package com.muhasibtech.app.ui.main

sealed class MainListItem {

    data class Header(
        val id: Int,
        val title: String,
        val expanded: Boolean
    ) : MainListItem()

    data class Child(
        val id: String,
        val title: String
    ) : MainListItem()
}
