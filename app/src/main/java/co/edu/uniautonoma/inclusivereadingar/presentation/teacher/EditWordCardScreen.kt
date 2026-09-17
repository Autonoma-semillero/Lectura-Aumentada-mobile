package co.edu.uniautonoma.inclusivereadingar.presentation.teacher

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.EditWordCardUiState
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.EditWordCardViewModel
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.EditWordCardViewModelFactory
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.LocalAudioSelection
import co.edu.uniautonoma.inclusivereadingar.presentation.teacher.viewmodel.TeacherAudioSource
import java.io.File

@Composable
fun EditWordCardRoute(
    cardId: String,
    onBack: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: EditWordCardViewModel = viewModel(
        key = cardId,
        factory = EditWordCardViewModelFactory(cardId, container.teacherContentRepository)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var backendBaseUrl by remember { mutableStateOf(BackendConfig.DEFAULT_REMOTE_BASE_URL) }
    LaunchedEffect(Unit) {
        backendBaseUrl = container.sessionStore.resolveBackendBaseUrl()
    }

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    EditWordCardScreen(
        uiState = uiState,
        backendBaseUrl = backendBaseUrl,
        onBack = onBack,
        onWordChange = viewModel::updateWord,
        onSelectArModel = viewModel::selectArModel,
        onSelectMarker = viewModel::selectMarker,
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
fun EditWordCardScreen(
    uiState: EditWordCardUiState,
    backendBaseUrl: String,
    onBack: () -> Unit,
    onWordChange: (String) -> Unit,
    onSelectArModel: (String) -> Unit,
    onSelectMarker: (String) -> Unit,
    onAudioUrlChange: (String) -> Unit,
    onAudioFileSelected: (LocalAudioSelection) -> Unit,
    onRecordedAudioSelected: (LocalAudioSelection) -> Unit,
    onError: (String?) -> Unit,
    onSaveClick: () -> Unit,
    onThemesClick: () -> Unit,
    onStudentsClick: () -> Unit,
    onWordCardsClick: () -> Unit
) {
    val context = LocalContext.current
    val primary = Color(0xFFE53734)

    var isRecording by remember { mutableStateOf(false) }
    var activeRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var pendingRecordedAudio by remember { mutableStateOf<LocalAudioSelection?>(null) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    val player = remember { MediaPlayer() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onError("Permiso de micrófono denegado.")
            return@rememberLauncherForActivityResult
        }
        startEditRecording(context, onStarted = { recorder, selection ->
            activeRecorder = recorder
            pendingRecordedAudio = selection
            isRecording = true
            onError(null)
        }, onFailure = { onError(it) })
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val result = copyEditAudioToCache(context, uri)) {
            is EditAudioCopyResult.Success -> {
                onAudioFileSelected(result.selection)
                onError(null)
            }
            is EditAudioCopyResult.Error -> onError(result.message)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.release() }
            stopEditRecorder(activeRecorder)
            pendingRecordedAudio?.let { runCatching { File(it.filePath).delete() } }
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
                text = "Editar Tarjeta",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp
            )
            Box(modifier = Modifier.size(48.dp))
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
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
                Text(text = "Palabra", fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = uiState.word,
                    onValueChange = onWordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Palabra (máx 15 caracteres)") },
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

                ArAssociationSection(
                    models = uiState.arModels,
                    selectedModelId = uiState.selectedArModelId,
                    selectedMarkerId = uiState.selectedMarkerId,
                    onSelectModel = onSelectArModel,
                    onSelectMarker = onSelectMarker,
                    enabled = !uiState.isSaving
                )

                Text(text = "Audio de pronunciación", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = uiState.audioUrlInput,
                    onValueChange = onAudioUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL de audio (opcional)") },
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
                                stopEditRecorder(activeRecorder)
                                activeRecorder = null
                                isRecording = false
                                pendingRecordedAudio?.let { onRecordedAudioSelected(it) }
                                pendingRecordedAudio = null
                                onError(null)
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    startEditRecording(context, onStarted = { rec, sel ->
                                        activeRecorder = rec
                                        pendingRecordedAudio = sel
                                        isRecording = true
                                        onError(null)
                                    }, onFailure = { onError(it) })
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
                        Icon(imageVector = Icons.Rounded.UploadFile, contentDescription = null)
                        Text(text = "Subir archivo", modifier = Modifier.padding(start = 8.dp))
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
                                stopEditRecorder(activeRecorder)
                                activeRecorder = null
                                isRecording = false
                                pendingRecordedAudio?.let { runCatching { File(it.filePath).delete() } }
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
                        is TeacherAudioSource.Recorded -> "Fuente activa: grabación"
                        null -> "Sin audio seleccionado"
                    }
                    Text(text = sourceLabel, color = Color(0xFF64748B), fontSize = 13.sp)

                    Button(
                        onClick = {
                            val target = resolveEditAudioPlaybackTarget(uiState.audioSource, backendBaseUrl)
                                ?: return@Button
                            runCatching {
                                player.reset()
                                player.setDataSource(context, Uri.parse(target))
                                player.setOnPreparedListener { isPlayingAudio = true; it.start() }
                                player.setOnCompletionListener { isPlayingAudio = false }
                                player.prepareAsync()
                            }.onFailure {
                                isPlayingAudio = false
                                onError("No fue posible reproducir el audio.")
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

                Surface(shape = RoundedCornerShape(24.dp), color = Color.White) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "Estado", fontWeight = FontWeight.Bold)
                        EditStatusRow(label = "Palabra escrita", done = uiState.word.isNotBlank())
                        EditStatusRow(label = "Audio listo", done = uiState.hasAudio)
                        if (uiState.selectedArModelId != null) {
                            EditStatusRow(
                                label = "Modelo y marcador AR",
                                done = uiState.hasArAssociation
                            )
                        }
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
                    modifier = Modifier.fillMaxWidth().height(64.dp),
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
                        Text(text = "Guardar cambios", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                        .navigationBarsPadding()
                )
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
private fun EditStatusRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (done) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
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

private sealed interface EditAudioCopyResult {
    data class Success(val selection: LocalAudioSelection) : EditAudioCopyResult
    data class Error(val message: String) : EditAudioCopyResult
}

private fun copyEditAudioToCache(context: Context, uri: android.net.Uri): EditAudioCopyResult {
    val resolver = context.contentResolver
    val mimeType = resolver.getType(uri)?.lowercase()
    val originalName = resolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
    }
    val extension = when {
        originalName?.contains('.') == true -> originalName.substringAfterLast('.').lowercase()
        mimeType?.contains("mp3") == true -> "mp3"
        mimeType?.contains("wav") == true -> "wav"
        else -> "m4a"
    }
    val allowed = setOf("m4a", "mp3", "wav")
    val mimeOk = mimeType?.let {
        it in setOf("audio/mp4", "audio/x-m4a", "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav", "audio/wave")
    } ?: false
    if (!mimeOk && extension !in allowed) {
        return EditAudioCopyResult.Error("Formato no permitido. Usa m4a, mp3 o wav.")
    }
    val temp = File.createTempFile("edit-audio-", ".$extension", context.cacheDir)
    return runCatching {
        resolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "No se pudo leer el archivo." }
            java.io.FileOutputStream(temp).use { out -> input.copyTo(out) }
        }
        if (temp.length() > 10L * 1024 * 1024) {
            temp.delete()
            throw IllegalStateException("El audio supera el límite de 10MB.")
        }
        LocalAudioSelection(filePath = temp.absolutePath, mimeType = mimeType, originalName = originalName ?: temp.name)
    }.fold(
        onSuccess = { EditAudioCopyResult.Success(it) },
        onFailure = { temp.delete(); EditAudioCopyResult.Error(it.message ?: "No fue posible preparar el audio.") }
    )
}

private fun startEditRecording(
    context: Context,
    onStarted: (MediaRecorder, LocalAudioSelection) -> Unit,
    onFailure: (String) -> Unit
) {
    val outputFile = File(context.cacheDir, "edit-recorded-${System.currentTimeMillis()}.m4a")
    runCatching {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioSamplingRate(44_100)
        recorder.setAudioEncodingBitRate(96_000)
        recorder.setOutputFile(outputFile.absolutePath)
        recorder.prepare()
        recorder.start()
        onStarted(recorder, LocalAudioSelection(outputFile.absolutePath, "audio/mp4", outputFile.name))
    }.onFailure { onFailure("No fue posible iniciar la grabación.") }
}

private fun stopEditRecorder(recorder: MediaRecorder?) {
    if (recorder == null) return
    runCatching { recorder.stop() }
    runCatching { recorder.reset() }
    runCatching { recorder.release() }
}

private fun resolveEditAudioPlaybackTarget(source: TeacherAudioSource?, baseUrl: String): String? {
    return when (source) {
        null -> null
        is TeacherAudioSource.Url -> {
            val v = source.value.trim()
            when {
                v.startsWith("http://") || v.startsWith("https://") -> v
                v.startsWith("/") -> "${baseUrl.removeSuffix("/")}$v"
                else -> "${baseUrl.removeSuffix("/")}/$v"
            }
        }
        is TeacherAudioSource.File -> Uri.fromFile(File(source.localAudio.filePath)).toString()
        is TeacherAudioSource.Recorded -> Uri.fromFile(File(source.localAudio.filePath)).toString()
    }
}
