package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.auth.LaunchBrandLogo
import co.edu.uniautonoma.inclusivereadingar.presentation.student.LoginErrorMessage
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.PrimaryRedDark

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
        identifierError = uiState.identifierError,
        passwordError = uiState.passwordError,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onIdentifierChange = viewModel::updateIdentifier,
        onPasswordChange = viewModel::updatePassword,
        onLoginClick = { viewModel.loginWithPassword(onLoginSuccess) },
        onStudentLoginClick = onStudentLoginClick
    )
}

@Composable
fun TeacherLoginScreen(
    identifier: String,
    password: String,
    identifierError: String?,
    passwordError: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onStudentLoginClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = onStudentLoginClick,
            modifier = Modifier
                .align(Alignment.Start)
                .heightIn(min = 48.dp)
                .testTag("teacher_back_to_student"),
            colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Volver al PIN", modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))
        LaunchBrandLogo(modifier = Modifier.testTag("teacher_login_logo"), size = 96.dp)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Acceso docente",
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gestiona el aprendizaje de tus estudiantes.",
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = colors.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        TeacherLoginField(
            value = identifier,
            onValueChange = onIdentifierChange,
            label = "Usuario o correo",
            leadingIcon = Icons.Rounded.Person,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
            errorMessage = identifierError,
            modifier = Modifier.testTag("teacher_identifier_field")
        )
        Spacer(modifier = Modifier.height(16.dp))
        TeacherPasswordField(
            value = password,
            onValueChange = onPasswordChange,
            errorMessage = passwordError,
            onDone = onLoginClick
        )
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            LoginErrorMessage(errorMessage)
        }

        Spacer(modifier = Modifier.height(28.dp))
        TeacherPrimaryButton(isLoading = isLoading, onClick = onLoginClick)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TeacherLoginField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        label = { Text(label) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = colors.secondary) },
        isError = errorMessage != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        shape = RoundedCornerShape(16.dp),
        colors = teacherFieldColors(),
        supportingText = errorMessage?.let { { Text(it) } }
    )
}

@Composable
private fun TeacherPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    onDone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("teacher_password_field"),
        label = { Text("Contraseña") },
        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = colors.secondary) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }, modifier = Modifier.testTag("teacher_password_visibility")) {
                Icon(
                    imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        isError = errorMessage != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        shape = RoundedCornerShape(16.dp),
        colors = teacherFieldColors(),
        supportingText = errorMessage?.let { { Text(it) } }
    )
}

@Composable
private fun teacherFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@Composable
private fun TeacherPrimaryButton(isLoading: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(bottom = 7.dp)
            .testTag("teacher_login_submit")
    ) {
        Surface(
            modifier = Modifier.matchParentSize().offset(y = 7.dp),
            shape = RoundedCornerShape(20.dp),
            color = PrimaryRedDark
        ) {}
        Button(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                Text(" Entrando…", fontWeight = FontWeight.Bold)
            } else {
                Text("Entrar", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}
