package com.makemission.makeflow.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseVersionTest {
    @Test
    fun detectsNewerPatchMinorAndMajorVersions() {
        assertTrue(AppReleaseVersion.isNewer("2.0.14-community", "2.0.13-community"))
        assertTrue(AppReleaseVersion.isNewer("v2.1.0", "2.0.99-community"))
        assertTrue(AppReleaseVersion.isNewer("3.0.0", "2.99.99"))
    }

    @Test
    fun rejectsSameOlderAndInvalidVersions() {
        assertFalse(AppReleaseVersion.isNewer("2.0.13", "2.0.13-community"))
        assertFalse(AppReleaseVersion.isNewer("2.0.12-community", "2.0.13-community"))
        assertFalse(AppReleaseVersion.isNewer("release-2.0.14", "2.0.13-community"))
    }
}
