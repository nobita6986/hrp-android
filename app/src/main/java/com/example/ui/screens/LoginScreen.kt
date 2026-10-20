package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.HrpBackground
import com.example.ui.theme.HrpError
import com.example.ui.theme.HrpNavyDark
import com.example.ui.theme.HrpPrimary
import com.example.ui.theme.HrpPrimarySoft
import com.example.ui.theme.HrpSurface
import com.example.ui.theme.HrpSurfaceBorder
import com.example.ui.theme.HrpTextSecondary

@Composable
fun LoginScreen(
    isAuthenticating: Boolean,
    errorMessage: String?,
    onLogin: (phone: String, pass: String) -> Unit,
    onRegister: ((phone: String, pass: String, fullName: String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HrpBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Branding Header
            Surface(
                modifier = Modifier.size(width = 110.dp, height = 88.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.hrp_logo),
                        contentDescription = "HRP Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HR PARTNER",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = HrpNavyDark,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "Ứng Dụng Dành Cho Người Lao Động & Tìm Việc",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = HrpTextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login / Register Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = HrpSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, HrpSurfaceBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Đăng Ký Tài Khoản Mới" else "Đăng Nhập Tài Khoản",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = HrpNavyDark,
                            fontSize = 17.sp
                        )
                    )
                    Text(
                        text = if (isRegisterMode) {
                            "Tạo tài khoản Người lao động để ứng tuyển và xác thực chấm công"
                        } else {
                            "Sử dụng số điện thoại cá nhân đã đăng ký"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = HrpTextSecondary,
                            fontSize = 12.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Họ và tên đầy đủ") },
                                placeholder = { Text("Nguyễn Văn A") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = HrpPrimary
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HrpPrimary,
                                    unfocusedBorderColor = HrpSurfaceBorder,
                                    focusedLabelColor = HrpPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("register_fullname_input")
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }

                    // Phone input (strictly 10 digits starting with 0 in register mode; rejects +84, spaces, separators)
                    val isRegisterPhoneInvalid = isRegisterMode &&
                        phone.isNotEmpty() &&
                        !com.example.data.repository.HrpFormatters.isValidStrictVnPhone(phone)

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isRegisterMode) "Số điện thoại (10 chữ số, bắt đầu bằng 0)" else "Số điện thoại") },
                        placeholder = { Text("0912345678") },
                        isError = isRegisterPhoneInvalid,
                        supportingText = if (isRegisterPhoneInvalid) {
                            {
                                Text(
                                    text = "SĐT phải đúng 10 chữ số, bắt đầu bằng 0 (không dùng +84, khoảng trắng hoặc dấu phân cách)",
                                    color = HrpError,
                                    fontSize = 11.sp
                                )
                            }
                        } else null,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = HrpPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HrpPrimary,
                            unfocusedBorderColor = HrpSurfaceBorder,
                            focusedLabelColor = HrpPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (isRegisterMode) "Mật khẩu (tối thiểu 8 ký tự)" else "Mật khẩu") },
                        placeholder = { Text("••••••••") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = HrpPrimary
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Ẩn mật khẩu" else "Hiện mật khẩu",
                                    tint = HrpTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                if (!isAuthenticating) {
                                    if (isRegisterMode && onRegister != null) {
                                        onRegister(phone, password, fullName)
                                    } else {
                                        onLogin(phone, password)
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HrpPrimary,
                            unfocusedBorderColor = HrpSurfaceBorder,
                            focusedLabelColor = HrpPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    AnimatedVisibility(visible = isRegisterMode) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HrpPrimarySoft,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Tài khoản mới có thể xem việc làm và ứng tuyển ngay. Số điện thoại khởi tạo ở trạng thái chưa xác minh OTP (phoneVerified=false); tính năng Góp ý cần xác minh OTP và Chấm công/Giới thiệu cần hồ sơ Worker được phân công ACTIVE.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = HrpNavyDark,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }

                    // Error Alert Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HrpError.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, HrpError.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = HrpError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = error,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = HrpError,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Submit Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            if (isRegisterMode && onRegister != null) {
                                onRegister(phone, password, fullName)
                            } else {
                                onLogin(phone, password)
                            }
                        },
                        enabled = !isAuthenticating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HrpPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_button")
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Đang xử lý...", fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Text(
                                if (isRegisterMode) "Đăng Ký Tài Khoản" else "Đăng Nhập",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            )
                        }
                    }

                    if (onRegister != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isRegisterMode) "Đã có tài khoản?" else "Chưa có tài khoản?",
                                style = MaterialTheme.typography.bodySmall.copy(color = HrpTextSecondary)
                            )
                            TextButton(
                                onClick = { isRegisterMode = !isRegisterMode },
                                modifier = Modifier.testTag("toggle_register_mode_button")
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Đăng nhập ngay" else "Đăng ký mới",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = HrpPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tổng đài hỗ trợ người lao động HRP: 0211.2216.999",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = HrpTextSecondary.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
