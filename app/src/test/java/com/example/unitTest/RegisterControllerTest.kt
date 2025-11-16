package com.example.unitTest

import com.example.socialgate.controller.RegisterController
import com.example.socialgate.model.TimeControlRepository
import com.example.socialgate.model.UserExistsResult
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.RegisterView
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class RegisterControllerTest {

    private lateinit var mockView: RegisterView
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockTimeControlRepo: TimeControlRepository
    private lateinit var controller: RegisterController

    private val validUser = "newUser"
    private val validName = "Test"
    private val validSubname = "User"
    private val validEmail = "test@user.com"
    private val validPass = "password123"

    @Before
    fun setup() {
        mockView = mock()
        mockUserRepo = mock()
        mockTimeControlRepo = mock()
        controller = RegisterController(mockView, mockUserRepo, mockTimeControlRepo)
    }

    /**
     * Prueba del Caso de Prueba CP-003: Registro Exitoso
     */
    @Test
    fun `PU - Validar que un usuario pueda registrarse en el prototipo`() {

        whenever(mockUserRepo.checkUserExists(any(), any())).thenReturn(UserExistsResult.NONE)

        whenever(mockUserRepo.createUser(any(), any(), any(), any(), any())).thenReturn(1L)


        controller.registerUser(validUser, validName, validSubname, validEmail, validPass, validPass)


        verify(mockUserRepo).createUser(any(), eq("$validName $validSubname"), any(), any(), any())

        verify(mockTimeControlRepo).createDefaultEntries(1L)
        verify(mockView).onRegistrationSuccess()
        verify(mockView, never()).onRegistrationFailure()
    }

    /**
     * Prueba del Caso de Prueba CP-004: Usuario Duplicado
     */
    @Test
    fun `PU - Validar que un usuario no pueda registrarse en el prototipo al ingresar credenciales pertenecientes a otro usuario`() {
        whenever(mockUserRepo.checkUserExists(any(), any())).thenReturn(UserExistsResult.USERNAME_TAKEN)

        controller.registerUser(validUser, validName, validSubname, validEmail, validPass, validPass)

        verify(mockView).showUserExistsError("El nombre de usuario ya está en uso.")
        verify(mockUserRepo, never()).createUser(any(), any(), any(), any(), any())
        verify(mockView, never()).onRegistrationSuccess()
    }

    @Test
    fun `PU - registro con email duplicado llama a showUserExistsError`() {
        whenever(mockUserRepo.checkUserExists(any(), any())).thenReturn(UserExistsResult.EMAIL_TAKEN)

        controller.registerUser(validUser, validName, validSubname, validEmail, validPass, validPass)

        verify(mockView).showUserExistsError("El correo electrónico ya está registrado.")
        verify(mockUserRepo, never()).createUser(any(), any(), any(), any(), any())
    }

    @Test
    fun `PU - registro con campos vacíos llama a showValidationError`() {
        controller.registerUser("", "Test", "User", "email", "pass", "pass")

        verify(mockView).showValidationError("Por favor, complete todos los campos")
        verify(mockUserRepo, never()).checkUserExists(any(), any())
    }

    @Test
    fun `PU - registro con contraseñas que no coinciden llama a passwordsDoNotMatchError`() {
        controller.registerUser(validUser, validName, validSubname, validEmail, "pass1", "pass2")

        verify(mockView).passwordsDoNotMatchError()
        verify(mockUserRepo, never()).checkUserExists(any(), any())
    }
}