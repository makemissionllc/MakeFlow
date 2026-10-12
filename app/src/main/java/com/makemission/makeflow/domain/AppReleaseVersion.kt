package com.makemission.makeflow.domain

object AppReleaseVersion {
    private val versionPattern = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:[-+][0-9A-Za-z.-]+)?$")

    fun isNewer(candidate: String, current: String): Boolean {
        val candidateParts = versionPattern.matchEntire(candidate.trim())
            ?.groupValues?.drop(1)?.take(3)?.mapNotNull { it.toIntOrNull() }
            ?: return false
        val currentParts = versionPattern.matchEntire(current.trim())
            ?.groupValues?.drop(1)?.take(3)?.mapNotNull { it.toIntOrNull() }
            ?: return false
        if (candidateParts.size != 3 || currentParts.size != 3) return false

        for (index in 0..2) {
            if (candidateParts[index] != currentParts[index]) {
                return candidateParts[index] > currentParts[index]
            }
        }
        return false
    }
}
