package com.example.unitTest

import android.content.Context
import android.content.SharedPreferences
import com.example.socialgate.controller.ScheduleController
import com.example.socialgate.model.ScheduleRepository
import com.example.socialgate.model.TimeControlRepository
import com.example.socialgate.view.activity.ScheduleActivity
import com.example.socialgate.view.view_interfaces.ScheduleView
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class ScheduleControllerTest {

    private lateinit var mockView: ScheduleView
    private lateinit var mockContext: Context
    private lateinit var mockTimeControlRepo: TimeControlRepository
    private lateinit var mockScheduleRepo: ScheduleRepository
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var controller: ScheduleController
    private val FAKE_USER_ID = 1

    @Before
    fun setup() {
        mockView = mock()
        mockContext = mock()
        mockTimeControlRepo = mock()
        mockScheduleRepo = mock()
        mockPrefs = mock()
        mockEditor = mock()

        whenever(mockContext.getSharedPreferences(eq(ScheduleActivity.PREFS_NAME), any()))
            .thenReturn(mockPrefs)
        whenever(mockPrefs.edit()).thenReturn(mockEditor)

        controller = ScheduleController(
            mockView,
            FAKE_USER_ID,
            mockContext,
            mockTimeControlRepo,
            mockScheduleRepo
        )
    }

    /**
     * Prueba de CP-007: Habilitar límite de tiempo
     */
    @Test
    fun `CP-006 Validar que el usuario pueda establecer límites de tiempo a las redes sociales Facebook e Instagram`() {

        whenever(mockPrefs.getFloat(eq(ScheduleActivity.KEY_SAVED_LIMIT), any())).thenReturn(2.5f)

        controller.onTimeLimitSwitchChanged(true, 0f)

        verify(mockTimeControlRepo).saveTimeLimit(FAKE_USER_ID, 150)

        verify(mockView).displayTimeLimit(2.5f, true)
        verify(mockView).showToast("Límite de tiempo activado")
    }

    /**
     * Prueba de CP-008: Deshabilitar límite de tiempo
     */
    @Test
    fun `CP-007 Validar que el usuario no pueda establecer límites de tiempo a las redes sociales Facebook e Instagram`() {

        controller.onTimeLimitSwitchChanged(false, 3.0f)

        verify(mockTimeControlRepo).saveTimeLimit(FAKE_USER_ID, 0)

        verify(mockView).displayTimeLimit(3.0f, false)
        verify(mockView).showToast("Límite de tiempo desactivado")
    }

    /**
     * Prueba de CP-013: Habilitar horario académico
     */
    @Test
    fun `CP-012 Validar que el usuario pueda establecer un horario académico a las redes sociales Facebook e Instagram`() {

        controller.onAcademicScheduleSwitchChanged(true)

        verify(mockScheduleRepo).updateScheduleStatus(FAKE_USER_ID, true)
        verify(mockView).showToast("Horario académico activado")
        verify(mockView, never()).resetAcademicScheduleUI()
    }

    /**
     * Prueba de CP-014: Deshabilitar horario académico
     */
    @Test
    fun `CP-013 Validar que el usuario pueda establecer un horario académico a las redes sociales Facebook e Instagram`() {
        controller.onAcademicScheduleSwitchChanged(false)

        verify(mockScheduleRepo).updateScheduleStatus(FAKE_USER_ID, false)
        verify(mockView).resetAcademicScheduleUI()
        verify(mockView).showToast("Horario académico desactivado")
    }

//    @Test
//    fun `onDayClicked gestiona la selección de días`() {
//        val result1 = controller.onDayClicked("Lunes")
//        val result2 = controller.onDayClicked("Lunes")
//
//        assert(result1 == true)
//        assert(result2 == false)
//    }

//    @Test
//    fun `saveNewSchedule sin días seleccionados muestra error`() {
//        controller.saveNewSchedule("08:00", "10:00")
//
//        verify(mockView).showToast("Por favor, seleccione al menos un día")
//        verify(mockScheduleRepo, never()).saveSchedule(any(), any(), any(), any())
//    }
}