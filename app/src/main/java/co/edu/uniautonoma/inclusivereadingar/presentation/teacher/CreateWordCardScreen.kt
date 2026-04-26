package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.uniautonoma.inclusivereadingar.appContainer
import co.edu.uniautonoma.inclusivereadingar.config.BackendConfig
import co.edu.uniautonoma.inclusivereadingar.domain.model.AppUser
import co.edu.uniautonoma.inclusivereadingar.domain.model.Category
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.CreateWordCardViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.LocalAudioSelection
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherAudioSource
import java.io.File
import java.io.FileOutputStream

private const val MAX_AUDIO_BYTES = 10L * 1024L * 1024L
private val ALLOWED_AUDIO_MIME_TYPES = setOf(
    "audio/mp4",
    "audio/x-m4a",
    "audio/mpeg",
    "audio/mp3",
    "audio/wav",
    "audio/x-wav",
    "audio/wave"
)
private val ALLOWED_AUDIO_EXTENSIONS = setOf("m4a", "mp3", "wav")

@Composable
fun CreateWordCardRoute(
    preSelectedCategoryId: String? = null,
    onBack: () -> Unit,
    onThemesClick: () -> Unit = onBack,
    onStudentsClick: () -> Unit = onBack,
    onWordCardsClick: () -> Unit = onBack
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: CreateWordCardViewModel = viewModel(
        factory = CreateWordCardViewModelFactory(container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var backendBaseUrl by remember { mutableStateOf(BackendConfig.DEFAULT_REMOTE_BASE_URL) }
    LaunchedEffect(Unit) {
        backendBaseUrl = container.sessionStore.resolveBackendBaseUrl()
    }

    LaunchedEffect(preSelectedCategoryId) {
        if (!preSelectedCategoryId.isNullOrBlank()) {
            viewModel.selectCategory(preSelectedCategoryId)
        }
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onBack()
        }
    }

    CreateWordCardScreen(
        uiState = uiState,
        backendBaseUrl = backendBaseUrl,
        onBack = onBack,
        onSelectStudent = viewModel::selectStudent,
        onWordChange = viewModel::updateWord,
        onSelectCategory = viewModel::selectCategory,
        onAudioUrlChange = viewModel::updateAudioUrl,
        onAudioFileSelected = viewModel::selectAudioFile,
        onRecordedAudioSelected = viewModel::selectRecordedAudio,
        onError = viewModel::setErrorMessage,
        onSaveClick = viewModel::save,
        onThemesClick = onThemesClick,
        onStudentsClick = onStudentsClick,
        onWordCardsClick = onWordCardsClick
    )
}

@Composable
fun CreateWordCardScreen(
    uiState: CreateWordCardUiState,
    backendBaseUrl: String,
    onBack: () -> Unit,
    onSelectStudent: (String) -> Unit,
    onWordChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onAudioUrlChange: (String) -> Unit,
    onAudioFileSelected: (LocalAudioSelection) -> Unit,
    onRecordedAudioSelected: (LocalAudioSelection) -> Unit,
    onError: (String?) -> Unit,
    onSaveClick: () -> Unit,
    onThemesClick: () -> Unit = onBack,
    onStudentsClick: () -> Unit = onBack,
    onWordCardsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val primary = Color(0xFFE53734)

    var studentMenuExpanded by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var pendingRecordedAudio by remember { mutableStateOf<LocalAudioSelection?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onError("Permiso de microfono denegado.")
            return@rememberLauncherForActivityResult
        }
        startRecording(
            context = context,
            onStarted = { recorder, audioSelection ->
                activeRecorder = recorder
                pendingRecordedAudio = audioSelection
                isRecording = true
                onError(null)
            },
            onFailure = { message -> onError(message) }
        )
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) {
            return@rememberLauncherForActivityResult
        }
        when (val copied = copyAudioToCache(context, uri)) {
            is AudioCopyResult.Success -> {
                onAudioFileSelected(copied.selection)
                onError(null)
            }
            is AudioCopyResult.Error -> onError(copied.message)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.release() }
            stopRecorder(activeRecorder)
            pendingRecordedAudio?.let {
                runCatching { File(it.filePath).delete() }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = primary
                )
            }
            Text(
                text = "Registrar Nueva Tarjeta",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Box(modifier = Modifier.size(48.dp))
        }

        if (uiState.isLoadingStudents || uiState.isLoadingCategories) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primary)
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Selecciona el Estudiante",
                    fontWeight = FontWeight.Bold
                )
                StudentDropdown(
                    students = uiState.students,
                    selectedStudentId = uiState.selectedStudentId,
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = it },
                    onSelectStudent = {
                        onSelectStudent(it)
                        studentMenuExpanded = false
                    }
                )

                Text(
                    text = "Detalles de la Palabra",
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = uiState.word,
                    onValueChange = onWordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primary
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Text(
                    text = "Selecciona el Tema",
                    fontWeight = FontWeight.Bold
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(uiState.categories, key = { it.id }) { category ->
                        val selected = uiState.selectedCategoryId == category.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clickable { onSelectCategory(category.id) },
                            shape = RoundedCornerShape(22.dp),
                            color = if (selected) primary.copy(alpha = 0.12f) else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (selected) primary else Color(0xFFE2E8F0)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = categoryIcon(category.icon),
                                    contentDescription = null,
                                    tint = if (selected) primary else Color(0xFF64748B)
                                )
                                Text(
                                    text = category.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF111827)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.audioUrlInput,
                    onValueChange = onAudioUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de pronunciacion (opcional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRecording) {
                                stopRecorder(activeRecorder)
                                activeRecorder = null
                                isRecording = false
                                pendingRecordedAudio?.let {
                                    onRecordedAudioSelected(it)
                                }
                                pendingRecordedAudio = null
                                onError(null)
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    startRecording(
                                        context = context,
                                        onStarted = { recorder, audioSelection ->
                                            activeRecorder = recorder
                                            pendingRecordedAudio = audioSelection
                                            isRecording = true
                                            onError(null)
                                        },
                                        onFailure = { message -> onError(message) }
                                    )
                                } else {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFB91C1C) else primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                            contentDescription = null
                        )
                        Text(
                            text = if (isRecording) "Detener" else "Grabar voz",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Button(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        enabled = !isRecording,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = null
                        )
                        Text(
                            text = "Subir archivo",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                if (isRecording) {
                    Text(
                        text = "Cancelar grabación",
                        color = Color(0xFFB91C1C),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                stopRecorder(activeRecorder)
                                activeRecorder = null
                                isRecording = false
                                pendingRecordedAudio?.let {
                                    runCatching { File(it.filePath).delete() }
                                }
                                pendingRecordedAudio = null
                                onError("Grabación cancelada.")
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sourceLabel = when (uiState.audioSource) {
                        is TeacherAudioSource.Url -> "Fuente activa: URL"
                        is TeacherAudioSource.File -> "Fuente activa: archivo"
                        is TeacherAudioSource.Recorded -> "Fuente activa: grabacion"
                        null -> "Sin audio seleccionado"
                    }
                    Text(
                        text = sourceLabel,
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Button(
                        onClick = {
                            val playbackTarget = resolveAudioPlaybackTarget(
                                source = uiState.audioSource,
                                backendBaseUrl = backendBaseUrl
                            ) ?: return@Button
                            runCatching {
                                player.reset()
                                player.setDataSource(context, Uri.parse(playbackTarget))
                                player.setOnPreparedListener {
                                    isPlayingAudio = true
                                    it.start()
                                }
                                player.setOnCompletionListener {
                                    isPlayingAudio = false
                                }
                                player.prepareAsync()
                            }.onFailure {
                                isPlayingAudio = false
                                onError("No fue posible reproducir el audio seleccionado.")
                            }
                        },
                        enabled = uiState.audioSource != null,
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEAF2FF),
                            contentColor = Color(0xFF004883)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(text = if (isPlayingAudio) "Reproduciendo" else "Previsualizar")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Estado",
                            fontWeight = FontWeight.Bold
                        )
                        StatusRow(
                            label = "Palabra escrita",
                            done = uiState.word.isNotBlank()
                        )
                        StatusRow(
                            label = "Categoria elegida",
                            done = uiState.selectedCategoryId != null
                        )
                        StatusRow(
                            label = "Estudiante seleccionado",
                            done = uiState.selectedStudentId != null
                        )
                        StatusRow(
                            label = "Audio listo",
                            done = uiState.hasAudio
                        )
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isRecording) {
                            onError("Detén o cancela la grabación antes de guardar.")
                        } else {
                            onSaveClick()
                        }
                    },
                    enabled = uiState.canSave && !uiState.isSaving && !isRecording,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text(
                            text = "Guardar Tarjeta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }

                Text(
                    text = "Cancelar",
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable(onClick = onBack)
                        .padding(vertical = 8.dp)
                )

                androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
            }
        }

        TeacherBottomBar(
            activeTab = TeacherTab.WORD_CARDS,
            onThemesClick = onThemesClick,
            onStudentsClick = onStudentsClick,
            onWordCardsClick = onWordCardsClick
        )
    }
}

@Composable
private fun StudentDropdown(
    students: List<AppUser>,
    selectedStudentId: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectStudent: (String) -> Unit
) {
    val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onExpandedChange(true) },
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text(
                text = selectedStudent?.displayName ?: selectedStudent?.email ?: "Selecciona un estudiante",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                color = if (selectedStudent == null) Color(0xFF64748B) else Color(0xFF111827)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            students.forEach { student ->
                DropdownMenuItem(
                    text = {
                        Text(student.displayName ?: student.email)
                    },
                    onClick = { onSelectStudent(student.id) }
                )
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (done) {
                Icons.Rounded.CheckCircle
            } else {
                Icons.Rounded.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (done) Color(0xFF16A34A) else Color(0xFF94A3B8)
        )
        Text(
            text = label,
            color = if (done) Color(0xFF166534) else Color(0xFF475569),
            fontWeight = if (done) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private sealed interface AudioCopyResult {
    data class Success(val selection: LocalAudioSelection) : AudioCopyResult
    data class Error(val message: String) : AudioCopyResult
}

private fun copyAudioToCache(context: Context, uri: Uri): AudioCopyResult {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)?.lowercase()
    val originalName = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
    }

    val extension = when {
        originalName != null && originalName.contains('.') -> {
            originalName.substringAfterLast('.').lowercase()
        }
        mimeType == null -> null
        mimeType.contains("mp3") -> "mp3"
        mimeType.contains("wav") -> "wav"
        mimeType.contains("mp4") || mimeType.contains("m4a") -> "m4a"
        else -> null
    }

    val isMimeSupported = mimeType?.let { it in ALLOWED_AUDIO_MIME_TYPES } ?: false
    val isExtensionSupported = extension?.let { it in ALLOWED_AUDIO_EXTENSIONS } ?: false
    if (!isMimeSupported && !isExtensionSupported) {
        return AudioCopyResult.Error("Formato no permitido. Usa m4a, mp3 o wav.")
    }

    val suffix = ".${extension ?: "m4a"}"
    val tempFile = File.createTempFile("teacher-audio-", suffix, context.cacheDir)

    return runCatching {
        resolver.openInputStream(uri).use { input ->
            if (input == null) {
                throw IllegalStateException("No se pudo leer el archivo seleccionado.")
            }
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        }
        if (tempFile.length() > MAX_AUDIO_BYTES) {
            tempFile.delete()
            throw IllegalStateException("El audio supera el límite de 10MB.")
        }
        LocalAudioSelection(
            filePath = tempFile.absolutePath,
            mimeType = mimeType,
            originalName = originalName ?: tempFile.name
        )
    }.fold(
        onSuccess = { AudioCopyResult.Success(it) },
        onFailure = {
            tempFile.delete()
            AudioCopyResult.Error(it.message ?: "No fue posible preparar el audio.")
        }
    )
}

private fun startRecording(
    context: Context,
    onStarted: (MediaRecorder, LocalAudioSelection) -> Unit,
    onFailure: (String) -> Unit
) {
    val outputFile = File(context.cacheDir, "recorded-${System.currentTimeMillis()}.m4a")

    runCatching {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioSamplingRate(44_100)
        recorder.setAudioEncodingBitRate(96_000)
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.prepare()
        recorder.start()

        onStarted(
            recorder,
            LocalAudioSelection(
                filePath = outputFile.absolutePath,
                mimeType = "audio/mp4",
                originalName = outputFile.name
            )
        )
    }.onFailure {
        onFailure("No fue posible iniciar la grabación.")
    }
}

private fun stopRecorder(recorder: MediaRecorder?) {
    if (recorder == null) {
        return
    }
    runCatching { recorder.stop() }
    runCatching { recorder.reset() }
    runCatching { recorder.release() }
}

private fun resolveAudioPlaybackTarget(
    source: TeacherAudioSource?,
    backendBaseUrl: String
): String? {
    return when (source) {
        null -> null
        is TeacherAudioSource.Url -> resolveAudioUrl(source.value, backendBaseUrl)
        is TeacherAudioSource.File -> Uri.fromFile(File(source.localAudio.filePath)).toString()
        is TeacherAudioSource.Recorded -> Uri.fromFile(File(source.localAudio.filePath)).toString()
    }
}

private fun resolveAudioUrl(rawValue: String, backendBaseUrl: String): String {
    val value = rawValue.trim()
    return when {
        value.startsWith("http://") || value.startsWith("https://") -> value
        value.startsWith("/") -> "${backendBaseUrl.removeSuffix("/")}$value"
        else -> "${backendBaseUrl.removeSuffix("/")}/$value"
    }
}
