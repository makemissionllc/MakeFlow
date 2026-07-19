package com.makemission.makeflow.ui.viewmodel

sealed class FocusedAccountState {
    object Loading : FocusedAccountState()
    object ShowAll : FocusedAccountState()
    data class Focused(val accountId: String) : FocusedAccountState()

    val accountIdOrNull: String? get() = when (this) {
        is Loading -> null
        is ShowAll -> null
        is Focused -> accountId
    }
}
