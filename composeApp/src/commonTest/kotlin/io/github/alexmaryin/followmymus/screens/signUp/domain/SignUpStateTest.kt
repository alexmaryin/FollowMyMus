package io.github.alexmaryin.followmymus.screens.signUp.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignUpStateTest {

    @Test
    fun `nicknameValidation should return true for valid nicknames`() {
        val state = SignUpState(nickname = "valid_nickname")
        assertTrue(state.nicknameValidation())

        val stateWithDot = SignUpState(nickname = "valid.nickname")
        assertTrue(stateWithDot.nicknameValidation())

        val stateWithNumbers = SignUpState(nickname = "valid123")
        assertTrue(stateWithNumbers.nicknameValidation())
    }

    @Test
    fun `nicknameValidation should return false for invalid nicknames`() {
        val stateWithSpace = SignUpState(nickname = "invalid nickname")
        assertFalse(stateWithSpace.nicknameValidation())

        val stateWithSpecialChar = SignUpState(nickname = "invalid-nickname")
        assertFalse(stateWithSpecialChar.nicknameValidation())

        val emptyState = SignUpState(nickname = "")
        assertFalse(emptyState.nicknameValidation())

        val blankState = SignUpState(nickname = "  ")
        assertFalse(blankState.nicknameValidation())
    }

    @Test
    fun `passwordValidation should return true for valid passwords`() {
        val state = SignUpState(password = "ValidPass1")
        assertTrue(state.passwordValidation())

        val stateWithSpecialChars = SignUpState(password = "ValidPass1@#$")
        assertTrue(stateWithSpecialChars.passwordValidation())
    }

    @Test
    fun `passwordValidation should return false for invalid passwords`() {
        val shortPassword = SignUpState(password = "Vp1")
        assertFalse(shortPassword.passwordValidation())

        val noUpperCase = SignUpState(password = "validpass1")
        assertFalse(noUpperCase.passwordValidation())

        val noLowerCase = SignUpState(password = "VALIDPASS1")
        assertFalse(noLowerCase.passwordValidation())

        val noDigit = SignUpState(password = "ValidPassword")
        assertFalse(noDigit.passwordValidation())
    }
}
