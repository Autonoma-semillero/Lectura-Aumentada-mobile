import test from "node:test";
import assert from "node:assert/strict";
import { FloatSceneController, hasWebGlSupport } from "../src/js/float-scene-controller.js";

function createFakeElement(tagName) {
  return {
    tagName,
    attributes: new Map(),
    children: [],
    setAttribute(name, value) {
      this.attributes.set(name, value);
    },
    appendChild(child) {
      this.children.push(child);
      child.parentElement = this;
      return child;
    },
    prepend(child) {
      this.children.unshift(child);
      child.parentElement = this;
      return child;
    },
    remove() {
      this.parentElement = null;
    },
  };
}

async function withFakeDom({ webglSupported = true } = {}, run) {
  const previousDocument = globalThis.document;
  const previousWindow = globalThis.window;
  globalThis.document = {
    createElement: (tagName) =>
      tagName === "canvas"
        ? { getContext: (kind) => (webglSupported && kind === "webgl" ? {} : null) }
        : createFakeElement(tagName),
  };
  globalThis.window = { AFRAME: {} };
  try {
    return await run();
  } finally {
    globalThis.document = previousDocument;
    globalThis.window = previousWindow;
  }
}

test("hasWebGlSupport reflects whether canvas can produce a webgl context", async () => {
  await withFakeDom({ webglSupported: true }, () => assert.equal(hasWebGlSupport(), true));
  await withFakeDom({ webglSupported: false }, () => assert.equal(hasWebGlSupport(), false));
});

test("start rejects when WebGL is unavailable, without touching the camera", async () => {
  await withFakeDom({ webglSupported: false }, async () => {
    const root = createFakeElement("div");
    const controller = new FloatSceneController({ root });

    await assert.rejects(controller.start(), /WebGL/);
    assert.equal(root.children.length, 0);
  });
});

test("start mounts a camera-relative card root fed by the device camera stream", async () => {
  await withFakeDom({ webglSupported: true }, async () => {
    const previousNavigator = globalThis.navigator;
    const tracks = [{ stop: () => {}, stopped: false }];
    Object.defineProperty(globalThis, "navigator", {
      configurable: true,
      value: {
        mediaDevices: {
          getUserMedia: async () => ({
            getTracks: () => tracks,
          }),
        },
      },
    });

    const root = createFakeElement("div");
    let ready = false;
    const controller = new FloatSceneController({ root, onCameraReady: () => (ready = true) });

    const cardRoot = await controller.start();

    assert.equal(ready, true);
    assert.equal(cardRoot.tagName, "a-entity");
    assert.equal(cardRoot.parentElement.tagName, "a-entity");
    assert.equal(root.children.some((child) => child.tagName === "a-scene"), true);

    controller.stop();
    assert.equal(controller.scene, null);

    Object.defineProperty(globalThis, "navigator", { configurable: true, value: previousNavigator });
  });
});
