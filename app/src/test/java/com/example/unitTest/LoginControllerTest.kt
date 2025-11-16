
package com.example.unitTest

import android.content.Context
import android.content.SharedPreferences
import com.example.socialgate.controller.LoginController
import com.example.socialgate.model.User
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.LoginView
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class LoginControllerTest {

    private lateinit var mockView: LoginView
    private lateinit var mockRepository: UserRepository
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var controller: LoginController

    /**
     * Caso de prueba
     */

    @Before
    fun setup() {

        mockView = mock()
        mockRepository = mock()
        mockContext = mock()
        mockPrefs = mock()
        mockEditor = mock()

        whenever(mockContext.getSharedPreferences(any(), any())).thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)

        controller = LoginController(mockView, mockRepository, mockContext)
    }


    /**
     * Prueba del Caso de Prueba CP-002: Login Fallido
     */
    @Test
    fun `CP-002 Validar que un usuario con credenciales incorrectas no pueda iniciar sesión`() {
        whenever(mockRepository.findUserByCredentials("test", "wrongpass")).thenReturn(null)

        controller.login("test", "wrongpass")

        verify(mockView).onLoginFailure("Credenciales incorrectas")
        verify(mockView, never()).onLoginSuccess(any(), any())
        verify(mockView, never()).navigateToPermissions(any())
    }

//    @Test
//    fun `login con campos vacíos llama a showValidationError`() {
//        controller.login("", "")
//
//        verify(mockView).showValidationError("Por favor, ingrese sus credenciales")
//        verify(mockRepository, never()).findUserByCredentials(any(), any())
//    }

    /*@Test
    fun `login con contraseña corta llama a showValidationError`() {
        controller.login("test", "123")

        verify(mockView).showValidationError("La contraseña debe tener entre 6 y 20 caracteres.")
        verify(mockRepository, never()).findUserByCredentials(any(), any())
    }*/

    /**
     * Prueba de CP-001
     */
    @Test
    fun `CP-001  Validar que un usuario con credenciales correctas pueda iniciar sesión`() {

        val fakeUser = User(id = 1, username = "test", name = "Test User", email = "test@test.com")
        whenever(mockRepository.findUserByCredentials("test", "pass123")).thenReturn(fakeUser)
        whenever(mockPrefs.getBoolean(eq(LoginController.KEY_PERMISSIONS_GRANTED), any())).thenReturn(true)

        controller.login("test", "pass123")

        verify(mockView).onLoginSuccess(eq("Test User"), eq(1))
        verify(mockView, never()).onLoginFailure(any())
        verify(mockView, never()).navigateToPermissions(any())
    }

    /**
     * Prueba de CP-010 (Usuario regular)
     */
    @Test
    fun `CP-009 Validar que prototipo no notifique y redirija usuario sobre acciones configurables pertinentes`() {

        val fakeUser = User(id = 1, username = "test", name = "Test User", email = "test@test.com")
        whenever(mockRepository.findUserByCredentials("test", "pass123")).thenReturn(fakeUser)
        whenever(mockPrefs.getBoolean(eq(LoginController.KEY_PERMISSIONS_GRANTED), any())).thenReturn(true)

        controller.login("test", "pass123")

        verify(mockView).onLoginSuccess(eq("Test User"), eq(1))
        verify(mockView, never()).onLoginFailure(any())
        verify(mockView, never()).navigateToPermissions(any())
    }



    /**
     * Prueba de CP-009: Login de primera vez
     */
    @Test
    fun `CP-008 Validar que prototipo notifique y redirija al usuario sobre acciones configurables pertinentes`() {

        val fakeUser = User(id = 1, username = "test", name = "Test User", email = "test@test.com")
        whenever(mockRepository.findUserByCredentials("test", "pass123")).thenReturn(fakeUser)
        whenever(mockPrefs.getBoolean(eq(LoginController.KEY_PERMISSIONS_GRANTED), any())).thenReturn(false)

        controller.login("test", "pass123")

        verify(mockView).navigateToPermissions(1)
        verify(mockView, never()).onLoginSuccess(any(), any())
        verify(mockView, never()).onLoginFailure(any())
    }
}