import test from "node:test";
import assert from "node:assert/strict";
import * as THREE from "three";
import {
  ModelController,
  calculateWordModelTransform,
} from "../src/js/model-controller.js";

test("fits arbitrary word-model bounds while keeping their base on the anchor", () => {
  const transform = calculateWordModelTransform({
    min: { x: -2, y: 10, z: -1 },
    max: { x: 4, y: 12, z: 5 },
  });

  assert.ok(transform);
  assert.equal(transform.scale, 0.075);
  assert.deepEqual(transform.position, { x: -0.075, y: -0.75, z: -0.15 });
  assert.equal((12 - 10) * transform.scale, 0.15);
  assert.ok(Math.abs((4 - (-2)) * transform.scale - 0.45) < Number.EPSILON);
  assert.equal(10 * transform.scale + transform.position.y, 0);
  assert.equal(calculateWordModelTransform({
    min: { x: 1, y: 1, z: 1 },
    max: { x: 1, y: 1, z: 1 },
  }), null);
});

test("applies the fitted transform to a loaded Three.js word model", (context) => {
  const previousThree = globalThis.THREE;
  globalThis.THREE = THREE;
  context.after(() => {
    globalThis.THREE = previousThree;
  });

  const listeners = new Map();
  const attributes = new Map();
  const gltfRoot = new THREE.Object3D();
  const geometry = new THREE.BoxGeometry(6, 2, 6);
  geometry.translate(1, 11, 2);
  gltfRoot.add(new THREE.Mesh(geometry));
  const model = {
    object3D: new THREE.Object3D(),
    addEventListener(type, listener) { listeners.set(type, listener); },
    removeEventListener(type, listener) {
      if (listeners.get(type) === listener) listeners.delete(type);
    },
    getObject3D(name) { return name === "mesh" ? gltfRoot : null; },
    removeObject3D() { this.object3D.remove(gltfRoot); },
    setAttribute(name, value) {
      attributes.set(name, value);
      if (name === "scale") this.object3D.scale.set(value.x, value.y, value.z);
      if (name === "position") this.object3D.position.set(value.x, value.y, value.z);
    },
    removeAttribute(name) { attributes.delete(name); },
    remove() { this.object3D.removeFromParent(); },
  };
  model.object3D.add(gltfRoot);

  const anchor = {
    dataset: { targetType: "word" },
    object3D: new THREE.Object3D(),
    ownerDocument: { createElement: () => model },
    appendChild(child) { this.object3D.add(child.object3D); },
  };
  anchor.object3D.position.set(-0.026, 0.156, -2);
  const controller = new ModelController(() => {});
  controller.registerMarker("word", anchor);
  controller.show("word", "https://cdn.example.test/word.glb", "asset-word");

  listeners.get("model-loaded")();

  assert.deepEqual(attributes.get("scale"), { x: 0.075, y: 0.075, z: 0.075 });
  assert.deepEqual(attributes.get("position"), { x: -0.075, y: -0.75, z: -0.15 });
  anchor.object3D.updateWorldMatrix(true, true);
  const fittedBounds = new THREE.Box3().setFromObject(gltfRoot);
  const fittedSize = fittedBounds.getSize(new THREE.Vector3());
  assert.ok(
    Math.abs(Math.max(fittedSize.x, fittedSize.y, fittedSize.z) - 0.45) < 1e-6,
    `unexpected fitted size ${fittedSize.toArray()}`
  );
  assert.ok(Math.abs(fittedBounds.min.y - anchor.object3D.position.y) < 1e-6);
});

test("isolates model loads and reports readiness or load errors", (context) => {
  const previousDocument = globalThis.document;
  const states = [];
  const readyAssetIds = [];
  const errors = [];
  const createdModels = [];

  const createModel = () => {
    const listeners = new Map();
    const attributes = new Map();
    const model = {
      listeners,
      attributes,
      parentElement: null,
      addEventListener(type, listener) {
        listeners.set(type, listener);
      },
      removeEventListener(type, listener) {
        if (listeners.get(type) === listener) listeners.delete(type);
      },
      setAttribute(name, value) {
        attributes.set(name, value);
      },
      removeAttribute(name) {
        attributes.delete(name);
      },
      getObject3D() {
        return null;
      },
      removeObject3D() {},
      remove() {
        if (!this.parentElement) return;
        const index = this.parentElement.children.indexOf(this);
        if (index >= 0) this.parentElement.children.splice(index, 1);
        this.parentElement = null;
      },
      emit(type) {
        listeners.get(type)?.();
      },
    };
    createdModels.push(model);
    return model;
  };
  globalThis.document = { createElement: createModel };
  context.after(() => {
    globalThis.document = previousDocument;
  });

  const root = {
    dataset: { targetType: "word" },
    children: [],
    appendChild(child) {
      this.children.push(child);
      child.parentElement = this;
    },
  };
  const controller = new ModelController(
    (state) => states.push(state),
    {
      onModelReady: (assetId) => { readyAssetIds.push(assetId); },
      onModelError: (message, assetId) => errors.push({ message, assetId }),
    }
  );
  controller.registerMarker("word", root);

  controller.show("word", "https://cdn.example.test/word.glb", "asset-a");
  const firstModel = createdModels[0];
  assert.deepEqual(firstModel.attributes.get("scale"), { x: 0.25, y: 0.25, z: 0.25 });
  assert.equal(firstModel.attributes.get("gltf-model"), "https://cdn.example.test/word.glb");
  controller.show("word", "https://cdn.example.test/new-word.glb", "asset-b");
  const secondModel = createdModels[1];
  assert.notEqual(secondModel, firstModel);
  assert.deepEqual(root.children, [secondModel]);

  // Request A finishes after B has started. It stays on its detached node and
  // cannot report B ready or replace B under the marker anchor.
  firstModel.emit("model-loaded");
  assert.deepEqual(readyAssetIds, []);
  assert.deepEqual(root.children, [secondModel]);

  secondModel.emit("model-loaded");
  assert.deepEqual(readyAssetIds, ["asset-b"]);
  assert.equal(states.at(-1), "visible");

  controller.show("word", "https://cdn.example.test/broken.glb", "asset-broken");
  const brokenModel = createdModels[2];
  brokenModel.emit("model-error");

  assert.deepEqual(errors, [{
    message: "No fue posible cargar el modelo 3D",
    assetId: "asset-broken",
  }]);
  assert.equal(states.at(-1), "error de carga");
  assert.equal(brokenModel.attributes.has("gltf-model"), false);
  assert.deepEqual(root.children, []);

  controller.show("word", undefined, "asset-without-model");
  assert.deepEqual(errors.at(-1), {
    message: "El contenido no tiene un modelo 3D disponible",
    assetId: "asset-without-model",
  });
  assert.equal(states.at(-1), "sin modelo");

  controller.show("missing-root", "https://cdn.example.test/word.glb", "asset-no-root");
  assert.deepEqual(errors.at(-1), {
    message: "No fue posible preparar el destino del modelo 3D",
    assetId: "asset-no-root",
  });
});
