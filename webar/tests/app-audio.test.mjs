import test from "node:test";
import assert from "node:assert/strict";

test("forwards asset-ready audio to the browser player", async (context) => {
  const previousWindow = globalThis.window;
  const previousDocument = globalThis.document;
  const previousAudio = globalThis.Audio;
  const navigatorDescriptor = Object.getOwnPropertyDescriptor(globalThis, "navigator");
  const elements = new Map();

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
    location: {
      href: "https://appassets.androidplatform.net/assets/webar/index.html?nativeSession=audio-test",
    },
    lecturaAumentada: { postMessage() {} },
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

  await import(`../src/js/app.js?audio-test=${Date.now()}`);
  const audioUrl =
    "https://appassets.androidplatform.net/assets/audio/animals/gato.mp3";

  window.WebAR.receiveNativeMessage({
    type: "asset-ready",
    target: { type: "word", word: "gato", centerX: 0.5, centerY: 0.5 },
    asset: {
      id: "asset-cat",
      learningUnitId: "unit-cat",
      markerId: "demo-animales-gato",
      word: "gato",
      model3dUrl:
        "https://appassets.androidplatform.net/assets/models/animals/animal-cat.glb",
      audioUrl,
    },
  });
  await Promise.resolve();

  assert.equal(FakeAudio.instance.src, audioUrl);
  assert.equal(FakeAudio.instance.playCount, 1);
});
