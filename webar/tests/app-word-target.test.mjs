import test from "node:test";
import assert from "node:assert/strict";

test("atomically activates a word asset and ignores stale marker responses", async (context) => {
  const previousWindow = globalThis.window;
  const previousDocument = globalThis.document;
  const previousAudio = globalThis.Audio;
  const navigatorDescriptor = Object.getOwnPropertyDescriptor(globalThis, "navigator");
  const elements = new Map();
  const bridgeMessages = [];

  const createElement = () => ({
    textContent: "",
    disabled: false,
    listeners: new Map(),
    classList: {
      add() {},
      remove() {},
      toggle() {},
    },
    addEventListener(type, listener) {
      this.listeners.set(type, listener);
    },
    focus() {},
  });
  const elementFor = (selector) => {
    if (!elements.has(selector)) elements.set(selector, createElement());
    return elements.get(selector);
  };

  class FakeAudio {
    addEventListener() {}
    pause() {}
    removeAttribute() {}
    load() {}
    play() { return Promise.resolve(); }
  }

  globalThis.window = {
    addEventListener() {},
    removeEventListener() {},
    location: { href: "https://appassets.androidplatform.net/assets/webar/index.html?nativeSession=test-page" },
    lecturaAumentada: {
      postMessage(message) { bridgeMessages.push(JSON.parse(message)); },
    },
  };
  globalThis.document = {
    visibilityState: "visible",
    querySelector: elementFor,
    querySelectorAll: () => [],
    addEventListener() {},
  };
  globalThis.Audio = FakeAudio;
  Object.defineProperty(globalThis, "navigator", {
    configurable: true,
    value: {},
  });

  context.after(() => {
    globalThis.window = previousWindow;
    globalThis.document = previousDocument;
    globalThis.Audio = previousAudio;
    if (navigatorDescriptor) {
      Object.defineProperty(globalThis, "navigator", navigatorDescriptor);
    } else {
      delete globalThis.navigator;
    }
  });

  await import(`../src/js/app.js?word-target-test=${Date.now()}`);

  window.WebAR.receiveNativeMessage({
    type: "asset-ready",
    target: { type: "word", centerX: 0.4, centerY: 0.6 },
    asset: {
      id: "asset-1",
      learningUnitId: "unit-1",
      markerId: "demo-animales-gato",
      word: "Árbol",
      model3dUrl: "https://cdn.example.test/arbol.glb",
    },
  });

  assert.equal(elementFor("#marker-state").textContent, "palabra: Árbol");
  assert.equal(elementFor("#main-status").textContent, "Mostrando Árbol");

  window.WebAR.receiveNativeMessage({
    type: "marker-not-found",
    markerId: "demo-animales-gato",
  });
  assert.equal(elementFor("#main-status").textContent, "Mostrando Árbol");

  window.WebAR.receiveNativeMessage({ type: "word-not-found", word: "casa" });
  assert.equal(elementFor("#main-status").textContent, "Mostrando Árbol");

  window.WebAR.receiveNativeMessage({ type: "word-not-found", word: "arbol" });
  assert.equal(
    elementFor("#main-status").textContent,
    "La palabra no tiene contenido asociado"
  );

  window.WebAR.receiveNativeMessage({ type: "clear-word-target", word: "arbol" });
  window.WebAR.receiveNativeMessage({
    type: "word-not-found",
    word: "Casa",
    target: { type: "word", centerX: 0.25, centerY: 0.75 },
  });
  assert.equal(elementFor("#marker-state").textContent, "palabra: Casa");
  assert.equal(
    elementFor("#main-status").textContent,
    "La palabra no tiene contenido asociado"
  );

  elementFor("#retry-camera").listeners.get("click")();
  assert.equal(elementFor("#retry-camera").disabled, true);
  assert.equal(bridgeMessages.at(-1).type, "camera-retry-requested");
});
