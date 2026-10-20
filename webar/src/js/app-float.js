import { FloatSceneController } from "./float-scene-controller.js";
import { FloatingCardController } from "./floating-card-controller.js";
import { NativeBridge, parseNativeMessage } from "./native-bridge.js";
import { probeXrCapabilities } from "./xr-capabilities.js";

const HARDCODED_INITIAL_WORD = "ejemplo";

// The native side can push the live session word before `cardController` exists
// (the card mounts only after the async camera permission flow resolves), so the
// latest requested word is remembered here and applied once mounting catches up.
let pendingWord = HARDCODED_INITIAL_WORD;

function setCardWord(word) {
  pendingWord = word;
  cardController?.setWord(word);
}

const elements = {
  root: document.querySelector("#ar-root"),
  chrome: document.querySelector("#float-chrome"),
  activation: document.querySelector("#activation"),
  loading: document.querySelector("#loading"),
  errorCard: document.querySelector("#error-card"),
  errorMessage: document.querySelector("#error-message"),
  activate: document.querySelector("#activate-camera"),
  retry: document.querySelector("#retry-camera"),
  compatibility: document.querySelector("#compatibility"),
};

const bridge = new NativeBridge();
const sceneController = new FloatSceneController({
  root: elements.root,
  onCameraReady: () => {
    showOnly(null);
    elements.chrome?.classList.remove("hidden");
    bridge.send("camera-ready");
  },
  onCameraError: showCameraError,
});

let cardController = null;

elements.activate?.addEventListener("click", () => void activateCamera());
elements.retry?.addEventListener("click", () => void activateCamera());
document.addEventListener("visibilitychange", () => {
  if (document.visibilityState === "hidden") stop({ showActivation: true });
});
window.addEventListener("pagehide", () => stop());
window.addEventListener("beforeunload", () => stop());

async function activateCamera() {
  showOnly(elements.loading);
  if (elements.activate) elements.activate.disabled = true;
  if (elements.retry) elements.retry.disabled = true;
  try {
    const capabilities = await probeXrCapabilities();
    if (!capabilities.camera) {
      throw new Error("Este dispositivo no ofrece una cámara web compatible.");
    }
    const cardRoot = await sceneController.start();
    cardController = new FloatingCardController({ cardRoot });
    cardController.mount(pendingWord);
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
        setCardWord(message.word);
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
  cardController = null;
  elements.chrome?.classList.add("hidden");
  if (elements.errorMessage) elements.errorMessage.textContent = message;
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
  cardController = null;
  sceneController.stop();
  elements.chrome?.classList.add("hidden");
  if (showActivation) showOnly(elements.activation);
}

function showOnly(element) {
  for (const candidate of [elements.activation, elements.loading, elements.errorCard]) {
    candidate?.classList.toggle("hidden", candidate !== element);
  }
}

window.WebAR = Object.freeze({
  receiveNativeMessage,
  stop,
  updateCardWord: setCardWord,
  updateCameraOrientation: (x, y, z, w) => sceneController.setCameraQuaternion(x, y, z, w),
});

void probeXrCapabilities().then(({ camera }) => {
  if (elements.compatibility) {
    elements.compatibility.textContent = camera
      ? "Dispositivo listo para solicitar permiso."
      : "Este dispositivo no expone una cámara compatible.";
  }
});
