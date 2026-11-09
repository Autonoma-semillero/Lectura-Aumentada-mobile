import { FloatingCardController } from "./floating-card-controller.js";
import { NativeBridge, parseNativeMessage } from "./native-bridge.js";
import { probeXrCapabilities } from "./xr-capabilities.js";
import { VrSceneController } from "./vr-scene-controller.js";

let pendingWord = "ejemplo";
let cardControllers = [];
const elements = {
  root: document.querySelector("#vr-root"), chrome: document.querySelector("#vr-chrome"),
  activation: document.querySelector("#activation"), loading: document.querySelector("#loading"),
  errorCard: document.querySelector("#error-card"), errorMessage: document.querySelector("#error-message"),
  activate: document.querySelector("#activate-camera"), retry: document.querySelector("#retry-camera"),
  compatibility: document.querySelector("#compatibility"),
};
const bridge = new NativeBridge();
const sceneController = new VrSceneController({
  root: elements.root,
  onCameraReady: () => { showOnly(null); elements.chrome?.classList.remove("hidden"); bridge.send("camera-ready"); },
  onCameraError: showCameraError,
});

elements.activate?.addEventListener("click", () => void activateCamera());
elements.retry?.addEventListener("click", () => void activateCamera());
document.addEventListener("visibilitychange", () => { if (document.visibilityState === "hidden") stop({ showActivation: true }); });
window.addEventListener("pagehide", () => stop());
window.addEventListener("beforeunload", () => stop());

async function activateCamera() {
  showOnly(elements.loading);
  if (elements.activate) elements.activate.disabled = true;
  if (elements.retry) elements.retry.disabled = true;
  try {
    const capabilities = await probeXrCapabilities();
    if (!capabilities.camera) throw new Error("Este dispositivo no ofrece una cámara compatible.");
    const roots = await sceneController.start();
    cardControllers = roots.map((cardRoot, index) => {
      const side = index === 0 ? "left" : "right";
      const controller = new FloatingCardController({ cardRoot, idPrefix: `vr-card-${side}`, cameraSelector: `#vr-camera-${side}` });
      controller.mount(pendingWord);
      return controller;
    });
  } catch (error) {
    showCameraError(error instanceof Error ? error.message : "No fue posible iniciar el modo VR");
  } finally {
    if (elements.activate) elements.activate.disabled = false;
    if (elements.retry) elements.retry.disabled = false;
  }
}

function setCardWord(word) { pendingWord = word; for (const controller of cardControllers) controller.setWord(word); }
function receiveNativeMessage(rawMessage) {
  try {
    const message = parseNativeMessage(rawMessage);
    if (message.type === "update-card-word") setCardWord(message.word);
    if (message.type === "camera-permission-denied") showCameraError("La cámara es necesaria para usar el visor VR.");
  } catch { reportRuntimeError("La respuesta de contenido VR no es válida"); }
}
function showCameraError(message) { stop(); if (elements.errorMessage) elements.errorMessage.textContent = message; showOnly(elements.errorCard); reportRuntimeError(message); }
function reportRuntimeError(message) { try { bridge.send("runtime-error", { message }); } catch { /* Host already closing. */ } }
function stop({ showActivation = false } = {}) {
  for (const controller of cardControllers) controller.unmount();
  cardControllers = [];
  sceneController.stop();
  elements.chrome?.classList.add("hidden");
  if (showActivation) showOnly(elements.activation);
}
function showOnly(element) {
  for (const candidate of [elements.activation, elements.loading, elements.errorCard]) candidate?.classList.toggle("hidden", candidate !== element);
}
window.WebAR = Object.freeze({ receiveNativeMessage, stop, updateCardWord: setCardWord, updateCameraOrientation: (x, y, z, w) => sceneController.setCameraQuaternion(x, y, z, w) });
void probeXrCapabilities().then(({ camera }) => {
  if (elements.compatibility) elements.compatibility.textContent = camera ? "Dispositivo listo. Colócalo en horizontal antes de continuar." : "Este dispositivo no expone una cámara compatible.";
});
