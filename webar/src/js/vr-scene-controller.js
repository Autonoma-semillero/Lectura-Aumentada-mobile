import { hasWebGlSupport } from "./float-scene-controller.js";

const EYES = ["left", "right"];

export class VrSceneController {
  constructor({ root, onCameraReady, onCameraError }) {
    this.root = root;
    this.onCameraReady = onCameraReady;
    this.onCameraError = onCameraError;
    this.stream = null;
    this.eyes = [];
  }

  async start() {
    if (this.eyes.length) return this.eyes.map((eye) => eye.cardRoot);
    if (!window.AFRAME) throw new Error("El motor VR no está disponible");
    if (!hasWebGlSupport()) throw new Error("Este dispositivo no admite renderizado 3D.");
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" }, audio: false });
    this.stream = stream;
    try {
      for (const side of EYES) this.eyes.push(await this.createEye(side, stream));
      this.onCameraReady?.();
      return this.eyes.map((eye) => eye.cardRoot);
    } catch (error) {
      this.stop();
      throw error;
    }
  }

  async createEye(side, stream) {
    const eye = document.createElement("div");
    eye.className = `vr-eye vr-eye--${side}`;
    eye.setAttribute("aria-hidden", "true");
    const video = document.createElement("video");
    video.autoplay = true;
    video.muted = true;
    video.playsInline = true;
    video.setAttribute("autoplay", "");
    video.setAttribute("muted", "");
    video.setAttribute("playsinline", "");
    video.srcObject = stream;
    eye.appendChild(video);
    const scene = document.createElement("a-scene");
    scene.setAttribute("embedded", "");
    scene.setAttribute("vr-mode-ui", "enabled: false");
    scene.setAttribute("device-orientation-permission-ui", "enabled: false");
    scene.setAttribute("renderer", "antialias: true; alpha: true; colorManagement: true; precision: medium;");
    const camera = document.createElement("a-entity");
    camera.setAttribute("id", `vr-camera-${side}`);
    camera.setAttribute("camera", "");
    camera.setAttribute("position", side === "left" ? "-0.032 0 0" : "0.032 0 0");
    const cardRoot = document.createElement("a-entity");
    cardRoot.setAttribute("id", `vr-card-root-${side}`);
    cardRoot.setAttribute("position", "0 0 -2");
    scene.appendChild(cardRoot);
    scene.appendChild(camera);
    eye.appendChild(scene);
    this.root.appendChild(eye);
    await video.play();
    return { side, eye, video, scene, camera, cardRoot };
  }

  setCameraQuaternion(x, y, z, w) {
    for (const eye of this.eyes) eye.camera?.object3D?.quaternion.set(x, y, z, w);
  }

  stop() {
    this.stream?.getTracks?.().forEach((track) => track.stop());
    this.stream = null;
    for (const eye of this.eyes) {
      eye.video?.pause?.();
      if (eye.video) eye.video.srcObject = null;
      eye.eye?.remove?.();
    }
    this.eyes = [];
  }
}
