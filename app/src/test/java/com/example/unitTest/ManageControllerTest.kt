package com.example.unitTest

import android.content.Context
import com.example.socialgate.controller.ManageController
import com.example.socialgate.model.ScheduleRepository
import com.example.socialgate.model.UserRepository
import com.example.socialgate.view.view_interfaces.ManageView
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ManageControllerTest {

    private lateinit var mockView: ManageView
    private lateinit var mockUserRepo: UserRepository
    private lateinit var mockScheduleRepo: ScheduleRepository
    private lateinit var mockContext: Context
    private lateinit var controller: ManageController

    private val FAKE_USER_ID = 1

    @Before
    fun setup() {
        mockView = mock()
        mockUserRepo = mock()
        mockScheduleRepo = mock()
        mockContext = mock()

        controller = ManageController(
            mockView,
            FAKE_USER_ID,
            mockContext,
            mockUserRepo,
            mockScheduleRepo
        )
    }

    /**
     * Prueba de CP-004: Validar que no se pueda actualizar a un usuario duplicado
     */
//    @Test
//    fun `CP-006 Validar que un usuario no pueda actualizar sus credenciales si son pertenecientes a otro usuario`() {
//
//        whenever(mockUserRepo.isUsernameTaken(eq("usertomado"), eq(FAKE_USER_ID))).thenReturn(true)
//
//        controller.updateUserData("usertomado", "Nombre", "email@valido.com", "")
//
//        verify(mockView).showUsernameTakenError()
//        verify(mockUserRepo, never()).updateUser(any(), any(), any(), any(), any())
//    }

//    @Test
//    fun `updateUserData con email duplicado llama a showEmailTakenError`() {
//
//        whenever(mockUserRepo.isUsernameTaken(any(), any())).thenReturn(false) // El username está bien
//        whenever(mockUserRepo.isEmailTaken(eq("email_tomado@a.com"), eq(FAKE_USER_ID))).thenReturn(true)
//
//        controller.updateUserData("uservalido", "Nombre", "email_tomado@a.com", "")
//
//        verify(mockView).showEmailTakenError()
//        verify(mockUserRepo, never()).updateUser(any(), any(), any(), any(), any())
//    }

//    @Test
//    fun `updateUserData con contraseña duplicada muestra error`() {
//
//        whenever(mockUserRepo.isUsernameTaken(any(), any())).thenReturn(false)
//        whenever(mockUserRepo.isEmailTaken(any(), any())).thenReturn(false)
//        whenever(mockUserRepo.checkPasswordExists("pass123")).thenReturn(true)
//
//        controller.updateUserData("uservalido", "Nombre", "email@valido.com", "pass123")
//
//        verify(mockView).showValidationError("La contraseña ya está en uso. Por favor, ingrese otra.")
//        verify(mockUserRepo, never()).updateUser(any(), any(), any(), any(), any())
//    }

//    @Test
//    fun `updateUserData con datos válidos llama a showUpdateSuccess`() {
//
//        whenever(mockUserRepo.isUsernameTaken(any(), any())).thenReturn(false)
//        whenever(mockUserRepo.isEmailTaken(any(), any())).thenReturn(false)
//        whenever(mockUserRepo.checkPasswordExists(any())).thenReturn(false)
//        whenever(mockUserRepo.updateUser(any(), any(), any(), any(), any())).thenReturn(1)
//
//        controller.updateUserData("uservalido", "Nombre", "email@valido.com", "nuevaPass")
//
//        verify(mockUserRepo).updateUser(eq(FAKE_USER_ID), eq("uservalido"), eq("Nombre"), eq("email@valido.com"), eq("nuevaPass"))
//        verify(mockView).showUpdateSuccess()
//    }

    /**
     * Prueba de CP-013: Lógica de Logout
     */
//    @Test
//    fun `logout llama a deleteSchedulesForUser y navigateToLogin`() {
//        controller.logout()
//
//        verify(mockScheduleRepo).deleteSchedulesForUser(FAKE_USER_ID)
//        verify(mockView).navigateToLogin()
//        verify(mockContext).stopService(any())
//    }
}