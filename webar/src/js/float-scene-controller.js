export function hasWebGlSupport() {
  try {
    const canvas = document.createElement("canvas");
    return Boolean(canvas.getContext("webgl") || canvas.getContext("experimental-webgl"));
  } catch {
    return false;
  }
}

export class FloatSceneController {
  constructor({ root, onCameraReady, onCameraError }) {
    this.root = root;
    this.onCameraReady = onCameraReady;
    this.onCameraError = onCameraError;
    this.scene = null;
    this.camera = null;
    this.cardRoot = null;
    this.video = null;
  }

  async start() {
    if (this.scene) return this.cardRoot;
    if (!window.AFRAME) throw new Error("El motor AR no está disponible");
    if (!hasWebGlSupport()) {
      throw new Error("Este dispositivo no admite renderizado 3D (WebGL no disponible).");
    }

    const stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: "environment" },
      audio: false,
    });

    const video = document.createElement("video");
    video.autoplay = true;
    video.muted = true;
    video.playsInline = true;
    video.setAttribute("autoplay", "");
    video.setAttribute("muted", "");
    video.setAttribute("playsinline", "");
    video.srcObject = stream;
    this.root.prepend(video);
    this.video = video;
    try {
      await video.play();
    } catch {
      this.onCameraError?.("No se pudo iniciar la vista en vivo de la cámara.");
    }

    const scene = document.createElement("a-scene");
    scene.setAttribute("embedded", "");
    scene.setAttribute("vr-mode-ui", "enabled: false");
    scene.setAttribute("device-orientation-permission-ui", "enabled: false");
    scene.setAttribute(
      "renderer",
      "antialias: true; alpha: true; colorManagement: true; precision: medium;"
    );

    const camera = document.createElement("a-entity");
    camera.setAttribute("camera", "");
    camera.setAttribute("look-controls", "");

    const cardRoot = document.createElement("a-entity");
    cardRoot.setAttribute("id", "floating-card-root");
    cardRoot.setAttribute("position", "0 0 -2");
    camera.appendChild(cardRoot);

    scene.appendChild(camera);
    this.root.appendChild(scene);

    this.scene = scene;
    this.camera = camera;
    this.cardRoot = cardRoot;
    this.onCameraReady?.();
    return cardRoot;
  }

  stop() {
    this.video?.srcObject?.getTracks?.().forEach((track) => track.stop());
    this.video?.pause?.();
    if (this.video) this.video.srcObject = null;
    this.video?.remove?.();
    this.video = null;
    this.scene?.remove?.();
    this.scene = null;
    this.camera = null;
    this.cardRoot = null;
  }
}
