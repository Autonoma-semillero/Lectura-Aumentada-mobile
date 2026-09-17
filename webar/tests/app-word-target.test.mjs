import test from "node:test";
import assert from "node:assert/strict";

test("keeps a fuzzy asset on its explicit OCR target while the word moves", async (context) => {
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
    constructor() {
      this.src = "";
      this.playCount = 0;
      FakeAudio.instance = this;
    }
    addEventListener() {}
    pause() {}
    removeAttribute(name) { if (name === "src") this.src = ""; }
    load() {}
    play() {
      this.playCount += 1;
      return Promise.resolve();
    }
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
    target: { type: "word", word: "pato", centerX: 0.4, centerY: 0.6 },
    asset: {
      id: "asset-1",
      learningUnitId: "unit-1",
      markerId: "demo-animales-gato",
      word: "gato",
      model3dUrl: "https://cdn.example.test/gato.glb",
      audioUrl: "https://cdn.example.test/gato.mp3",
    },
  });

  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");
  assert.equal(elementFor("#main-status").textContent, "Mostrando gato");
  assert.equal(elementFor("#model-state").textContent, "sin modelo");
  assert.equal(FakeAudio.instance.src, "https://cdn.example.test/gato.mp3");
  assert.equal(FakeAudio.instance.playCount, 1);

  window.WebAR.receiveNativeMessage({
    type: "word-not-found",
    word: "casa",
    target: { type: "word", word: "casa", centerX: 0.15, centerY: 0.2 },
  });

  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");
  assert.equal(elementFor("#main-status").textContent, "Mostrando gato");
  assert.equal(FakeAudio.instance.src, "https://cdn.example.test/gato.mp3");
  assert.equal(FakeAudio.instance.playCount, 1);

  window.WebAR.receiveNativeMessage({
    type: "asset-ready",
    target: { type: "word", word: "casa", centerX: 0.2, centerY: 0.3 },
    asset: {
      id: "asset-stale",
      learningUnitId: "unit-stale",
      markerId: "demo-casa",
      word: "casa",
      model3dUrl: "https://cdn.example.test/casa.glb",
      audioUrl: "https://cdn.example.test/casa.mp3",
    },
  });

  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");
  assert.equal(elementFor("#main-status").textContent, "Mostrando gato");
  assert.equal(elementFor("#model-state").textContent, "sin modelo");
  assert.equal(FakeAudio.instance.src, "https://cdn.example.test/gato.mp3");
  assert.equal(FakeAudio.instance.playCount, 1);

  window.WebAR.receiveNativeMessage({
    type: "activate-word-target",
    word: "pato",
    centerX: 0.58,
    centerY: 0.63,
  });
  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");
  assert.equal(elementFor("#model-state").textContent, "sin modelo");

  window.WebAR.receiveNativeMessage({
    type: "marker-not-found",
    markerId: "demo-animales-gato",
  });
  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");

  window.WebAR.receiveNativeMessage({ type: "word-not-found", word: "casa" });
  assert.equal(elementFor("#marker-state").textContent, "palabra: pato");

  window.WebAR.receiveNativeMessage({ type: "word-not-found", word: "pato" });
  assert.equal(
    elementFor("#main-status").textContent,
    "La palabra no tiene contenido asociado"
  );

  window.WebAR.receiveNativeMessage({ type: "clear-word-target", word: "pato" });
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
