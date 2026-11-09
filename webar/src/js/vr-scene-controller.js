import { hasWebGlSupport } from "./float-scene-controller.js";

export class VrSceneController {
  constructor({ root, onCameraReady, onCameraError }) {
    Object.assign(this, { root, onCameraReady, onCameraError });
    this.stream = this.scene = this.camera = this.cardRoot = null;
    this.video = this.cameraCanvas = null;
    this.cameraFrame = null;
    this.restoreRender = null;
    this.initialOrientation = null;
  }

  async start() {
    if (this.scene) return this.cardRoot;
    if (!window.AFRAME) throw new Error("El motor VR no está disponible");
    if (!hasWebGlSupport()) throw new Error("Este dispositivo no admite renderizado 3D.");
    this.stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" }, audio: false });
    try {
      const video = document.createElement("video");
      Object.assign(video, { autoplay: true, muted: true, playsInline: true, srcObject: this.stream, className: "vr-source-video" });
      for (const attribute of ["autoplay", "muted", "playsinline"]) video.setAttribute(attribute, "");
      this.root.appendChild(video);
      this.video = video;
      await video.play();

      const cameraCanvas = document.createElement("canvas");
      cameraCanvas.className = "vr-camera-canvas";
      this.root.appendChild(cameraCanvas);
      this.cameraCanvas = cameraCanvas;
      this.startCameraCanvas(video, cameraCanvas);

      const scene = document.createElement("a-scene");
      scene.setAttribute("embedded", "");
      scene.setAttribute("vr-mode-ui", "enabled: false");
      scene.setAttribute("device-orientation-permission-ui", "enabled: false");
      scene.setAttribute("renderer", "antialias: true; alpha: true; colorManagement: true; precision: medium;");
      const camera = document.createElement("a-entity");
      camera.setAttribute("id", "vr-camera");
      camera.setAttribute("camera", "fov: 80");
      const cardRoot = document.createElement("a-entity");
      cardRoot.setAttribute("id", "vr-card-root");
      cardRoot.setAttribute("position", "0 0 -2");
      scene.appendChild(cardRoot);
      scene.appendChild(camera);
      this.root.appendChild(scene);
      Object.assign(this, { scene, camera, cardRoot });

      await this.waitForRenderStart(scene);
      scene.object3D.background = null;
      scene.renderer.setClearColor(0x000000, 0);
      scene.canvas.style.background = "transparent";
      this.installStereoRenderer(scene, camera);
      this.onCameraReady?.();
      return cardRoot;
    } catch (error) {
      this.stop();
      throw error;
    }
  }

  waitForRenderStart(scene) {
    if (scene.renderer) return Promise.resolve();
    return new Promise((resolve) => scene.addEventListener("renderstart", resolve, { once: true }));
  }

  startCameraCanvas(video, canvas) {
    const context = canvas.getContext("2d", { alpha: false });
    const draw = () => {
      const width = Math.max(2, this.root.clientWidth || window.innerWidth || 2);
      const height = Math.max(2, this.root.clientHeight || window.innerHeight || 2);
      if (canvas.width !== width || canvas.height !== height) {
        canvas.width = width;
        canvas.height = height;
      }
      const half = Math.floor(width / 2);
      const sourceWidth = video.videoWidth || 640;
      const sourceHeight = video.videoHeight || 480;
      const sourceAspect = sourceWidth / sourceHeight;
      const eyeAspect = half / height;
      let sx = 0, sy = 0, sw = sourceWidth, sh = sourceHeight;
      if (sourceAspect > eyeAspect) { sw = sourceHeight * eyeAspect; sx = (sourceWidth - sw) / 2; }
      else { sh = sourceWidth / eyeAspect; sy = (sourceHeight - sh) / 2; }
      context.drawImage(video, sx, sy, sw, sh, 0, 0, half, height);
      context.drawImage(video, sx, sy, sw, sh, half, 0, width - half, height);
      this.cameraFrame = requestAnimationFrame(draw);
    };
    draw();
  }

  installStereoRenderer(scene, cameraEntity) {
    const renderer = scene.renderer;
    const THREE = window.AFRAME.THREE;
    const stereo = new THREE.StereoCamera();
    stereo.eyeSep = 0.064;
    stereo.aspect = 0.5;
    stereo.cameraL.layers.enable(0);
    stereo.cameraR.layers.enable(0);
    const originalRender = renderer.render.bind(renderer);
    renderer.autoClear = false;
    renderer.render = (sceneObject, activeCamera) => {
      if (activeCamera !== cameraEntity.getObject3D("camera")) return originalRender(sceneObject, activeCamera);
      // WebGLRenderer#setViewport/setScissor expect logical CSS pixels and apply
      // devicePixelRatio internally. Using the drawing-buffer size here scaled
      // every eye a second time on high-density phones, leaving only a cropped
      // fragment of the card visible.
      const size = renderer.getSize(new THREE.Vector2());
      const halfWidth = Math.floor(size.x / 2);
      stereo.update(activeCamera);
      renderer.setScissorTest(true);
      renderer.clear(true, true, true);
      renderer.setViewport(0, 0, halfWidth, size.y);
      renderer.setScissor(0, 0, halfWidth, size.y);
      originalRender(sceneObject, stereo.cameraL);
      renderer.setViewport(halfWidth, 0, size.x - halfWidth, size.y);
      renderer.setScissor(halfWidth, 0, size.x - halfWidth, size.y);
      originalRender(sceneObject, stereo.cameraR);
      renderer.setScissorTest(false);
      renderer.setViewport(0, 0, size.x, size.y);
    };
    this.restoreRender = () => { renderer.render = originalRender; renderer.setScissorTest(false); };
  }

  setCameraQuaternion(x, y, z, w) {
    if (!this.camera?.object3D) return;
    const THREE = window.AFRAME.THREE;
    const current = new THREE.Quaternion(x, y, z, w).normalize();
    if (!this.initialOrientation) this.initialOrientation = current.clone();
    const relative = this.initialOrientation.clone().invert().multiply(current);
    this.camera.object3D.quaternion.copy(relative);
  }

  stop() {
    this.restoreRender?.();
    this.restoreRender = null;
    this.stream?.getTracks?.().forEach((track) => track.stop());
    this.stream = null;
    if (this.cameraFrame != null) cancelAnimationFrame(this.cameraFrame);
    this.cameraFrame = null;
    this.video?.pause?.();
    if (this.video) this.video.srcObject = null;
    this.video?.remove?.();
    this.video = null;
    this.cameraCanvas?.remove?.();
    this.cameraCanvas = null;
    this.scene?.remove?.();
    this.scene = this.camera = this.cardRoot = null;
    this.initialOrientation = null;
  }
}
