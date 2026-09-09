import test from "node:test";
import assert from "node:assert/strict";
import { CameraController } from "../src/js/camera-controller.js";

test("subscribes to the AR.js camera lifecycle on window", (context) => {
  const previousWindow = globalThis.window;
  const previousDocument = globalThis.document;
  const addedEvents = [];
  const removedEvents = [];
  const windowListeners = new Map();
  let documentSubscriptions = 0;
  let mountedVideo = null;
  let playCalls = 0;
  let queriedVideos = [];
  let removedVideos = 0;

  const createElement = (tagName = "unknown") => ({
    tagName,
    dataset: {},
    attributes: new Map(),
    children: [],
    setAttribute(name, value) {
      this.attributes.set(name, value);
    },
    appendChild(child) {
      this.children.push(child);
      child.parentElement = this;
    },
    remove() {},
  });

  globalThis.window = {
    AFRAME: {},
    ARjs: {},
    innerWidth: 400,
    innerHeight: 800,
    addEventListener(type, listener) {
      addedEvents.push(type);
      windowListeners.set(type, listener);
    },
    removeEventListener(type) {
      removedEvents.push(type);
    },
  };
  globalThis.document = {
    createElement,
    querySelectorAll() {
      return queriedVideos;
    },
    addEventListener() {
      documentSubscriptions += 1;
    },
  };

  context.after(() => {
    globalThis.window = previousWindow;
    globalThis.document = previousDocument;
  });

  const root = {
    ...createElement(),
    prepend(video) {
      mountedVideo = video;
      video.parentElement = root;
    },
  };
  const controller = new CameraController({
    root,
    markers: [],
    onCameraReady() {},
    onCameraError() {},
  });

  controller.start();
  assert.deepEqual(addedEvents, ["camera-init", "camera-error", "arjs-video-loaded"]);
  assert.equal(documentSubscriptions, 0);
  assert.ok(controller.wordModelRoot, "a camera-relative root is created for OCR words");
  assert.equal(controller.wordModelRoot.dataset.targetType, "word");
  assert.equal(controller.wordModelRoot.attributes.get("position"), "0 0 -2");

  // Before A-Frame exposes scene.camera, positioning falls back safely to the center.
  assert.equal(controller.positionWordTarget({ centerX: 1, centerY: 0 }), true);
  assert.equal(controller.wordModelRoot.attributes.get("position"), "0 0 -2");

  class ProjectionVector {
    constructor(x, y, z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }
    applyMatrix4(matrix) {
      this.x *= matrix.xScale;
      this.y *= matrix.yScale;
      this.z = matrix.zValue;
      return this;
    }
  }
  let copiedProjection = null;
  class ProjectionMatrix {
    copy(matrix) {
      copiedProjection = matrix;
      this.xScale = matrix.xScale;
      this.yScale = matrix.yScale;
      this.zValue = matrix.zValue;
      return this;
    }
    invert() { return this; }
  }
  window.THREE = { Vector3: ProjectionVector, Matrix4: ProjectionMatrix };
  const liveProjection = { xScale: 4, yScale: 2, zValue: -4 };
  controller.scene.camera = {
    projectionMatrix: liveProjection,
    projectionMatrixInverse: { xScale: 999, yScale: 999, zValue: -4 },
    updateProjectionMatrix() { throw new Error("must preserve AR.js projection"); },
  };
  assert.equal(controller.positionWordTarget({ centerX: 0.75, centerY: 0.5 }), true);
  assert.equal(copiedProjection, liveProjection);
  assert.equal(controller.wordModelRoot.attributes.get("position"), "1.000 0.320 -2");

  const staleVideo = {
    parentElement: globalThis.document,
    srcObject: null,
    pause() {},
    remove() {
      removedVideos += 1;
    },
  };
  const videoAttributes = new Map();
  const video = {
    parentElement: globalThis.document,
    srcObject: { getTracks: () => [] },
    setAttribute(name, value) {
      videoAttributes.set(name, value);
    },
    play() {
      playCalls += 1;
      return Promise.resolve();
    },
    pause() {},
    remove() {
      removedVideos += 1;
    },
  };
  queriedVideos = [staleVideo, video];
  windowListeners.get("arjs-video-loaded")({ detail: { component: staleVideo } });
  assert.equal(mountedVideo, video);
  assert.equal(video.parentElement, root);
  assert.equal(video.autoplay, true);
  assert.equal(video.muted, true);
  assert.equal(video.playsInline, true);
  assert.deepEqual([...videoAttributes.keys()], ["autoplay", "muted", "playsinline"]);
  assert.equal(playCalls, 1);

  const previousWordRoot = controller.wordModelRoot;
  let sourceDisposeCalls = 0;
  let contextDisposeCalls = 0;
  controller.scene.systems = {
    arjs: {
      _arSession: {
        arSource: {
          dispose() {
            sourceDisposeCalls += 1;
            throw new Error("vendor source cleanup failed after releasing tracks");
          },
        },
        arContext: { dispose() { contextDisposeCalls += 1; } },
      },
    },
  };
  controller.stop();
  assert.deepEqual(removedEvents, ["camera-init", "camera-error", "arjs-video-loaded"]);
  assert.equal(removedVideos, 2);
  assert.equal(sourceDisposeCalls, 1);
  assert.equal(contextDisposeCalls, 1);
  assert.equal(controller.wordModelRoot, null);

  // Error-card retry can create a clean scene in the same document after teardown.
  queriedVideos = [];
  controller.start();
  assert.ok(controller.wordModelRoot);
  assert.notEqual(controller.wordModelRoot, previousWordRoot);
  controller.stop();
});
