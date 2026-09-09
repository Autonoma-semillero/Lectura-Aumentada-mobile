export class CameraController {
  constructor({ root, markers, onCameraReady, onCameraError }) {
    this.root = root;
    this.markers = markers;
    this.onCameraReady = onCameraReady;
    this.onCameraError = onCameraError;
    this.scene = null;
    this.markerEntries = [];
    this.cameraReadyHandler = () => this.onCameraReady();
    this.cameraErrorHandler = (event) => this.onCameraError(normalizeCameraError(event));
    this.videoReadyHandler = (event) => this.mountCameraVideo(event?.detail?.component);
  }

  async capabilities() {
    if (!navigator.mediaDevices?.getUserMedia) {
      return { camera: false, webxr: false };
    }
    let webxr = false;
    try {
      webxr = Boolean(await navigator.xr?.isSessionSupported?.("immersive-ar"));
    } catch {
      webxr = false;
    }
    return { camera: true, webxr };
  }

  start({ webxr = false } = {}) {
    if (this.scene) return this.markerEntries;
    if (!window.AFRAME || !window.ARjs) throw new Error("El motor AR no está disponible");

    window.addEventListener("camera-init", this.cameraReadyHandler, { once: true });
    window.addEventListener("camera-error", this.cameraErrorHandler, { once: true });
    window.addEventListener("arjs-video-loaded", this.videoReadyHandler, { once: true });

    const scene = document.createElement("a-scene");
    scene.setAttribute("embedded", "");
    scene.setAttribute("vr-mode-ui", "enabled: false");
    scene.setAttribute("device-orientation-permission-ui", "enabled: false");
    scene.setAttribute(
      "renderer",
      "antialias: true; alpha: true; colorManagement: true; precision: medium;"
    );
    scene.setAttribute(
      "arjs",
      "sourceType: webcam; debugUIEnabled: false; detectionMode: mono_and_matrix; matrixCodeType: 3x3;"
    );
    if (webxr) {
      scene.setAttribute(
        "webxr",
        "optionalFeatures: local-floor, bounded-floor, hit-test, dom-overlay; overlayElement: #experience;"
      );
    }

    this.markerEntries = this.markers.map(({ markerId, preset }) => {
      const marker = document.createElement("a-marker");
      marker.setAttribute("preset", preset);
      marker.setAttribute("emitevents", "true");
      marker.setAttribute("smooth", "true");
      marker.setAttribute("smoothCount", "8");
      marker.setAttribute("smoothTolerance", "0.01");
      marker.setAttribute("smoothThreshold", "4");
      marker.dataset.markerId = markerId;

      const modelRoot = document.createElement("a-entity");
      modelRoot.setAttribute("position", "0 0 0");
      modelRoot.setAttribute("rotation", "-90 0 0");
      modelRoot.setAttribute("scale", "0.5 0.5 0.5");
      marker.appendChild(modelRoot);
      scene.appendChild(marker);
      return { markerId, element: marker, modelRoot };
    });

    const camera = document.createElement("a-entity");
    camera.setAttribute("camera", "");
    scene.appendChild(camera);
    this.root.appendChild(scene);
    this.scene = scene;
    return this.markerEntries;
  }

  stop() {
    window.removeEventListener("camera-init", this.cameraReadyHandler);
    window.removeEventListener("camera-error", this.cameraErrorHandler);
    window.removeEventListener("arjs-video-loaded", this.videoReadyHandler);
    for (const video of document.querySelectorAll("video")) {
      for (const track of video.srcObject?.getTracks?.() ?? []) track.stop();
      video.pause();
      video.srcObject = null;
      video.remove();
    }
    this.scene?.systems?.arjs?.arToolkitSource?.domElement?.srcObject
      ?.getTracks?.()
      ?.forEach((track) => track.stop());
    this.scene?.remove();
    this.scene = null;
    this.markerEntries = [];
  }

  mountCameraVideo(video) {
    if (!video) return;

    const activeVideo =
      [...document.querySelectorAll("video")].find((candidate) => candidate.srcObject) ?? video;

    activeVideo.autoplay = true;
    activeVideo.muted = true;
    activeVideo.playsInline = true;
    activeVideo.setAttribute?.("autoplay", "");
    activeVideo.setAttribute?.("muted", "");
    activeVideo.setAttribute?.("playsinline", "");

    if (activeVideo.parentElement !== this.root) this.root.prepend(activeVideo);

    const playback = activeVideo.play?.();
    playback?.catch?.(() =>
      this.onCameraError("No se pudo iniciar la vista en vivo de la cámara.")
    );
  }
}

function normalizeCameraError(event) {
  const name = event?.detail?.error?.name || event?.detail?.name || "CameraError";
  if (name === "NotAllowedError" || name === "PermissionDeniedError") {
    return "Permiso de cámara denegado. Puedes reintentarlo cuando estés listo.";
  }
  if (name === "NotFoundError" || name === "DevicesNotFoundError") {
    return "No se encontró una cámara compatible en este dispositivo.";
  }
  return "La cámara no pudo inicializarse. Comprueba que otra aplicación no la esté usando.";
}
