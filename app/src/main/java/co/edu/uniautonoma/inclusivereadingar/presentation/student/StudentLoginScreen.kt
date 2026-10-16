package co.edu.uniautonoma.inclusivereadingar.presentation.student

import android.content.Context
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.presentation.auth.LaunchBrandLogo
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.student.viewmodel.AuthViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.theme.PrimaryRedDark
import kotlinx.coroutines.delay

@Composable
fun StudentLoginRoute(
    onLoginSuccess: () -> Unit,
    onTeacherLoginClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(container.authRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StudentLoginScreen(
        pin = uiState.pin,
        pinError = uiState.pinError,
        isLoading = uiState.isLoading,
        errorMessage = uiState.errorMessage,
        onPinChange = viewModel::updatePin,
        onLoginClick = { viewModel.loginWithStudentPin(onLoginSuccess) },
        onTeacherLoginClick = onTeacherLoginClick
    )
}

@Composable
fun StudentLoginScreen(
    pin: String,
    pinError: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onPinChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onTeacherLoginClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var showHelp by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        StudentLoginBackgroundPattern()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 188.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextButton(
                onClick = { showHelp = true },
                modifier = Modifier
                    .align(Alignment.End)
                    .heightIn(min = 48.dp)
                    .testTag("student_login_help"),
                colors = ButtonDefaults.textButtonColors(contentColor = colors.secondary)
            ) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null, modifier = Modifier.size(22.dp))
                Text("Ayuda", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))
            LaunchBrandLogo(modifier = Modifier.testTag("student_login_logo"))
            Spacer(modifier = Modifier.height(28.dp))
            StudentPinField(
                value = pin,
                onValueChange = onPinChange,
                errorMessage = pinError,
                onDone = onLoginClick,
                keepKeyboardVisible = !showHelp
            )
            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                LoginErrorMessage(errorMessage)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding(),
            color = colors.background,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StudentPrimaryButton(isLoading = isLoading, onClick = onLoginClick)
                TextButton(
                    onClick = onTeacherLoginClick,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("student_login_teacher_link"),
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant)
                ) {
                    Text("Acceso para docentes", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("¿Necesitas ayuda?") },
            text = { Text("Pide a tu profe o a un adulto los 4 a 6 números de tu PIN.") },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Entendido") } }
        )
    }
}

@Composable
private fun StudentLoginBackgroundPattern() {
    val tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    Box(modifier = Modifier.fillMaxSize()) {
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 190.dp)
                .size(34.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 290.dp)
                .size(38.dp)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.MenuBook,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 244.dp)
                .size(32.dp)
        )
        Icon(
            imageVector = Icons.Rounded.Star,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 286.dp)
                .size(30.dp)
        )
    }
}

@Composable
private fun StudentPinField(
    value: String,
    onValueChange: (String) -> Unit,
    errorMessage: String?,
    onDone: () -> Unit,
    keepKeyboardVisible: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val inputMethodManager = remember(context) {
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    LaunchedEffect(keepKeyboardVisible) {
        if (keepKeyboardVisible) {
            delay(450)
            focusRequester.requestFocus()
            delay(150)
            keyboardController?.show()
            inputMethodManager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
        } else {
            keyboardController?.hide()
        }
    }

    DisposableEffect(view) {
        onDispose { inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0) }
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .focusRequester(focusRequester)
            .fillMaxWidth()
            .height(96.dp)
            .testTag("student_pin_field"),
        singleLine = true,
        label = { Text("Tu PIN", fontSize = 18.sp) },
        placeholder = { Text("4 a 6 números", fontSize = 18.sp) },
        isError = errorMessage != null,
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = colors.secondary,
                modifier = Modifier.size(30.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { onDone() }),
        visualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(18.dp),
        textStyle = TextStyle(fontSize = 32.sp, letterSpacing = 8.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.secondary,
            unfocusedBorderColor = colors.outline,
            focusedLabelColor = colors.secondary,
            unfocusedLabelColor = colors.onSurfaceVariant,
            focusedContainerColor = colors.surface,
            unfocusedContainerColor = colors.surface,
            cursorColor = colors.secondary,
            errorCursorColor = colors.error
        ),
        supportingText = errorMessage?.let { { Text(it, fontSize = 16.sp) } }
    )
}

@Composable
private fun StudentPrimaryButton(isLoading: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .padding(bottom = 8.dp)
            .testTag("student_login_submit")
    ) {
        Surface(
            modifier = Modifier.matchParentSize().offset(y = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = PrimaryRedDark
        ) {}
        Button(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = colors.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Text(" Abriendo tu aventura…", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            } else {
                Text("¡Vamos a leer!", fontWeight = FontWeight.ExtraBold, fontSize = 21.sp)
            }
        }
    }
}

@Composable
internal fun LoginErrorMessage(message: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        shape = RoundedCornerShape(16.dp),
        color = colors.errorContainer
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = colors.onErrorContainer,
                modifier = Modifier.align(Alignment.TopStart).size(22.dp)
            )
            Text(
                text = message,
                color = colors.onErrorContainer,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(start = 32.dp)
            )
        }
    }
}
