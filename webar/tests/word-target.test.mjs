import test from "node:test";
import assert from "node:assert/strict";
import {
  WORD_TARGET_MODEL_ID,
  WordTargetGate,
  normalizeWord,
  resolveAssetTarget,
} from "../src/js/word-target.js";

test("normalizes case, accents and repeated whitespace for OCR targets", () => {
  assert.equal(normalizeWord("  ÁRbol  "), "arbol");
  assert.equal(normalizeWord("Árbol   grande"), "arbol grande");
  assert.equal(normalizeWord("NIÑO"), "niño");
  assert.notEqual(normalizeWord("niño"), normalizeWord("nino"));
});

test("matches the backend canonical form for OCR separators and Unicode letters", () => {
  assert.equal(normalizeWord("  CO–OPERAR  "), "co-operar");
  assert.equal(normalizeWord("L’ARBRE"), "l'arbre");
  assert.equal(normalizeWord("NIN\u0303O"), "niño");
  assert.equal(normalizeWord("ＡＲＢＯＬ"), "arbol");
  assert.equal(normalizeWord(" ¡ÁRBOL! "), "arbol");
  assert.equal(normalizeWord("東京"), "東京");
});

test("deduplicates activations and ignores a stale clear", () => {
  const gate = new WordTargetGate();

  assert.deepEqual(gate.activate("Árbol"), {
    word: "Árbol",
    normalizedWord: "arbol",
    centerX: 0.5,
    centerY: 0.5,
    previousTarget: null,
    wordChanged: true,
    positionChanged: true,
  });
  assert.equal(gate.activate("ARBOL"), false);
  const moved = gate.activate("ARBOL", { centerX: 1.4, centerY: -0.2 });
  assert.equal(moved.wordChanged, false);
  assert.equal(moved.positionChanged, true);
  assert.equal(moved.centerX, 1);
  assert.equal(moved.centerY, 0);
  assert.equal(gate.clear("casa"), false);
  assert.equal(gate.activeTarget.normalizedWord, "arbol");
  assert.equal(gate.clear("arbol").normalizedWord, "arbol");
  assert.equal(gate.activeTarget, null);
});

test("rejects non-numeric OCR coordinates", () => {
  const gate = new WordTargetGate();
  assert.throws(() => gate.activate("árbol", { centerX: "0.5" }), TypeError);
  assert.throws(() => gate.activate("árbol", { centerY: Number.NaN }), TypeError);
});

test("routes assets to the camera root while a matching word is active", () => {
  const asset = { markerId: "demo-animales-gato", word: "árbol" };

  assert.equal(resolveAssetTarget({
    asset,
    activeWordTarget: { normalizedWord: "arbol" },
    activeMarkerId: "demo-animales-gato",
  }), WORD_TARGET_MODEL_ID);
  assert.equal(resolveAssetTarget({
    asset: { ...asset, word: "casa" },
    activeWordTarget: { normalizedWord: "arbol" },
    activeMarkerId: "demo-animales-gato",
  }), null);
});

test("keeps marker routing when no word target is active", () => {
  assert.equal(resolveAssetTarget({
    asset: { markerId: "demo-animales-gato", word: "gato" },
    activeWordTarget: null,
    activeMarkerId: "demo-animales-gato",
  }), "demo-animales-gato");
});
