package com.example.unitTest

import com.example.socialgate.controller.ReportController
import com.example.socialgate.model.ReportData
import com.example.socialgate.model.ReportRepository
import com.example.socialgate.view.view_interfaces.ReportView
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class ReportControllerTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var mockView: ReportView
    private lateinit var mockRepository: ReportRepository
    private lateinit var controller: ReportController
    private val FAKE_USER_ID = 1

    @Before
    fun setup() {
        mockView = mock()
        mockRepository = mock()
        controller = ReportController(
            mockView,
            FAKE_USER_ID,
            mockRepository
        )
    }

    @Test
    fun `CP-005 Validar que el prototipo registre el tiempo de uso de las redes sociales Facebook e Instagram`() = runTest {
        val fakeData = ReportData(
            userName = "Test User", userEmail = "test@user.com", totalUsageSeconds = 300,
            totalDaysWithActivity = 5, facebookUsageSeconds = 120, instagramUsageSeconds = 180,
            dailyUsageMinutes = FloatArray(7)
        )
        whenever(mockRepository.getReportData(FAKE_USER_ID)).thenReturn(fakeData)

        controller.loadData()

        verify(mockView).displayReportData(eq(fakeData))
        verify(mockView, never()).displayError(any())
    }

    @Test
    fun `CP-010 Validar que prototipo permita genera reportes sobre el monitoreo y tiempo de uso`() = runTest {
        val fakeData = ReportData(
            userName = "Test User", userEmail = "test@user.com", totalUsageSeconds = 300,
            totalDaysWithActivity = 5, facebookUsageSeconds = 120, instagramUsageSeconds = 180,
            dailyUsageMinutes = FloatArray(7)
        )
        whenever(mockRepository.getReportData(FAKE_USER_ID)).thenReturn(fakeData)

        controller.loadData()

        controller.onGeneratePdfClicked()

        verify(mockView).generatePdf(eq(fakeData))
        verify(mockView, never()).showNoDataToGenerateReportError()
    }

    @Test
    fun `CP-011 Validar que prototipo no permita genera reportes sobre el monitoreo y tiempo de uso`() = runTest {
        val emptyData = ReportData(
            userName = "Test User", userEmail = "test@user.com", totalUsageSeconds = 0,
            totalDaysWithActivity = 0, facebookUsageSeconds = 0, instagramUsageSeconds = 0,
            dailyUsageMinutes = FloatArray(7)
        )
        whenever(mockRepository.getReportData(FAKE_USER_ID)).thenReturn(emptyData)

        controller.loadData()

        controller.onGeneratePdfClicked()

        verify(mockView).showNoDataToGenerateReportError()
        verify(mockView, never()).generatePdf(any())
    }
}