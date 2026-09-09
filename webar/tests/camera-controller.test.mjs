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

  controller.stop();
  assert.deepEqual(removedEvents, ["camera-init", "camera-error", "arjs-video-loaded"]);
  assert.equal(removedVideos, 2);
});
