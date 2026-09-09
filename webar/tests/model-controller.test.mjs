import test from "node:test";
import assert from "node:assert/strict";
import { ModelController } from "../src/js/model-controller.js";

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
