package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TicketType
import com.example.data.model.UserRole
import com.example.ui.components.ApplyJobModal
import com.example.ui.components.CheckInSuccessModal
import com.example.ui.components.CreateTicketModal
import com.example.ui.components.HrpBottomBar
import com.example.ui.components.HrpTopBar
import com.example.ui.components.RoleSwitcherDialog
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobBoardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PayrollScreen
import com.example.ui.screens.TicketScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.HrpViewModel
import com.example.ui.viewmodel.NavigationTab
import com.example.utils.LocationHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost(
    viewModel: HrpViewModel = viewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val loginErrorMessage by viewModel.loginErrorMessage.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val context = LocalContext.current

    // Request notification permission on Android 13+ (API 33)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    AnimatedContent(
        targetState = isLoggedIn,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AuthHostTransition"
    ) { loggedIn ->
        if (loggedIn) {
            HrpApp(viewModel = viewModel)
        } else {
            LoginScreen(
                isAuthenticating = isAuthenticating,
                errorMessage = loginErrorMessage,
                onLogin = { phone, password ->
                    viewModel.login(phone, password)
                }
            )
        }
    }
}

@Composable
fun HrpApp(
    viewModel: HrpViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentRole by viewModel.currentRole.collectAsState()
    val workerProfile by viewModel.workerProfile.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val payslip by viewModel.payslip.collectAsState()
    val tickets by viewModel.tickets.collectAsState()
    val jobListings by viewModel.jobListings.collectAsState()
    val ctvCandidates by viewModel.ctvCandidates.collectAsState()
    val bodMetrics by viewModel.bodMetrics.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val lastGpsCheckIn by viewModel.lastGpsCheckIn.collectAsState()

    val showRoleSwitcher by viewModel.showRoleSwitcher.collectAsState()
    val showCreateTicketDialog by viewModel.showCreateTicketDialog.collectAsState()
    val showAdvanceSalaryDialog by viewModel.showAdvanceSalaryDialog.collectAsState()
    val showCheckInSuccessDialog by viewModel.showCheckInSuccessDialog.collectAsState()
    val showApplyJobDialog by viewModel.showApplyJobDialog.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // GPS Hardware runtime permission launcher for Check-In
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        coroutineScope.launch {
            if (fineGranted || coarseGranted) {
                val loc = viewModel.locationHelper.getCurrentLocation()
                viewModel.checkIn(loc)
            } else {
                // If permission denied by user, attempt check-in with fallback location
                viewModel.checkIn(null)
            }
        }
    }

    val performGpsCheckIn = {
        if (LocationHelper.hasLocationPermission(context)) {
            coroutineScope.launch {
                val loc = viewModel.locationHelper.getCurrentLocation()
                viewModel.checkIn(loc)
            }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Handle back button: return to Home tab if on a sub-screen
    BackHandler(enabled = currentTab != NavigationTab.HOME) {
        viewModel.selectTab(NavigationTab.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HrpTopBar(
                currentRole = currentRole,
                onOpenRoleSwitcher = { viewModel.setRoleSwitcherVisible(true) },
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            HrpBottomBar(
                currentTab = currentTab,
                pendingTicketsCount = tickets.count {
                    it.status == com.example.data.model.TicketStatus.PENDING ||
                        it.status == com.example.data.model.TicketStatus.HR_APPROVED
                },
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "HrpTabTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.HOME -> {
                        HomeScreen(
                            currentRole = currentRole,
                            workerProfile = workerProfile,
                            payslip = payslip,
                            tickets = tickets,
                            bodMetrics = bodMetrics,
                            onNavigateTab = { viewModel.selectTab(it) },
                            onCheckIn = { performGpsCheckIn() },
                            onCheckOut = { viewModel.checkOut() },
                            onRequestAdvanceSalary = { viewModel.setAdvanceSalaryDialogVisible(true) },
                            onCreateTicket = { viewModel.setCreateTicketDialogVisible(true) },
                            onApproveAllAttendance = { viewModel.approveAllAttendance() }
                        )
                    }

                    NavigationTab.ATTENDANCE -> {
                        AttendanceScreen(
                            currentRole = currentRole,
                            attendanceList = attendanceList,
                            onCheckIn = { performGpsCheckIn() },
                            onCheckOut = { viewModel.checkOut() },
                            onDisputeAttendance = { viewModel.setCreateTicketDialogVisible(true) },
                            onApproveAll = { viewModel.approveAllAttendance() }
                        )
                    }

                    NavigationTab.PAYROLL -> {
                        PayrollScreen(
                            currentRole = currentRole,
                            workerProfile = workerProfile,
                            payslip = payslip,
                            onRequestAdvanceSalary = { viewModel.setAdvanceSalaryDialogVisible(true) }
                        )
                    }

                    NavigationTab.TICKETS -> {
                        TicketScreen(
                            currentRole = currentRole,
                            tickets = tickets,
                            onCreateTicketClick = { viewModel.setCreateTicketDialogVisible(true) },
                            onApproveTicket = { viewModel.approveTicket(it) },
                            onMarkPaid = { viewModel.markTicketPaid(it) },
                            onRejectTicket = { viewModel.rejectTicket(it) }
                        )
                    }

                    NavigationTab.JOBS -> {
                        JobBoardScreen(
                            currentRole = currentRole,
                            jobs = jobListings,
                            candidates = ctvCandidates,
                            onApplyJob = { viewModel.openApplyJob(it) }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showRoleSwitcher) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            onRoleSelected = { viewModel.switchRole(it) },
            onDismiss = { viewModel.setRoleSwitcherVisible(false) },
            onLogout = { viewModel.logout() }
        )
    }

    if (showCreateTicketDialog) {
        CreateTicketModal(
            initialType = TicketType.LEAVE_REQUEST,
            onSubmit = { type, title, desc, amount, dates ->
                viewModel.submitTicket(type, title, desc, amount, dates)
            },
            onDismiss = { viewModel.setCreateTicketDialogVisible(false) }
        )
    }

    if (showAdvanceSalaryDialog) {
        CreateTicketModal(
            initialType = TicketType.ADVANCE_SALARY,
            onSubmit = { type, title, desc, amount, dates ->
                viewModel.submitTicket(type, title, desc, amount, dates)
            },
            onDismiss = { viewModel.setAdvanceSalaryDialogVisible(false) }
        )
    }

    if (showCheckInSuccessDialog) {
        CheckInSuccessModal(
            gpsInfo = lastGpsCheckIn,
            onDismiss = { viewModel.setCheckInSuccessVisible(false) }
        )
    }

    showApplyJobDialog?.let { job ->
        ApplyJobModal(
            job = job,
            onSubmit = { name, phone ->
                viewModel.submitApplication(job.title, name, phone)
            },
            onDismiss = { viewModel.openApplyJob(null) }
        )
    }
}
