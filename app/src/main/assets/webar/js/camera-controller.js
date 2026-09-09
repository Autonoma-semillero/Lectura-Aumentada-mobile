export class CameraController {
  constructor({ root, markers, onCameraReady, onCameraError }) {
    this.root = root;
    this.markers = markers;
    this.onCameraReady = onCameraReady;
    this.onCameraError = onCameraError;
    this.scene = null;
    this.markerEntries = [];
    this.wordModelRoot = null;
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

    const wordModelRoot = document.createElement("a-entity");
    wordModelRoot.setAttribute("id", "word-model-root");
    wordModelRoot.setAttribute("position", "0 0 -2");
    wordModelRoot.setAttribute("rotation", "0 0 0");
    wordModelRoot.setAttribute("scale", "0.6 0.6 0.6");
    wordModelRoot.dataset.targetType = "word";
    camera.appendChild(wordModelRoot);
    scene.appendChild(camera);
    this.root.appendChild(scene);
    this.scene = scene;
    this.wordModelRoot = wordModelRoot;
    return this.markerEntries;
  }

  positionWordTarget({ centerX = 0.5, centerY = 0.5 } = {}) {
    if (!this.wordModelRoot) return false;

    const distance = 2;
    const modelCenterY = Math.min(0.92, Math.max(0.08, centerY - 0.16));
    const { x, y } = this.#unprojectToCameraPlane(centerX, modelCenterY, distance);
    this.wordModelRoot.setAttribute(
      "position",
      `${formatPosition(x)} ${formatPosition(y)} -${distance}`
    );
    return true;
  }

  #unprojectToCameraPlane(centerX, centerY, distance) {
    const camera = this.scene?.camera;
    const Three = window.THREE ?? window.AFRAME?.THREE;
    if (!camera?.projectionMatrix || !Three?.Vector3 || !Three?.Matrix4) {
      return { x: 0, y: 0 };
    }

    try {
      // AR.js copies a calibrated projection matrix without necessarily refreshing
      // camera.projectionMatrixInverse, so invert the live matrix explicitly.
      const inverseProjection = new Three.Matrix4()
        .copy(camera.projectionMatrix)
        .invert();
      const localPoint = new Three.Vector3(
        centerX * 2 - 1,
        1 - centerY * 2,
        0.5
      ).applyMatrix4(inverseProjection);
      if (![localPoint.x, localPoint.y, localPoint.z].every(Number.isFinite) || localPoint.z >= 0) {
        return { x: 0, y: 0 };
      }
      const planeScale = -distance / localPoint.z;
      return { x: localPoint.x * planeScale, y: localPoint.y * planeScale };
    } catch {
      return { x: 0, y: 0 };
    }
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
    const arjsSystem = this.scene?.systems?.arjs;
    const arSession = arjsSystem?._arSession;
    const arSources = uniqueResources([
      arSession?.arSource,
      arjsSystem?.arSource,
      arjsSystem?.arToolkitSource,
    ]);
    const arContexts = uniqueResources([
      arSession?.arContext,
      arjsSystem?.arContext,
      arjsSystem?.arToolkitContext,
    ]);
    for (const source of arSources) {
      source.domElement?.srcObject
        ?.getTracks?.()
        ?.forEach((track) => track.stop());
      disposeSilently(source);
    }
    for (const context of arContexts) disposeSilently(context);
    this.scene?.remove();
    this.scene = null;
    this.markerEntries = [];
    this.wordModelRoot = null;
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

function formatPosition(value) {
  return Math.abs(value) < 0.0005 ? "0" : value.toFixed(3);
}

function disposeSilently(resource) {
  try {
    resource?.dispose?.();
  } catch {
    // Native reloads the WebView document after teardown; continue releasing what remains.
  }
}

function uniqueResources(resources) {
  return [...new Set(resources.filter(Boolean))];
}
