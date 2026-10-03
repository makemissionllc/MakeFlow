package com.makemission.makeflow.core.haptics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticManagerTest {

    @Test
    fun isLowPriorityEvent_returnsTrueForTAB_SWITCH() {
        assertTrue(isLowPriorityEvent(HapticEvent.TAB_SWITCH))
    }

    @Test
    fun isLowPriorityEvent_returnsTrueForNAV_SELECT() {
        assertTrue(isLowPriorityEvent(HapticEvent.NAV_SELECT))
    }

    @Test
    fun isLowPriorityEvent_returnsTrueForTOGGLE() {
        assertTrue(isLowPriorityEvent(HapticEvent.TOGGLE))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForTRANSACTION_SAVED() {
        assertFalse(isLowPriorityEvent(HapticEvent.TRANSACTION_SAVED))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForTRANSACTION_DELETED() {
        assertFalse(isLowPriorityEvent(HapticEvent.TRANSACTION_DELETED))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForBUDGET_EXCEEDED() {
        assertFalse(isLowPriorityEvent(HapticEvent.BUDGET_EXCEEDED))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForVALIDATION_ERROR() {
        assertFalse(isLowPriorityEvent(HapticEvent.VALIDATION_ERROR))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForDELETE_UNDO_AVAILABLE() {
        assertFalse(isLowPriorityEvent(HapticEvent.DELETE_UNDO_AVAILABLE))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForSWIPE_ACTION_ARMED() {
        assertFalse(isLowPriorityEvent(HapticEvent.SWIPE_ACTION_ARMED))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForBUDGET_WARNING() {
        assertFalse(isLowPriorityEvent(HapticEvent.BUDGET_WARNING))
    }

    @Test
    fun isLowPriorityEvent_returnsFalseForONBOARDING_STEP_COMPLETE() {
        assertFalse(isLowPriorityEvent(HapticEvent.ONBOARDING_STEP_COMPLETE))
    }
}
