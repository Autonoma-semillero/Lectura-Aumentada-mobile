import { AudioController } from "./audio-controller.js";
import { CameraController } from "./camera-controller.js";
import { DetectionController } from "./detection-controller.js";
import { MARKERS } from "./marker-config.js";
import { ModelController } from "./model-controller.js";
import { NativeBridge, parseAsset, parseNativeMessage } from "./native-bridge.js";
import { beginOcrScanning, endOcrScanning } from "./scan-ui.js";
import {
  WORD_TARGET_MODEL_ID,
  WordTargetGate,
  resolveAssetTarget,
} from "./word-target.js";

const elements = {
  root: document.querySelector("#ar-root"),
  activation: document.querySelector("#activation"),
  loading: document.querySelector("#loading"),
  errorCard: document.querySelector("#error-card"),
  errorMessage: document.querySelector("#error-message"),
  activate: document.querySelector("#activate-camera"),
  retry: document.querySelector("#retry-camera"),
  compatibility: document.querySelector("#compatibility"),
  mainStatus: document.querySelector("#main-status"),
  cameraState: document.querySelector("#camera-state"),
  webxrState: document.querySelector("#webxr-state"),
  markerState: document.querySelector("#marker-state"),
  modelState: document.querySelector("#model-state"),
  audioState: document.querySelector("#audio-state"),
  repeatAudio: document.querySelector("#repeat-audio"),
  debugPanel: document.querySelector("#debug-panel"),
};

const bridge = new NativeBridge();
const modelController = new ModelController(
  (state) => setText(elements.modelState, state),
  {
    onModelReady: (assetId) => bridge.send("model-ready", { assetId }),
    onModelError: (message, assetId) => bridge.send("model-error", { assetId, message }),
  }
);
const audioController = new AudioController({
  repeatButton: elements.repeatAudio,
  onStateChange: (state) => setText(elements.audioState, state),
});
const detectionController = new DetectionController({
  onFound: markerFound,
  onLost: markerLost,
});
const wordTargetGate = new WordTargetGate();
let activeContentSource = null;
const cameraController = new CameraController({
  root: elements.root,
  markers: MARKERS,
  onCameraReady: () => {
    showOnly(null);
    setText(elements.cameraState, "activa");
    setText(elements.mainStatus, "Enfoca una palabra o tarjeta");
    beginOcrScanning(elements.debugPanel, () => bridge.send("camera-ready"));
  },
  onCameraError: showCameraError,
});

elements.activate.addEventListener("click", () => void activateCamera());
elements.retry.addEventListener("click", requestCameraRetry);
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden") stop({ showActivation: true });
});
window.addEventListener("pagehide", () => stop());
window.addEventListener("beforeunload", () => stop());
window.addEventListener("error", () => reportRuntimeError("Error inesperado en el motor AR"));
window.addEventListener("unhandledrejection", () => reportRuntimeError("Operación AR no completada"));

async function activateCamera() {
  showOnly(elements.loading);
  setText(elements.cameraState, "solicitando permiso");
  setText(elements.mainStatus, "Preparando cámara…");
  elements.activate.disabled = true;
  elements.retry.disabled = true;
  try {
    await audioController.unlock();
    const capabilities = await cameraController.capabilities();
    setText(elements.webxrState, capabilities.webxr ? "compatible" : "modo marcador");
    if (!capabilities.camera) throw new Error("Este dispositivo no ofrece una cámara web compatible.");
    const markerEntries = cameraController.start({ webxr: capabilities.webxr });
    detectionController.attach(markerEntries);
    for (const entry of markerEntries) {
      modelController.registerMarker(entry.markerId, entry.modelRoot);
    }
    modelController.registerMarker(WORD_TARGET_MODEL_ID, cameraController.wordModelRoot);
  } catch (error) {
    showCameraError(error instanceof Error ? error.message : "No fue posible iniciar la cámara");
  } finally {
    elements.activate.disabled = false;
    elements.retry.disabled = false;
  }
}

function requestCameraRetry() {
  elements.retry.disabled = true;
  setText(elements.mainStatus, "Reiniciando cámara…");
  try {
    bridge.send("camera-retry-requested");
  } catch {
    elements.retry.disabled = false;
    reportRuntimeError("No fue posible reiniciar la cámara");
  }
}

function markerFound(markerId) {
  const clearedWordTarget = wordTargetGate.reset();
  if (clearedWordTarget) clearContent("word", clearedWordTarget.normalizedWord);
  setText(elements.markerState, markerId);
  setText(elements.mainStatus, "Marcador detectado. Consultando contenido…");
  bridge.send("marker-found", { markerId });
}

function markerLost(markerId) {
  if (!wordTargetGate.activeTarget) {
    clearContent("marker", markerId);
    setText(elements.markerState, "perdido");
    setText(elements.mainStatus, "Marcador perdido. Enfoca una palabra o tarjeta.");
  }
  bridge.send("marker-lost", { markerId });
}

function receiveNativeMessage(rawMessage) {
  try {
    const message = parseNativeMessage(rawMessage);
    switch (message.type) {
      case "asset-ready": {
        const asset = parseAsset(message.asset);
        const atomicWordTarget = message.target?.type === "word"
          ? message.target
          : message.wordTarget;
        if (atomicWordTarget) {
          if (detectionController.gate.activeMarkerId) return;
          activateWordTarget(atomicWordTarget.word || asset.word, atomicWordTarget);
        }
        const targetId = resolveAssetTarget({
          asset,
          activeWordTarget: wordTargetGate.activeTarget,
          activeMarkerId: detectionController.gate.activeMarkerId,
        });
        if (!targetId) return;
        const isWordTarget = targetId === WORD_TARGET_MODEL_ID;
        setText(elements.mainStatus, `Mostrando ${asset.word}`);
        modelController.show(targetId, asset.model3dUrl, asset.id);
        activeContentSource = {
          type: isWordTarget ? "word" : "marker",
          id: isWordTarget ? wordTargetGate.activeTarget.normalizedWord : asset.markerId,
        };
        void audioController.loadAndPlay(asset.audioUrl);
        break;
      }
      case "activate-word-target": {
        const target = message.target || message;
        activateWordTarget(target.word, target);
        break;
      }
      case "clear-word-target": {
        const cleared = wordTargetGate.clear(message.word);
        if (!cleared) return;
        clearContent("word", cleared.normalizedWord);
        const activeMarkerId = detectionController.gate.activeMarkerId;
        setText(elements.markerState, activeMarkerId || "ninguno");
        setText(
          elements.mainStatus,
          activeMarkerId ? "Marcador detectado. Consultando contenido…" : "Enfoca una palabra o tarjeta"
        );
        break;
      }
      case "word-not-found":
        if (message.target?.type === "word" || message.wordTarget) {
          if (detectionController.gate.activeMarkerId) return;
          const target = message.target?.type === "word" ? message.target : message.wordTarget;
          activateWordTarget(target.word || message.word, target);
        }
        if (!wordTargetGate.matches(message.word)) return;
        clearContent("word", wordTargetGate.activeTarget.normalizedWord);
        setText(elements.mainStatus, "La palabra no tiene contenido asociado");
        break;
      case "marker-not-found":
        if (message.markerId !== detectionController.gate.activeMarkerId) return;
        wordTargetGate.reset();
        clearContent();
        setText(elements.markerState, message.markerId);
        setText(elements.mainStatus, "La tarjeta no tiene contenido asociado");
        break;
      case "asset-error":
        clearContent();
        setText(elements.mainStatus, message.message || "Error al cargar el contenido");
        break;
      case "clear-marker":
        if (!wordTargetGate.activeTarget) clearContent("marker", message.markerId);
        break;
      case "camera-permission-denied":
        showCameraError("La cámara es necesaria para reconocer palabras y tarjetas.");
        break;
      default:
        break;
    }
  } catch {
    reportRuntimeError("La respuesta de contenido AR no es válida");
  }
}

function showCameraError(message) {
  cameraController.stop();
  detectionController.detach();
  wordTargetGate.reset();
  activeContentSource = null;
  modelController.clear();
  audioController.clear();
  endOcrScanning(elements.debugPanel);
  elements.errorMessage.textContent = message;
  setText(elements.cameraState, "error");
  setText(elements.mainStatus, "Cámara no disponible");
  showOnly(elements.errorCard);
}

function reportRuntimeError(message) {
  setText(elements.mainStatus, message);
  try {
    bridge.send("runtime-error", { message });
  } catch {
    // The native host may already be closing.
  }
}

function stop({ showActivation = false } = {}) {
  detectionController.detach();
  wordTargetGate.reset();
  activeContentSource = null;
  modelController.destroy();
  audioController.clear();
  cameraController.stop();
  endOcrScanning(elements.debugPanel);
  setText(elements.cameraState, "inactiva");
  setText(elements.markerState, "ninguno");
  setText(elements.mainStatus, "Cámara inactiva");
  if (showActivation) showOnly(elements.activation);
}

function showOnly(element) {
  for (const candidate of [elements.activation, elements.loading, elements.errorCard]) {
    candidate.classList.toggle("hidden", candidate !== element);
  }
}

function setText(element, value) {
  element.textContent = value;
}

function clearContent(type, id) {
  if (
    type &&
    activeContentSource &&
    (activeContentSource.type !== type || activeContentSource.id !== id)
  ) {
    return false;
  }
  modelController.clear();
  audioController.clear();
  activeContentSource = null;
  return true;
}

function activateWordTarget(word, { centerX, centerY } = {}) {
  if (detectionController.gate.activeMarkerId) return null;
  const transition = wordTargetGate.activate(word, { centerX, centerY });
  if (!transition) return wordTargetGate.activeTarget;

  cameraController.positionWordTarget(transition);
  if (transition.wordChanged) clearContent();
  setText(elements.markerState, `palabra: ${transition.word}`);
  setText(elements.mainStatus, `Palabra «${transition.word}» detectada. Consultando contenido…`);
  return wordTargetGate.activeTarget;
}

window.WebAR = Object.freeze({ receiveNativeMessage, stop });

void cameraController.capabilities().then(({ camera, webxr }) => {
  elements.compatibility.textContent = camera
    ? "Dispositivo listo para solicitar permiso."
    : "Este dispositivo no expone una cámara compatible.";
  setText(elements.webxrState, webxr ? "compatible" : "modo marcador");
});
