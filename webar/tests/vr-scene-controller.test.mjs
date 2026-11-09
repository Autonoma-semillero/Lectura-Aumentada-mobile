import test from "node:test";
import assert from "node:assert/strict";
import { VrSceneController } from "../src/js/vr-scene-controller.js";

function element(tagName, renderer) {
  const cameraObject = {};
  return {
    tagName, children: [], attributes: new Map(), renderer: tagName === "a-scene" ? renderer : undefined,
    canvas: tagName === "a-scene" ? { style: {} } : undefined,
    clientWidth: 200, clientHeight: 100,
    object3D: { quaternion: { set(x, y, z, w) { Object.assign(this, { x, y, z, w }); } }, add(child) { child.parent = this; }, remove() {} },
    setAttribute(name, value) { this.attributes.set(name, value); },
    addEventListener() {},
    appendChild(child) { this.children.push(child); child.parentElement = this; return child; },
    getObject3D(name) { return name === "camera" ? cameraObject : null; },
    getContext() { return { drawImage() {} }; },
    remove() { this.parentElement = null; }, async play() {}, pause() {},
  };
}

test("stereo view uses one stream, scene, renderer and card root", async () => {
  const previousDocument = globalThis.document;
  const previousWindow = globalThis.window;
  const previousNavigator = globalThis.navigator;
  const previousRequestAnimationFrame = globalThis.requestAnimationFrame;
  const previousCancelAnimationFrame = globalThis.cancelAnimationFrame;
  const track = { stopped: false, stop() { this.stopped = true; } };
  let cameraRequests = 0;
  const renderer = {
    render() {}, setScissorTest() {}, setViewport() {}, setScissor() {}, clear() {}, setClearColor() {},
    getSize(target) { target.x = 200; target.y = 100; return target; },
  };
  class StereoCamera { constructor() { this.cameraL = { layers: { enable() {} } }; this.cameraR = { layers: { enable() {} } }; } update() {} }
  class Vector2 {}
  class Quaternion {
    constructor(x = 0, y = 0, z = 0, w = 1) { Object.assign(this, { x, y, z, w }); }
    normalize() { return this; } clone() { return new Quaternion(this.x, this.y, this.z, this.w); }
    invert() { this.x *= -1; this.y *= -1; this.z *= -1; return this; }
    multiply() { return this; }
  }
  globalThis.document = { createElement: (tag) => tag === "canvas" ? { ...element(tag, renderer), getContext: () => ({ drawImage() {} }) } : element(tag, renderer) };
  globalThis.window = { location: { search: "" }, AFRAME: { THREE: { StereoCamera, Vector2, Quaternion } } };
  globalThis.requestAnimationFrame = () => 1;
  globalThis.cancelAnimationFrame = () => {};
  Object.defineProperty(globalThis, "navigator", { configurable: true, value: { mediaDevices: { getUserMedia: async () => { cameraRequests += 1; return { getTracks: () => [track] }; } } } });

  try {
    const root = element("div", renderer);
    const controller = new VrSceneController({ root });
    const cardRoot = await controller.start();
    assert.equal(cameraRequests, 1);
    assert.equal(root.children.length, 3);
    assert.ok(controller.video);
    assert.ok(controller.cameraCanvas);
    assert.equal(cardRoot.attributes.get("id"), "vr-card-root");
    assert.equal(controller.scene.renderer, renderer);
    controller.camera.object3D.quaternion.copy = function (value) { Object.assign(this, value); };
    controller.setCameraQuaternion(0.1, 0.2, 0.3, 0.9);
    assert.ok(controller.initialOrientation);
    controller.stop();
    assert.equal(track.stopped, true);
  } finally {
    globalThis.document = previousDocument;
    globalThis.window = previousWindow;
    globalThis.requestAnimationFrame = previousRequestAnimationFrame;
    globalThis.cancelAnimationFrame = previousCancelAnimationFrame;
    Object.defineProperty(globalThis, "navigator", { configurable: true, value: previousNavigator });
  }
});
