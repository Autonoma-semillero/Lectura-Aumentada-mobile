import test from "node:test";
import assert from "node:assert/strict";
import { CameraController } from "../src/js/camera-controller.js";

test("subscribes to the AR.js camera lifecycle on window", (context) => {
  const previousWindow = globalThis.window;
  const previousDocument = globalThis.document;
  const addedEvents = [];
  const removedEvents = [];
  let documentSubscriptions = 0;

  const createElement = () => ({
    dataset: {},
    setAttribute() {},
    appendChild() {},
    remove() {},
  });

  globalThis.window = {
    AFRAME: {},
    ARjs: {},
    addEventListener(type) {
      addedEvents.push(type);
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

  const root = createElement();
  const controller = new CameraController({
    root,
    markers: [],
    onCameraReady() {},
    onCameraError() {},
  });

  controller.start();
  assert.deepEqual(addedEvents, ["camera-init", "camera-error"]);
  assert.equal(documentSubscriptions, 0);

  controller.stop();
  assert.deepEqual(removedEvents, ["camera-init", "camera-error"]);
});
