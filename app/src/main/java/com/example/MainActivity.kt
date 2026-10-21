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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TicketType
import com.example.ui.components.ApplyJobModal
import com.example.ui.components.CheckInSuccessModal
import com.example.ui.components.CreateTicketModal
import com.example.ui.components.HrpBottomBar
import com.example.ui.components.HrpTopBar
import com.example.ui.components.PhoneOtpVerificationModal
import com.example.ui.components.WorkerVerificationModal
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.JobBoardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ReferralScreen
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
                },
                onRegister = { phone, password, fullName ->
                    viewModel.register(phone, password, fullName)
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

    val workerProfile by viewModel.workerProfile.collectAsState()
    val verificationInfo by viewModel.verificationInfo.collectAsState()
    val publicProjects by viewModel.publicProjects.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val tickets by viewModel.tickets.collectAsState()
    val referralProgram by viewModel.referralProgram.collectAsState()
    val referralList by viewModel.referralList.collectAsState()
    val jobListings by viewModel.jobListings.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val lastGpsCheckIn by viewModel.lastGpsCheckIn.collectAsState()

    val showCreateTicketDialog by viewModel.showCreateTicketDialog.collectAsState()
    val showAdvanceSalaryDialog by viewModel.showAdvanceSalaryDialog.collectAsState()
    val showCheckInSuccessDialog by viewModel.showCheckInSuccessDialog.collectAsState()
    val showApplyJobDialog by viewModel.showApplyJobDialog.collectAsState()
    val selectedJobDetail by viewModel.selectedJobDetail.collectAsState()
    val isLoadingJobDetail by viewModel.isLoadingJobDetail.collectAsState()

    val showPhoneOtpDialog by viewModel.showPhoneOtpDialog.collectAsState()
    val otpCooldownSeconds by viewModel.otpCooldownSeconds.collectAsState()
    val otpErrorMessage by viewModel.otpErrorMessage.collectAsState()
    val isSendingOtp by viewModel.isSendingOtp.collectAsState()
    val isVerifyingOtp by viewModel.isVerifyingOtp.collectAsState()

    val showWorkerVerificationDialog by viewModel.showWorkerVerificationDialog.collectAsState()
    val isSubmittingVerification by viewModel.isSubmittingVerification.collectAsState()
    val isSubmittingReferral by viewModel.isSubmittingReferral.collectAsState()
    val isSubmittingApplication by viewModel.isSubmittingApplication.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingGpsAction by remember { mutableStateOf("CHECK_IN") }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearToast()
        }
    }

    // GPS Hardware runtime permission launcher for Check-In & Check-Out
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        coroutineScope.launch {
            val loc = if (fineGranted || coarseGranted) {
                viewModel.locationHelper.getCurrentLocation()
            } else {
                null
            }
            if (pendingGpsAction == "CHECK_OUT") {
                viewModel.checkOut(loc)
            } else {
                viewModel.checkIn(loc)
            }
        }
    }

    val performGpsCheckIn = {
        pendingGpsAction = "CHECK_IN"
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

    val performGpsCheckOut = {
        pendingGpsAction = "CHECK_OUT"
        if (LocationHelper.hasLocationPermission(context)) {
            coroutineScope.launch {
                val loc = viewModel.locationHelper.getCurrentLocation()
                viewModel.checkOut(loc)
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

    // Handle back button: return to Home tab if on a sub-screen (when not viewing a job detail)
    BackHandler(enabled = currentTab != NavigationTab.HOME && selectedJobDetail == null) {
        viewModel.selectTab(NavigationTab.HOME)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HrpTopBar(
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            HrpBottomBar(
                currentTab = currentTab,
                pendingTicketsCount = tickets.count {
                    it.status == com.example.data.model.TicketStatus.PENDING
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
                            workerProfile = workerProfile,
                            verificationInfo = verificationInfo,
                            attendanceList = attendanceList,
                            tickets = tickets,
                            onNavigateTab = { viewModel.selectTab(it) },
                            onCheckIn = { performGpsCheckIn() },
                            onCheckOut = { performGpsCheckOut() },
                            onRequestAdvanceSalary = { viewModel.setAdvanceSalaryDialogVisible(true) },
                            onCreateTicket = { viewModel.setCreateTicketDialogVisible(true) },
                            onOpenPhoneOtp = { viewModel.setPhoneOtpDialogVisible(true) },
                            onOpenWorkerVerification = { viewModel.setWorkerVerificationDialogVisible(true) }
                        )
                    }

                    NavigationTab.JOBS -> {
                        JobBoardScreen(
                            jobs = jobListings,
                            onApplyJob = { viewModel.openApplyJob(it) },
                            selectedJobDetail = selectedJobDetail,
                            isLoadingJobDetail = isLoadingJobDetail,
                            onViewJobDetail = { viewModel.openJobDetail(it) },
                            onCloseJobDetail = { viewModel.openJobDetail(null) }
                        )
                    }

                    NavigationTab.ATTENDANCE -> {
                        AttendanceScreen(
                            attendanceList = attendanceList,
                            workerProfile = workerProfile,
                            onCheckIn = { performGpsCheckIn() },
                            onCheckOut = { performGpsCheckOut() },
                            onDisputeAttendance = { viewModel.setCreateTicketDialogVisible(true) },
                            onOpenWorkerVerification = { viewModel.setWorkerVerificationDialogVisible(true) }
                        )
                    }

                    NavigationTab.FEEDBACK -> {
                        TicketScreen(
                            tickets = tickets,
                            phoneVerified = workerProfile.phoneVerified,
                            onCreateTicketClick = { viewModel.setCreateTicketDialogVisible(true) },
                            onOpenPhoneOtp = { viewModel.setPhoneOtpDialogVisible(true) }
                        )
                    }

                    NavigationTab.REFERRAL -> {
                        ReferralScreen(
                            program = referralProgram,
                            referralList = referralList,
                            workerProfile = workerProfile,
                            isSubmitting = isSubmittingReferral,
                            onSubmitReferral = { name, phone, jobInterest, consent ->
                                viewModel.submitReferral(name, phone, jobInterest, consent)
                            },
                            onOpenWorkerVerification = { viewModel.setWorkerVerificationDialogVisible(true) }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showCreateTicketDialog) {
        CreateTicketModal(
            initialType = TicketType.LEAVE_REQUEST,
            phoneVerified = workerProfile.phoneVerified,
            onSubmit = { type, title, desc, amount, dates ->
                viewModel.submitTicket(type, title, desc, amount, dates)
            },
            onSubmitFeedback = { category, content ->
                viewModel.submitFeedback(category, content)
            },
            onOpenPhoneOtp = { viewModel.setPhoneOtpDialogVisible(true) },
            onDismiss = { viewModel.setCreateTicketDialogVisible(false) }
        )
    }

    if (showAdvanceSalaryDialog) {
        CreateTicketModal(
            initialType = TicketType.ADVANCE_SALARY,
            phoneVerified = workerProfile.phoneVerified,
            onSubmit = { type, title, desc, amount, dates ->
                viewModel.submitTicket(type, title, desc, amount, dates)
            },
            onSubmitFeedback = { category, content ->
                viewModel.submitFeedback(category, content)
            },
            onOpenPhoneOtp = { viewModel.setPhoneOtpDialogVisible(true) },
            onDismiss = { viewModel.setAdvanceSalaryDialogVisible(false) }
        )
    }

    if (showCheckInSuccessDialog) {
        CheckInSuccessModal(
            gpsInfo = lastGpsCheckIn,
            onDismiss = { viewModel.setCheckInSuccessVisible(false) }
        )
    }

    if (showPhoneOtpDialog) {
        PhoneOtpVerificationModal(
            phone = workerProfile.phone,
            cooldownSeconds = otpCooldownSeconds,
            isSendingOtp = isSendingOtp,
            isVerifyingOtp = isVerifyingOtp,
            errorMessage = otpErrorMessage,
            onSendOtp = { phone -> viewModel.sendPhoneOtp(phone) },
            onVerifyOtp = { code -> viewModel.verifyPhoneOtp(code) },
            onDismiss = { viewModel.setPhoneOtpDialogVisible(false) }
        )
    }

    if (showWorkerVerificationDialog) {
        WorkerVerificationModal(
            defaultFullName = workerProfile.fullName.takeIf { it != "Người Lao Động" } ?: "",
            defaultEmployeeCode = workerProfile.employeeCode,
            publicProjects = publicProjects,
            existingVerification = verificationInfo,
            isSubmitting = isSubmittingVerification,
            onSubmit = { fullName, cccdNumber, dob, projectId, employeeCode ->
                viewModel.submitWorkerVerification(fullName, cccdNumber, dob, projectId, employeeCode)
            },
            onDismiss = { viewModel.setWorkerVerificationDialogVisible(false) }
        )
    }

    showApplyJobDialog?.let { job ->
        ApplyJobModal(
            job = job,
            defaultName = workerProfile.fullName.takeIf { it != "Người Lao Động" } ?: "",
            defaultPhone = workerProfile.phone,
            isAuthenticated = true,
            phoneVerified = workerProfile.phoneVerified,
            isSubmitting = isSubmittingApplication,
            onSubmit = { name, phone ->
                viewModel.submitApplication(job.slug, name, phone)
            },
            onDismiss = { viewModel.openApplyJob(null) }
        )
    }
}
