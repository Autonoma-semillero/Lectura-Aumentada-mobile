import { CardTextController } from "./card-text-controller.js";
import { FloatSceneController } from "./float-scene-controller.js";
import { FloatingCardController } from "./floating-card-controller.js";
import { NativeBridge, parseNativeMessage } from "./native-bridge.js";
import { probeXrCapabilities } from "./xr-capabilities.js";

const HARDCODED_INITIAL_WORD = "ejemplo";

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
};

const bridge = new NativeBridge();
const sceneController = new FloatSceneController({
  root: elements.root,
  onCameraReady: () => {
    showOnly(null);
    setText(elements.cameraState, "activa");
    setText(elements.mainStatus, "Tarjeta flotante activa");
    bridge.send("camera-ready");
  },
  onCameraError: showCameraError,
});

let cardController = null;
let textController = null;

elements.activate?.addEventListener("click", () => void activateCamera());
elements.retry?.addEventListener("click", () => void activateCamera());
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden") stop({ showActivation: true });
});
window.addEventListener("pagehide", () => stop());
window.addEventListener("beforeunload", () => stop());

async function activateCamera() {
  showOnly(elements.loading);
  setText(elements.cameraState, "solicitando permiso");
  setText(elements.mainStatus, "Preparando cámara…");
  if (elements.activate) elements.activate.disabled = true;
  if (elements.retry) elements.retry.disabled = true;
  try {
    const capabilities = await probeXrCapabilities();
    setText(elements.webxrState, capabilities.webxr ? "compatible" : "no disponible (HUD-3D)");
    if (!capabilities.camera) {
      throw new Error("Este dispositivo no ofrece una cámara web compatible.");
    }
    const cardRoot = await sceneController.start();
    cardController = new FloatingCardController({ cardRoot });
    cardController.mount();
    textController = new CardTextController({ parent: cardController.plane });
    textController.mount(HARDCODED_INITIAL_WORD);
  } catch (error) {
    showCameraError(error instanceof Error ? error.message : "No fue posible iniciar la cámara");
  } finally {
    if (elements.activate) elements.activate.disabled = false;
    if (elements.retry) elements.retry.disabled = false;
  }
}

function receiveNativeMessage(rawMessage) {
  try {
    const message = parseNativeMessage(rawMessage);
    switch (message.type) {
      case "update-card-word":
        textController?.setWord(message.word);
        break;
      case "camera-permission-denied":
        showCameraError("La cámara es necesaria para mostrar la tarjeta flotante.");
        break;
      default:
        break;
    }
  } catch {
    reportRuntimeError("La respuesta de contenido AR no es válida");
  }
}

function showCameraError(message) {
  sceneController.stop();
  cardController?.unmount();
  textController?.unmount();
  cardController = null;
  textController = null;
  if (elements.errorMessage) elements.errorMessage.textContent = message;
  setText(elements.cameraState, "error");
  setText(elements.mainStatus, "Cámara no disponible");
  showOnly(elements.errorCard);
  reportRuntimeError(message);
}

function reportRuntimeError(message) {
  try {
    bridge.send("runtime-error", { message });
  } catch {
    // The native host may already be closing.
  }
}

function stop({ showActivation = false } = {}) {
  cardController?.unmount();
  textController?.unmount();
  cardController = null;
  textController = null;
  sceneController.stop();
  setText(elements.cameraState, "inactiva");
  setText(elements.mainStatus, "Cámara inactiva");
  if (showActivation) showOnly(elements.activation);
}

function showOnly(element) {
  for (const candidate of [elements.activation, elements.loading, elements.errorCard]) {
    candidate?.classList.toggle("hidden", candidate !== element);
  }
}

function setText(element, value) {
  if (element) element.textContent = value;
}

window.WebAR = Object.freeze({ receiveNativeMessage, stop, updateCardWord: (word) => textController?.setWord(word) });

void probeXrCapabilities().then(({ camera, webxr }) => {
  if (elements.compatibility) {
    elements.compatibility.textContent = camera
      ? "Dispositivo listo para solicitar permiso."
      : "Este dispositivo no expone una cámara compatible.";
  }
  setText(elements.webxrState, webxr ? "compatible" : "no disponible (HUD-3D)");
});
