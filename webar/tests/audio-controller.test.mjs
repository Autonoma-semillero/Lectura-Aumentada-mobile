import test from "node:test";
import assert from "node:assert/strict";
import { AudioController } from "../src/js/audio-controller.js";

const CAT_AUDIO_URL =
  "https://appassets.androidplatform.net/assets/audio/animals/gato.mp3";

function createHarness({ rejectPlay = false, playImplementation } = {}) {
  const previousAudio = globalThis.Audio;
  const audioListeners = new Map();
  const buttonListeners = new Map();
  const classes = new Set(["hidden"]);
  const states = [];
  let focusCount = 0;

  class FakeAudio {
    constructor() {
      this.src = "";
      this.currentTime = 8;
      this.loadCount = 0;
      this.pauseCount = 0;
      this.playCount = 0;
      this.listeners = new Map();
      FakeAudio.instances.push(this);
    }

    addEventListener(type, listener) {
      this.listeners.set(type, listener);
      audioListeners.set(type, listener);
    }

    removeEventListener(type, listener) {
      if (this.listeners.get(type) === listener) this.listeners.delete(type);
    }

    pause() {
      this.pauseCount += 1;
    }

    removeAttribute(name) {
      if (name === "src") this.src = "";
    }

    load() {
      this.loadCount += 1;
    }

    play() {
      this.playCount += 1;
      if (playImplementation) return playImplementation(this);
      return rejectPlay ? Promise.reject(new Error("blocked")) : Promise.resolve();
    }
  }
  FakeAudio.instances = [];

  const repeatButton = {
    classList: {
      add(name) { classes.add(name); },
      remove(name) { classes.delete(name); },
    },
    addEventListener(type, listener) {
      buttonListeners.set(type, listener);
    },
    focus() {
      focusCount += 1;
    },
  };

  globalThis.Audio = FakeAudio;
  const controller = new AudioController({
    repeatButton,
    onStateChange: (state) => states.push(state),
  });

  return {
    get audio() { return FakeAudio.instances.at(-1); },
    get audios() { return FakeAudio.instances; },
    audioListeners,
    buttonListeners,
    classes,
    controller,
    states,
    get focusCount() { return focusCount; },
    restore() { globalThis.Audio = previousAudio; },
  };
}

test("loads, plays and exposes repeat for the bundled cat audio", async () => {
  const harness = createHarness();
  try {
    await harness.controller.loadAndPlay(CAT_AUDIO_URL);

    assert.equal(harness.audio.src, CAT_AUDIO_URL);
    assert.equal(harness.audio.loadCount, 1);
    assert.equal(harness.audio.playCount, 1);
    assert.equal(harness.audio.currentTime, 0);
    assert.equal(harness.classes.has("hidden"), false);
    assert.deepEqual(harness.states.slice(-2), ["sin contenido", "cargando"]);

    harness.buttonListeners.get("click")();
    await Promise.resolve();
    assert.equal(harness.audio.playCount, 2);
  } finally {
    harness.restore();
  }
});

test("keeps playback absent and the repeat control hidden without a URL", async () => {
  const harness = createHarness();
  try {
    await harness.controller.loadAndPlay(undefined);

    assert.equal(harness.audios.length, 0);
    assert.equal(harness.classes.has("hidden"), true);
    assert.equal(harness.states.at(-1), "sin audio");
  } finally {
    harness.restore();
  }
});

test("reports media load errors", async () => {
  const harness = createHarness();
  try {
    await harness.controller.loadAndPlay(CAT_AUDIO_URL);
    harness.audioListeners.get("error")();

    assert.equal(harness.states.at(-1), "error de carga");
  } finally {
    harness.restore();
  }
});

test("offers the repeat control when automatic playback is rejected", async () => {
  const harness = createHarness({ rejectPlay: true });
  try {
    await harness.controller.loadAndPlay(CAT_AUDIO_URL);

    assert.equal(harness.states.at(-1), "toca «Repetir audio»");
    assert.equal(harness.focusCount, 1);
    assert.equal(harness.classes.has("hidden"), false);
  } finally {
    harness.restore();
  }
});

test("treats audio unlock failure as nonfatal for camera activation", async () => {
  const previousWindow = globalThis.window;
  const harness = createHarness();
  globalThis.window = {
    AudioContext: class {
      resume() { return Promise.reject(new Error("audio unavailable")); }
    },
  };
  try {
    await assert.doesNotReject(() => harness.controller.unlock());
    assert.equal(await harness.controller.unlock(), false);
  } finally {
    globalThis.window = previousWindow;
    harness.restore();
  }
});

test("ignores stale playback rejection and media events after loading a new source", async () => {
  let rejectFirstPlay;
  const firstPlay = new Promise((_, reject) => { rejectFirstPlay = reject; });
  const harness = createHarness({
    playImplementation: (audio) =>
      harness.audios.indexOf(audio) === 0 ? firstPlay : Promise.resolve(),
  });
  try {
    const firstLoad = harness.controller.loadAndPlay(
      "https://appassets.androidplatform.net/assets/audio/animals/old.mp3"
    );
    await Promise.resolve();
    const oldAudio = harness.audio;
    const oldErrorListener = oldAudio.listeners.get("error");

    await harness.controller.loadAndPlay(CAT_AUDIO_URL);
    const currentAudio = harness.audio;
    const stateAfterCurrentLoad = harness.states.at(-1);

    rejectFirstPlay(new Error("old source aborted"));
    await firstLoad;
    oldErrorListener();

    assert.notEqual(currentAudio, oldAudio);
    assert.equal(currentAudio.src, CAT_AUDIO_URL);
    assert.equal(harness.states.at(-1), stateAfterCurrentLoad);
    assert.equal(harness.focusCount, 0);
  } finally {
    harness.restore();
  }
});

test("ignores pending playback and stale events after clear", async () => {
  let rejectPlay;
  const pendingPlay = new Promise((_, reject) => { rejectPlay = reject; });
  const harness = createHarness({ playImplementation: () => pendingPlay });
  try {
    const loading = harness.controller.loadAndPlay(CAT_AUDIO_URL);
    await Promise.resolve();
    const oldAudio = harness.audio;
    const oldPlayingListener = oldAudio.listeners.get("playing");

    harness.controller.clear();
    const stateAfterClear = harness.states.at(-1);
    rejectPlay(new Error("cleared"));
    await loading;
    oldPlayingListener();

    assert.equal(harness.controller.audio, null);
    assert.equal(harness.states.at(-1), stateAfterClear);
    assert.equal(harness.classes.has("hidden"), true);
    assert.equal(harness.focusCount, 0);
  } finally {
    harness.restore();
  }
});
