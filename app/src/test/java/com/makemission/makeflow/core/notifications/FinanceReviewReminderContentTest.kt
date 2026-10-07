package com.makemission.makeflow.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceReviewReminderContentTest {
    @Test
    fun body_usesFirstNameWhenProvided() {
        assertEquals(
            "Amina, take a moment to check in on your spending and progress.",
            FinanceReviewReminderContent.body("  Amina Wanjiku  ")
        )
    }

    @Test
    fun bodyUsesNeutralCopyWhenNameIsBlank() {
        assertEquals(
            "Take a moment to check in on your spending and progress.",
            FinanceReviewReminderContent.body("  ")
        )
    }
}
