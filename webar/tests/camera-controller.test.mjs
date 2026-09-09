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

  const createElement = () => ({
    dataset: {},
    setAttribute() {},
    appendChild() {},
    remove() {},
  });

  globalThis.window = {
    AFRAME: {},
    ARjs: {},
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
      return [];
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

  const video = { parentElement: globalThis.document };
  windowListeners.get("arjs-video-loaded")({ detail: { component: video } });
  assert.equal(mountedVideo, video);
  assert.equal(video.parentElement, root);

  controller.stop();
  assert.deepEqual(removedEvents, ["camera-init", "camera-error", "arjs-video-loaded"]);
});
