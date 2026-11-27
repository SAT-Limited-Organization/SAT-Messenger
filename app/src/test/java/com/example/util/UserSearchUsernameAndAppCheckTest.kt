package com.example.util

import com.example.data.model.UserProfile
import com.example.data.repository.DeviceContactInfo
import com.example.data.repository.DeviceContactsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSearchUsernameAndAppCheckTest {

    private val alice = UserProfile(
        userId = "u_alice",
        displayName = "Alice Moyo",
        email = "alice.moyo@gmail.com",
        primaryUsername = "alice.moyo",
        secondaryUsername = "orbital_alice",
        phoneNumber = "+1 (555) 234-5678",
        availabilityStatus = "Available",
        isOnline = true
    )

    private val bob = UserProfile(
        userId = "u_bob",
        displayName = "Bob Dube",
        email = "bob.engineer@satlimited.com",
        primaryUsername = "bob.engineer",
        secondaryUsername = null,
        phoneNumber = "+263 77 123 4567",
        availabilityStatus = "Busy",
        isOnline = true
    )

    @Test
    fun userSearchMatcher_matchesAllFiveRegisteredFields() {
        // 1. Registered Name
        assertEquals(UserMatchField.NAME, UserSearchMatcher.matchField(alice, "Alice Moyo"))
        // 2. Primary Username (part before @) with and without '@'
        assertEquals(UserMatchField.PRIMARY_USERNAME, UserSearchMatcher.matchField(alice, "@alice.moyo"))
        assertEquals(UserMatchField.PRIMARY_USERNAME, UserSearchMatcher.matchField(bob, "bob.engineer"))
        // 3. Secondary Username with and without '@'
        assertEquals(UserMatchField.SECONDARY_USERNAME, UserSearchMatcher.matchField(alice, "orbital_alice"))
        assertEquals(UserMatchField.SECONDARY_USERNAME, UserSearchMatcher.matchField(alice, "@orbital_alice"))
        // 4. Email
        assertEquals(UserMatchField.EMAIL, UserSearchMatcher.matchField(alice, "alice.moyo@gmail.com"))
        // 5. Phone Number (raw formatting AND digits-only formatting)
        assertEquals(UserMatchField.PHONE, UserSearchMatcher.matchField(alice, "+1 (555) 234"))
        assertEquals(UserMatchField.PHONE, UserSearchMatcher.matchField(alice, "5552345678"))
        assertEquals(UserMatchField.PHONE, UserSearchMatcher.matchField(bob, "0771234567".drop(1)))
        // Non-matching query
        assertNull(UserSearchMatcher.matchField(alice, "nonexistent_handle"))
        assertFalse(UserSearchMatcher.matches(alice, "nonexistent_handle"))
    }

    @Test
    fun usernamePolicy_enforcesUniquenessThreePerDayAndTenDayCooldown() {
        val dayOneMs = 1_795_000_000_000L // fixed reference UTC day
        var profile = UserProfile(
            userId = "u_1",
            displayName = "Simba",
            email = "simba@gmail.com"
        )

        // Conflict with another user's secondary username -> rejected
        val other = UserProfile(userId = "u_2", displayName = "Other", secondaryUsername = "taken_handle")
        val conflictResult = UsernamePolicy.evaluateChange(profile, "TAKEN_HANDLE", dayOneMs, listOf(other))
        assertTrue(conflictResult is UsernameChangeEvaluation.Rejected)

        // Initial pick (free pick) succeeds and leaves full 3 changes available on change day
        val initialPick = UsernamePolicy.evaluateChange(profile, "@first_pick", dayOneMs, emptyList())
        assertTrue(initialPick is UsernameChangeEvaluation.Allowed)
        initialPick as UsernameChangeEvaluation.Allowed
        assertEquals("first_pick", initialPick.normalizedUsername)
        assertNull(initialPick.nextLastChangedAtMs)

        profile = profile.copy(
            secondaryUsername = initialPick.normalizedUsername,
            secondaryUsernameLower = initialPick.normalizedLower,
            secondaryUsernameDayKey = initialPick.nextDayKey,
            secondaryUsernameChangesInDay = initialPick.nextChangesInDay,
            secondaryUsernameLastChangedAtMs = initialPick.nextLastChangedAtMs
        )

        // 3 changes in the same day are allowed (1/3, 2/3, 3/3)
        for (attempt in 1..3) {
            val res = UsernamePolicy.evaluateChange(profile, "handle_v$attempt", dayOneMs + attempt * 1000L, emptyList())
            assertTrue("Expected change #$attempt of 3 to succeed", res is UsernameChangeEvaluation.Allowed)
            res as UsernameChangeEvaluation.Allowed
            assertEquals(attempt, res.nextChangesInDay)
            assertEquals(3 - attempt, res.remainingChangesTodayAfterSave)
            profile = profile.copy(
                secondaryUsername = res.normalizedUsername,
                secondaryUsernameLower = res.normalizedLower,
                secondaryUsernameDayKey = res.nextDayKey,
                secondaryUsernameChangesInDay = res.nextChangesInDay,
                secondaryUsernameLastChangedAtMs = res.nextLastChangedAtMs
            )
        }

        // 4th change in the same day -> rejected
        val fourthSameDay = UsernamePolicy.evaluateChange(profile, "handle_v4", dayOneMs + 10_000L, emptyList())
        assertTrue(fourthSameDay is UsernameChangeEvaluation.Rejected)

        // Next day (still within 10-day cooldown) -> rejected
        val dayTwoMs = dayOneMs + 24L * 60L * 60L * 1000L
        val withinCooldown = UsernamePolicy.evaluateChange(profile, "handle_day2", dayTwoMs, emptyList())
        assertTrue(withinCooldown is UsernameChangeEvaluation.Rejected)
        assertFalse(UsernamePolicy.describeStatus(profile, dayTwoMs).canChangeNow)

        // After 10 full days -> unlocked with 3 fresh changes
        val dayElevenMs = dayOneMs + UsernamePolicy.COOLDOWN_MS + 60_000L
        assertTrue(UsernamePolicy.describeStatus(profile, dayElevenMs).canChangeNow)
        val afterCooldown = UsernamePolicy.evaluateChange(profile, "handle_after_10d", dayElevenMs, emptyList())
        assertTrue(afterCooldown is UsernameChangeEvaluation.Allowed)
        afterCooldown as UsernameChangeEvaluation.Allowed
        assertEquals(1, afterCooldown.nextChangesInDay)
        assertEquals(2, afterCooldown.remainingChangesTodayAfterSave)
    }

    @Test
    fun deviceContactsRepository_matchesRegisteredUsersByEmailAndPhone() {
        val repo = DeviceContactsRepository()
        val phoneBook = listOf(
            DeviceContactInfo(
                displayName = "Alice Phone Entry",
                normalizedPhones = DeviceContactsRepository.phoneMatchVariants("555-234-5678"),
                emails = emptySet()
            ),
            DeviceContactInfo(
                displayName = "Bob Work",
                normalizedPhones = emptySet(),
                emails = setOf("bob.engineer@satlimited.com")
            )
        )

        val matches = repo.matchRegisteredUsers(
            deviceContacts = phoneBook,
            directoryUsers = listOf(alice, bob),
            currentUserId = "me_999"
        )

        assertEquals(2, matches.size)
        assertNotNull(matches["u_alice"])
        assertEquals("Alice Phone Entry", matches["u_alice"]?.deviceContactName)
        assertEquals("Phone", matches["u_alice"]?.matchedVia)

        assertNotNull(matches["u_bob"])
        assertEquals("Bob Work", matches["u_bob"]?.deviceContactName)
        assertEquals("Email", matches["u_bob"]?.matchedVia)
    }

    @Test
    fun resolveAppCheckProviderChoice_usesRecaptchaOnExternalDebugApkAndDebugWithIntentToken() {
        // Sideloaded / GitHub debug APK without launch Intent token -> reCAPTCHA
        assertEquals(
            AppCheckProviderChoice.RECAPTCHA,
            resolveAppCheckProviderChoice(
                isDebugBuild = true,
                intentDebugToken = null,
                hasExplicitStoredDebugToken = false,
                recaptchaSiteKey = "6LfS0iwuAAAAAILocgzsfnh0iXdAKRn1zyj9-Knl"
            )
        )

        // AI Studio streaming emulator with explicit Intent debug token -> Debug provider
        assertEquals(
            AppCheckProviderChoice.DEBUG,
            resolveAppCheckProviderChoice(
                isDebugBuild = true,
                intentDebugToken = "12345678-1234-1234-1234-1234567890ab",
                hasExplicitStoredDebugToken = false,
                recaptchaSiteKey = "6LfS0iwuAAAAAILocgzsfnh0iXdAKRn1zyj9-Knl"
            )
        )

        // Release build always uses reCAPTCHA
        assertEquals(
            AppCheckProviderChoice.RECAPTCHA,
            resolveAppCheckProviderChoice(
                isDebugBuild = false,
                intentDebugToken = "ignored-in-release",
                hasExplicitStoredDebugToken = true,
                recaptchaSiteKey = "6LfS0iwuAAAAAILocgzsfnh0iXdAKRn1zyj9-Knl"
            )
        )
    }
}
