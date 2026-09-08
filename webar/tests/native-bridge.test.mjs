import test from "node:test";
import assert from "node:assert/strict";
import { parseAsset, parseNativeMessage } from "../src/js/native-bridge.js";

test("accepts the typed asset contract returned by the Android host", () => {
  const asset = parseAsset({
    id: "asset-1",
    learningUnitId: "unit-1",
    markerId: "demo-animales-gato",
    word: "gato",
    model3dUrl: "https://cdn.example.test/gato.glb",
    audioUrl: "https://cdn.example.test/gato.mp3",
  });

  assert.equal(asset.markerId, "demo-animales-gato");
  assert.equal(asset.word, "gato");
});

test("rejects executable or unencrypted asset URLs", () => {
  for (const model3dUrl of ["javascript:alert(1)", "http://cdn.example.test/gato.glb"]) {
    assert.throws(() => parseAsset({
      id: "asset-1",
      learningUnitId: "unit-1",
      markerId: "demo-animales-gato",
      word: "gato",
      model3dUrl,
    }));
  }
});

test("requires a message type", () => {
  assert.throws(() => parseNativeMessage("{}"));
});
