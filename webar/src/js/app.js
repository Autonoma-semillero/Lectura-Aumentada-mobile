import { AudioController } from "./audio-controller.js";
import { CameraController } from "./camera-controller.js";
import { DetectionController } from "./detection-controller.js";
import { MARKERS } from "./marker-config.js";
import { ModelController } from "./model-controller.js";
import { NativeBridge, parseAsset, parseNativeMessage } from "./native-bridge.js";

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
};

const bridge = new NativeBridge();
const modelController = new ModelController((state) => setText(elements.modelState, state));
const audioController = new AudioController({
  repeatButton: elements.repeatAudio,
  onStateChange: (state) => setText(elements.audioState, state),
});
const detectionController = new DetectionController({
  onFound: markerFound,
  onLost: markerLost,
});
const cameraController = new CameraController({
  root: elements.root,
  markers: MARKERS,
  onCameraReady: () => {
    showOnly(null);
    setText(elements.cameraState, "activa");
    setText(elements.mainStatus, "Enfoca una tarjeta");
  },
  onCameraError: showCameraError,
});

elements.activate.addEventListener("click", () => void activateCamera());
elements.retry.addEventListener("click", () => void activateCamera());
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
  } catch (error) {
    showCameraError(error instanceof Error ? error.message : "No fue posible iniciar la cámara");
  } finally {
    elements.activate.disabled = false;
    elements.retry.disabled = false;
  }
}

function markerFound(markerId) {
  setText(elements.markerState, markerId);
  setText(elements.mainStatus, "Marcador detectado. Consultando contenido…");
  bridge.send("marker-found", { markerId });
}

function markerLost(markerId) {
  modelController.clear();
  audioController.clear();
  setText(elements.markerState, "perdido");
  setText(elements.mainStatus, "Marcador perdido. Enfoca una tarjeta.");
  bridge.send("marker-lost", { markerId });
}

function receiveNativeMessage(rawMessage) {
  try {
    const message = parseNativeMessage(rawMessage);
    switch (message.type) {
      case "asset-ready": {
        const asset = parseAsset(message.asset);
        if (asset.markerId !== detectionController.gate.activeMarkerId) return;
        setText(elements.mainStatus, `Mostrando ${asset.word}`);
        modelController.show(asset.markerId, asset.model3dUrl);
        void audioController.loadAndPlay(asset.audioUrl);
        break;
      }
      case "marker-not-found":
        modelController.clear();
        audioController.clear();
        setText(elements.mainStatus, "La tarjeta no tiene contenido asociado");
        break;
      case "asset-error":
        modelController.clear();
        audioController.clear();
        setText(elements.mainStatus, message.message || "Error al cargar el contenido");
        break;
      case "clear-marker":
        modelController.clear();
        audioController.clear();
        break;
      case "camera-permission-denied":
        showCameraError("La cámara es necesaria para reconocer las tarjetas.");
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
  modelController.clear();
  audioController.clear();
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
  modelController.destroy();
  audioController.clear();
  cameraController.stop();
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

window.WebAR = Object.freeze({ receiveNativeMessage, stop });

void cameraController.capabilities().then(({ camera, webxr }) => {
  elements.compatibility.textContent = camera
    ? "Dispositivo listo para solicitar permiso."
    : "Este dispositivo no expone una cámara compatible.";
  setText(elements.webxrState, webxr ? "compatible" : "modo marcador");
});
