import test from "node:test";
import assert from "node:assert/strict";
import { beginOcrScanning, endOcrScanning } from "../src/js/scan-ui.js";

test("hides WebAR diagnostics during OCR scanning and restores them afterwards", () => {
  const classes = new Set(["debug-panel"]);
  const attributes = new Map([["aria-live", "polite"]]);
  const panel = {
    classList: {
      toggle(name, enabled) {
        if (enabled) classes.add(name);
        else classes.delete(name);
      },
    },
    setAttribute(name, value) {
      attributes.set(name, value);
    },
    removeAttribute(name) {
      attributes.delete(name);
    },
  };

  let panelWasHiddenWhenCameraReady = false;
  beginOcrScanning(panel, () => {
    panelWasHiddenWhenCameraReady = classes.has("hidden");
  });
  assert.equal(classes.has("hidden"), true);
  assert.equal(attributes.get("aria-hidden"), "true");
  assert.equal(panelWasHiddenWhenCameraReady, true);

  endOcrScanning(panel);
  assert.equal(classes.has("hidden"), false);
  assert.equal(attributes.has("aria-hidden"), false);
  assert.equal(attributes.get("aria-live"), "polite");
});
