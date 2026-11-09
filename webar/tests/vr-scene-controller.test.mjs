import test from "node:test";
import assert from "node:assert/strict";
import { VrSceneController } from "../src/js/vr-scene-controller.js";

function element(tagName) {
  return {
    tagName,
    children: [],
    attributes: new Map(),
    setAttribute(name, value) { this.attributes.set(name, value); },
    listeners: new Map(),
    addEventListener(name, listener) { this.listeners.set(name, listener); },
    appendChild(child) { this.children.push(child); child.parentElement = this; return child; },
    remove() { this.parentElement = null; },
    async play() {},
    pause() {},
  };
}

test("stereo view creates two synchronized eyes from one camera stream", async () => {
  const previousDocument = globalThis.document;
  const previousWindow = globalThis.window;
  const previousNavigator = globalThis.navigator;
  const track = { stopped: false, stop() { this.stopped = true; } };
  let cameraRequests = 0;
  globalThis.document = {
    createElement: (tagName) => tagName === "canvas"
      ? { getContext: (kind) => kind === "webgl" ? {} : null }
      : element(tagName),
  };
  globalThis.window = { AFRAME: {} };
  Object.defineProperty(globalThis, "navigator", {
    configurable: true,
    value: { mediaDevices: { getUserMedia: async () => { cameraRequests += 1; return { getTracks: () => [track] }; } } },
  });

  try {
    const root = element("div");
    const controller = new VrSceneController({ root });
    const cardRoots = await controller.start();

    assert.equal(cameraRequests, 1);
    assert.equal(root.children.length, 2);
    assert.equal(cardRoots.length, 2);
    assert.equal(controller.eyes[0].camera.attributes.get("position"), "-0.032 0 0");
    assert.equal(controller.eyes[1].camera.attributes.get("position"), "0.032 0 0");

    const clearCalls = [];
    const firstScene = controller.eyes[0].scene;
    firstScene.object3D = { background: "opaque" };
    firstScene.canvas = { style: {} };
    firstScene.renderer = { setClearColor(color, alpha) { clearCalls.push([color, alpha]); } };
    firstScene.listeners.get("renderstart")();
    assert.deepEqual(clearCalls, [[0x000000, 0]]);
    assert.equal(firstScene.object3D.background, null);
    assert.equal(firstScene.canvas.style.backgroundColor, "transparent");

    for (const eye of controller.eyes) eye.camera.object3D = { quaternion: { set(x, y, z, w) { Object.assign(this, { x, y, z, w }); } } };
    controller.setCameraQuaternion(0.1, 0.2, 0.3, 0.9);
    assert.equal(controller.eyes[0].camera.object3D.quaternion.w, 0.9);
    assert.equal(controller.eyes[1].camera.object3D.quaternion.w, 0.9);

    controller.stop();
    assert.equal(track.stopped, true);
    assert.equal(controller.eyes.length, 0);
  } finally {
    globalThis.document = previousDocument;
    globalThis.window = previousWindow;
    Object.defineProperty(globalThis, "navigator", { configurable: true, value: previousNavigator });
  }
});
