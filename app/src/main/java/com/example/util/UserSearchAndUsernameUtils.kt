package com.example.util

import com.example.data.model.UserProfile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class UserMatchField(val label: String) {
    NAME("Registered Name"),
    PRIMARY_USERNAME("Primary Username"),
    SECONDARY_USERNAME("Secondary Username"),
    EMAIL("Email"),
    PHONE("Phone Number"),
    STATUS("Status")
}

object UserSearchMatcher {

    fun normalizePhoneDigits(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.filter { it.isDigit() }
    }

    fun matchField(
        user: UserProfile,
        rawQuery: String,
        includeStatusFields: Boolean = false
    ): UserMatchField? {
        val query = rawQuery.trim()
        if (query.isBlank()) return null

        val handleQuery = query.removePrefix("@").trim()
        val queryDigits = normalizePhoneDigits(query)
        val looksLikePhoneQuery = queryDigits.length >= 3 &&
            query.all { it.isDigit() || it in "+-() ." }

        if (user.displayName.contains(query, ignoreCase = true)) {
            return UserMatchField.NAME
        }
        if (handleQuery.isNotBlank() &&
            user.resolvedPrimaryUsername.contains(handleQuery, ignoreCase = true)
        ) {
            return UserMatchField.PRIMARY_USERNAME
        }
        if (handleQuery.isNotBlank() &&
            !user.secondaryUsername.isNullOrBlank() &&
            user.secondaryUsername.contains(handleQuery, ignoreCase = true)
        ) {
            return UserMatchField.SECONDARY_USERNAME
        }
        if (user.email.contains(query, ignoreCase = true)) {
            return UserMatchField.EMAIL
        }
        if (!user.phoneNumber.isNullOrBlank()) {
            val phoneMatchesRaw = user.phoneNumber.contains(query, ignoreCase = true)
            val phoneDigits = normalizePhoneDigits(user.phoneNumber)
            val phoneMatchesDigits = looksLikePhoneQuery && phoneDigits.contains(queryDigits)
            if (phoneMatchesRaw || phoneMatchesDigits) {
                return UserMatchField.PHONE
            }
        }
        if (includeStatusFields) {
            val statusMatches = (user.statusMessage?.contains(query, ignoreCase = true) == true) ||
                user.resolvedAvailability().label.contains(query, ignoreCase = true)
            if (statusMatches) {
                return UserMatchField.STATUS
            }
        }
        return null
    }

    fun matches(
        user: UserProfile,
        rawQuery: String,
        includeStatusFields: Boolean = false
    ): Boolean {
        val query = rawQuery.trim()
        if (query.isBlank()) return true
        return matchField(user, query, includeStatusFields) != null
    }
}

sealed interface UsernameChangeEvaluation {
    data class Allowed(
        val normalizedUsername: String,
        val normalizedLower: String,
        val nextDayKey: String,
        val nextChangesInDay: Int,
        val nextLastChangedAtMs: Long?,
        val remainingChangesTodayAfterSave: Int
    ) : UsernameChangeEvaluation

    data class Rejected(val reason: String) : UsernameChangeEvaluation
}

data class SecondaryUsernameStatus(
    val currentUsername: String?,
    val canChangeNow: Boolean,
    val remainingChangesToday: Int,
    val cooldownRemainingMs: Long,
    val statusSummary: String
)

object UsernamePolicy {
    const val MAX_CHANGES_PER_DAY = 3
    const val COOLDOWN_DAYS = 10L
    const val COOLDOWN_MS = COOLDOWN_DAYS * 24L * 60L * 60L * 1000L

    private val USERNAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9._]{1,28}[a-zA-Z0-9]$")

    fun normalize(raw: String): String {
        return raw.trim().removePrefix("@").trim()
    }

    fun isValidFormat(normalized: String): Boolean {
        if (normalized.length !in 3..30) return false
        if (normalized.contains("..")) return false
        return USERNAME_REGEX.matches(normalized)
    }

    fun dayKeyFor(timestampMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(timestampMs))
    }

    fun describeStatus(
        profile: UserProfile?,
        nowMs: Long = System.currentTimeMillis()
    ): SecondaryUsernameStatus {
        val current = profile?.secondaryUsername?.trim()?.takeIf { it.isNotBlank() }
        if (profile == null || current == null) {
            return SecondaryUsernameStatus(
                currentUsername = null,
                canChangeNow = true,
                remainingChangesToday = MAX_CHANGES_PER_DAY,
                cooldownRemainingMs = 0L,
                statusSummary = "Pick any unpicked username. After setting it, you can change it up to 3 times on an active change day, with a 10-day cooldown between change windows."
            )
        }

        val lastChangedAtMs = profile.secondaryUsernameLastChangedAtMs
        val todayKey = dayKeyFor(nowMs)

        if (lastChangedAtMs == null) {
            return SecondaryUsernameStatus(
                currentUsername = current,
                canChangeNow = true,
                remainingChangesToday = MAX_CHANGES_PER_DAY,
                cooldownRemainingMs = 0L,
                statusSummary = "$MAX_CHANGES_PER_DAY of $MAX_CHANGES_PER_DAY changes available today (10-day cooldown applies after today)."
            )
        }

        if (profile.secondaryUsernameDayKey == todayKey) {
            val remaining = (MAX_CHANGES_PER_DAY - profile.secondaryUsernameChangesInDay).coerceAtLeast(0)
            return if (remaining > 0) {
                SecondaryUsernameStatus(
                    currentUsername = current,
                    canChangeNow = true,
                    remainingChangesToday = remaining,
                    cooldownRemainingMs = 0L,
                    statusSummary = "$remaining of $MAX_CHANGES_PER_DAY changes remaining today."
                )
            } else {
                val cooldownEnd = lastChangedAtMs + COOLDOWN_MS
                val remainingMs = (cooldownEnd - nowMs).coerceAtLeast(0L)
                SecondaryUsernameStatus(
                    currentUsername = current,
                    canChangeNow = false,
                    remainingChangesToday = 0,
                    cooldownRemainingMs = remainingMs,
                    statusSummary = "Daily limit (3/3) reached. Next change available in ${formatDuration(remainingMs)}."
                )
            }
        }

        val cooldownEnd = lastChangedAtMs + COOLDOWN_MS
        val remainingMs = (cooldownEnd - nowMs).coerceAtLeast(0L)
        return if (remainingMs == 0L) {
            SecondaryUsernameStatus(
                currentUsername = current,
                canChangeNow = true,
                remainingChangesToday = MAX_CHANGES_PER_DAY,
                cooldownRemainingMs = 0L,
                statusSummary = "10-day cooldown complete • $MAX_CHANGES_PER_DAY changes available today."
            )
        } else {
            SecondaryUsernameStatus(
                currentUsername = current,
                canChangeNow = false,
                remainingChangesToday = 0,
                cooldownRemainingMs = remainingMs,
                statusSummary = "Cooldown active: next change available in ${formatDuration(remainingMs)} (10-day rule)."
            )
        }
    }

    fun evaluateChange(
        currentProfile: UserProfile,
        candidateRaw: String,
        nowMs: Long = System.currentTimeMillis(),
        otherUsers: List<UserProfile> = emptyList()
    ): UsernameChangeEvaluation {
        val normalized = normalize(candidateRaw)
        if (normalized.isBlank()) {
            return UsernameChangeEvaluation.Rejected("Enter a secondary username to claim.")
        }
        if (!isValidFormat(normalized)) {
            return UsernameChangeEvaluation.Rejected(
                "Username must be 3–30 characters using letters, numbers, '.' or '_' (no '..')."
            )
        }

        val lower = normalized.lowercase(Locale.US)
        if (currentProfile.secondaryUsername == normalized) {
            return UsernameChangeEvaluation.Rejected("You are already using @$normalized.")
        }

        val conflict = otherUsers.any { other ->
            other.userId != currentProfile.userId &&
                other.secondaryUsername?.trim()?.lowercase(Locale.US) == lower
        }
        if (conflict) {
            return UsernameChangeEvaluation.Rejected("@$normalized is already taken. Choose an unpicked username.")
        }

        val todayKey = dayKeyFor(nowMs)
        val hasExistingSecondary = !currentProfile.secondaryUsername.isNullOrBlank()
        if (!hasExistingSecondary) {
            return UsernameChangeEvaluation.Allowed(
                normalizedUsername = normalized,
                normalizedLower = lower,
                nextDayKey = todayKey,
                nextChangesInDay = 0,
                nextLastChangedAtMs = null,
                remainingChangesTodayAfterSave = MAX_CHANGES_PER_DAY
            )
        }

        val lastChangedAtMs = currentProfile.secondaryUsernameLastChangedAtMs
        if (lastChangedAtMs == null) {
            return UsernameChangeEvaluation.Allowed(
                normalizedUsername = normalized,
                normalizedLower = lower,
                nextDayKey = todayKey,
                nextChangesInDay = 1,
                nextLastChangedAtMs = nowMs,
                remainingChangesTodayAfterSave = MAX_CHANGES_PER_DAY - 1
            )
        }

        if (currentProfile.secondaryUsernameDayKey == todayKey) {
            val usedToday = currentProfile.secondaryUsernameChangesInDay
            if (usedToday >= MAX_CHANGES_PER_DAY) {
                val remainingMs = (lastChangedAtMs + COOLDOWN_MS - nowMs).coerceAtLeast(0L)
                return UsernameChangeEvaluation.Rejected(
                    "Daily limit of $MAX_CHANGES_PER_DAY changes reached. Next change available in ${formatDuration(remainingMs)} (10-day cooldown)."
                )
            }
            val nextCount = usedToday + 1
            return UsernameChangeEvaluation.Allowed(
                normalizedUsername = normalized,
                normalizedLower = lower,
                nextDayKey = todayKey,
                nextChangesInDay = nextCount,
                nextLastChangedAtMs = nowMs,
                remainingChangesTodayAfterSave = (MAX_CHANGES_PER_DAY - nextCount).coerceAtLeast(0)
            )
        }

        val cooldownEnd = lastChangedAtMs + COOLDOWN_MS
        if (nowMs < cooldownEnd) {
            val remainingMs = cooldownEnd - nowMs
            return UsernameChangeEvaluation.Rejected(
                "Secondary username can be changed again after 10 days (${formatDuration(remainingMs)} remaining)."
            )
        }

        return UsernameChangeEvaluation.Allowed(
            normalizedUsername = normalized,
            normalizedLower = lower,
            nextDayKey = todayKey,
            nextChangesInDay = 1,
            nextLastChangedAtMs = nowMs,
            remainingChangesTodayAfterSave = MAX_CHANGES_PER_DAY - 1
        )
    }

    fun formatDuration(durationMs: Long): String {
        val totalMinutes = (durationMs / 60_000L).coerceAtLeast(1L)
        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L
        return when {
            days > 0 && hours > 0 -> "${days}d ${hours}h"
            days > 0 -> "${days}d"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
}
