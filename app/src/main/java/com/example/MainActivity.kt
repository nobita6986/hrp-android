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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpTextSecondary
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
    val showLoginScreen by viewModel.showLoginScreen.collectAsState()
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val loginErrorMessage by viewModel.loginErrorMessage.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Always open Tổng quan (HOME) with the Check-In button whenever the user enters the app while logged in
    DisposableEffect(lifecycleOwner, isLoggedIn) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && isLoggedIn) {
                viewModel.onAppEntered()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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

    val shouldShowLoginOverlay = !isLoggedIn && showLoginScreen

    AnimatedContent(
        targetState = shouldShowLoginOverlay,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AuthHostTransition"
    ) { showingLogin ->
        if (showingLogin) {
            LoginScreen(
                isAuthenticating = isAuthenticating,
                errorMessage = loginErrorMessage,
                onLogin = { phone, password ->
                    viewModel.login(phone, password)
                },
                onRegister = { phone, password, fullName ->
                    viewModel.register(phone, password, fullName)
                },
                onBrowseJobsWithoutLogin = {
                    viewModel.setLoginScreenVisible(false)
                    viewModel.selectTab(NavigationTab.JOBS)
                }
            )
        } else {
            HrpApp(viewModel = viewModel)
        }
    }
}

@Composable
fun HrpApp(
    viewModel: HrpViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val workerProfile by viewModel.workerProfile.collectAsState()
    val verificationInfo by viewModel.verificationInfo.collectAsState()
    val publicProjects by viewModel.publicProjects.collectAsState()
    val attendanceList by viewModel.attendanceList.collectAsState()
    val lastCheckInEpochMs by viewModel.lastCheckInEpochMs.collectAsState()
    val lastCheckOutEpochMs by viewModel.lastCheckOutEpochMs.collectAsState()
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

    // Handle back button: return to default tab (HOME when logged in, JOBS when guest) if not viewing a job detail
    val defaultRootTab = if (isLoggedIn) NavigationTab.HOME else NavigationTab.JOBS
    BackHandler(enabled = currentTab != defaultRootTab && selectedJobDetail == null) {
        viewModel.selectTab(defaultRootTab)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HrpTopBar(
                isLoggedIn = isLoggedIn,
                onLoginClick = { viewModel.setLoginScreenVisible(true) },
                onLogout = { viewModel.logout() }
            )
        },
        bottomBar = {
            HrpBottomBar(
                currentTab = currentTab,
                pendingTicketsCount = if (isLoggedIn) {
                    tickets.count { it.status == com.example.data.model.TicketStatus.PENDING }
                } else {
                    0
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

                    NavigationTab.HOME -> {
                        if (isLoggedIn) {
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
                                onOpenWorkerVerification = { viewModel.setWorkerVerificationDialogVisible(true) },
                                lastCheckInEpochMs = lastCheckInEpochMs,
                                lastCheckOutEpochMs = lastCheckOutEpochMs
                            )
                        } else {
                            AuthRequiredTabGate(
                                tab = NavigationTab.HOME,
                                onOpenLogin = { viewModel.setLoginScreenVisible(true) },
                                onBrowseJobs = { viewModel.selectTab(NavigationTab.JOBS) }
                            )
                        }
                    }

                    NavigationTab.ATTENDANCE -> {
                        if (isLoggedIn) {
                            AttendanceScreen(
                                attendanceList = attendanceList,
                                workerProfile = workerProfile,
                                onCheckIn = { performGpsCheckIn() },
                                onCheckOut = { performGpsCheckOut() },
                                onDisputeAttendance = { viewModel.setCreateTicketDialogVisible(true) },
                                onOpenWorkerVerification = { viewModel.setWorkerVerificationDialogVisible(true) },
                                lastCheckInEpochMs = lastCheckInEpochMs,
                                lastCheckOutEpochMs = lastCheckOutEpochMs
                            )
                        } else {
                            AuthRequiredTabGate(
                                tab = NavigationTab.ATTENDANCE,
                                onOpenLogin = { viewModel.setLoginScreenVisible(true) },
                                onBrowseJobs = { viewModel.selectTab(NavigationTab.JOBS) }
                            )
                        }
                    }

                    NavigationTab.FEEDBACK -> {
                        if (isLoggedIn) {
                            TicketScreen(
                                tickets = tickets,
                                phoneVerified = workerProfile.phoneVerified,
                                onCreateTicketClick = { viewModel.setCreateTicketDialogVisible(true) },
                                onOpenPhoneOtp = { viewModel.setPhoneOtpDialogVisible(true) }
                            )
                        } else {
                            AuthRequiredTabGate(
                                tab = NavigationTab.FEEDBACK,
                                onOpenLogin = { viewModel.setLoginScreenVisible(true) },
                                onBrowseJobs = { viewModel.selectTab(NavigationTab.JOBS) }
                            )
                        }
                    }

                    NavigationTab.REFERRAL -> {
                        if (isLoggedIn) {
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
                        } else {
                            AuthRequiredTabGate(
                                tab = NavigationTab.REFERRAL,
                                onOpenLogin = { viewModel.setLoginScreenVisible(true) },
                                onBrowseJobs = { viewModel.selectTab(NavigationTab.JOBS) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs (Authenticated features require isLoggedIn == true; ApplyJobModal works in both guest and authenticated modes)
    if (isLoggedIn && showCreateTicketDialog) {
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

    if (isLoggedIn && showAdvanceSalaryDialog) {
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

    if (isLoggedIn && showCheckInSuccessDialog) {
        CheckInSuccessModal(
            gpsInfo = lastGpsCheckIn,
            onDismiss = { viewModel.setCheckInSuccessVisible(false) }
        )
    }

    if (isLoggedIn && showPhoneOtpDialog) {
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

    if (isLoggedIn && showWorkerVerificationDialog) {
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
            defaultName = if (isLoggedIn) (workerProfile.fullName.takeIf { it != "Người Lao Động" } ?: "") else "",
            defaultPhone = if (isLoggedIn) workerProfile.phone else "",
            isAuthenticated = isLoggedIn,
            phoneVerified = isLoggedIn && workerProfile.phoneVerified,
            isSubmitting = isSubmittingApplication,
            onSubmit = { name, phone ->
                viewModel.submitApplication(job.slug, name, phone)
            },
            onDismiss = { viewModel.openApplyJob(null) }
        )
    }
}

@Composable
fun AuthRequiredTabGate(
    tab: NavigationTab,
    onOpenLogin: () -> Unit,
    onBrowseJobs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featureDescription = when (tab) {
        NavigationTab.HOME ->
            "Đăng nhập tài khoản Người lao động HRP để thực hiện chấm công GPS, xem hồ sơ cá nhân và trạng thái phân công dự án."
        NavigationTab.ATTENDANCE ->
            "Đăng nhập tài khoản để xem chi tiết bảng công, số giờ làm việc thực tế và lịch sử chấm công GPS."
        NavigationTab.FEEDBACK ->
            "Đăng nhập và xác thực số điện thoại để gửi góp ý, phản ánh hoặc yêu cầu hỗ trợ tới bộ phận nhân sự HR."
        NavigationTab.REFERRAL ->
            "Đăng nhập tài khoản để tham gia chương trình giới thiệu bạn bè đi làm và theo dõi tiền thưởng giới thiệu."
        NavigationTab.JOBS ->
            "Bạn có thể xem danh sách việc làm và nộp hồ sơ ứng tuyển ngay mà không cần đăng nhập."
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
            .padding(horizontal = 20.dp, vertical = 24.dp)
            .testTag("auth_required_gate_${tab.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = HrpSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, HrpSurfaceBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(HrpPrimarySoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = HrpPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Tính Năng Yêu Cầu Đăng Nhập",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = HrpNavyDark,
                        fontSize = 17.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Mục \"${tab.label}\" chỉ khả dụng sau khi đăng nhập",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = HrpPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = featureDescription,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = HrpTextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onOpenLogin,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("gate_open_login_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đăng Nhập / Đăng Ký Tài Khoản",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onBrowseJobs,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, HrpPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("gate_back_to_jobs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = HrpPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Xem Danh Sách Việc Làm & Ứng Tuyển",
                        fontWeight = FontWeight.Bold,
                        color = HrpPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
