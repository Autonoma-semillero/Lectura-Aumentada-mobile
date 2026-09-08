import test from "node:test";
import assert from "node:assert/strict";
import { MarkerGate } from "../src/js/marker-gate.js";

test("deduplicates consecutive detections of the same marker", () => {
  const gate = new MarkerGate();

  assert.deepEqual(gate.found("demo-animales-gato"), {
    markerId: "demo-animales-gato",
    previousMarkerId: null,
  });
  assert.equal(gate.found("demo-animales-gato"), false);
});

test("only clears the marker that is currently active", () => {
  const gate = new MarkerGate();
  gate.found("demo-animales-gato");

  assert.equal(gate.lost("demo-animales-perro"), false);
  assert.deepEqual(gate.lost("demo-animales-gato"), { markerId: "demo-animales-gato" });
  assert.equal(gate.activeMarkerId, null);
});

test("reports the previous marker when the active target changes", () => {
  const gate = new MarkerGate();
  gate.found("demo-animales-gato");

  assert.deepEqual(gate.found("demo-animales-perro"), {
    markerId: "demo-animales-perro",
    previousMarkerId: "demo-animales-gato",
  });
});
