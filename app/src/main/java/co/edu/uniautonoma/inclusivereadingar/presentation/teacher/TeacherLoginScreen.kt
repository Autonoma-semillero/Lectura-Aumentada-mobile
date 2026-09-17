package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModelFactory

@Composable
fun TeacherLoginRoute(
    onLoginSuccess: () -> Unit,
    onStudentLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(container.authRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TeacherLoginScreen(
        identifier = uiState.identifier,
        password = uiState.password,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onIdentifierChange = viewModel::updateIdentifier,
        onPasswordChange = viewModel::updatePassword,
        onLoginClick = { viewModel.login(onLoginSuccess) },
        onStudentLoginClick = onStudentLoginClick
    )
}

@Composable
fun TeacherLoginScreen(
    identifier: String,
    password: String,
    isLoading: Boolean,
    errorMessage: String?,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onStudentLoginClick: () -> Unit
) {
    val primary = Color(0xFFE53734)
    val background = Color(0xFFF8F6F6)
    val textDark = Color(0xFF111827)
    val textMuted = Color(0xFF64748B)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(
                onClick = onStudentLoginClick,
                modifier = Modifier
                    .align(Alignment.Start)
                    .statusBarsPadding()
                    .padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(116.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoStories,
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(74.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Lectura Inclusiva Aumentada",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textDark
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Ingreso docente",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Administra temáticas y crea nuevas tarjetas de lectura",
                fontSize = 17.sp,
                color = textMuted,
                lineHeight = 24.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            TeacherLoginField(
                value = identifier,
                onValueChange = onIdentifierChange,
                label = "Usuario o correo electrónico",
                placeholder = "profe.ana o teacher@lectura.app",
                leadingIcon = Icons.Rounded.Person,
                isPassword = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            TeacherLoginField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Contraseña",
                placeholder = "Ingresa tu contraseña",
                leadingIcon = Icons.Rounded.Lock,
                isPassword = true
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onLoginClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Entrar",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Credenciales demo docente",
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = "Correo: teacher@lectura.app\nContraseña: Lectura123!",
                        color = textMuted,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(24.dp)
                    .navigationBarsPadding()
            )
        }
    }
}

@Composable
private fun TeacherLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    isPassword: Boolean
) {
    val primary = Color(0xFFE53734)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(text = label) },
        placeholder = { Text(text = placeholder) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = primary.copy(alpha = 0.75f)
            )
        },
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            androidx.compose.ui.text.input.VisualTransformation.None
        },
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primary,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
