import test from "node:test";
import assert from "node:assert/strict";
import { probeXrCapabilities } from "../src/js/xr-capabilities.js";

function withFakeNavigator(fakeNavigator, run) {
  const previousNavigator = globalThis.navigator;
  Object.defineProperty(globalThis, "navigator", { value: fakeNavigator, configurable: true });
  const restore = () => {
    Object.defineProperty(globalThis, "navigator", { value: previousNavigator, configurable: true });
  };
  return Promise.resolve(run()).finally(restore);
}

test("reports no camera and no webxr without getUserMedia", async () => {
  await withFakeNavigator({}, async () => {
    assert.deepEqual(await probeXrCapabilities(), { camera: false, webxr: false });
  });
});

test("reports camera available and webxr unsupported on a plain Android WebView", async () => {
  await withFakeNavigator(
    { mediaDevices: { getUserMedia: async () => ({}) } },
    async () => {
      assert.deepEqual(await probeXrCapabilities(), { camera: true, webxr: false });
    }
  );
});

test("reports webxr as supported only when isSessionSupported resolves true", async () => {
  await withFakeNavigator(
    {
      mediaDevices: { getUserMedia: async () => ({}) },
      xr: { isSessionSupported: async () => true },
    },
    async () => {
      assert.deepEqual(await probeXrCapabilities(), { camera: true, webxr: true });
    }
  );
});

test("treats a throwing isSessionSupported as webxr unsupported, not a crash", async () => {
  await withFakeNavigator(
    {
      mediaDevices: { getUserMedia: async () => ({}) },
      xr: {
        isSessionSupported: async () => {
          throw new Error("boom");
        },
      },
    },
    async () => {
      assert.deepEqual(await probeXrCapabilities(), { camera: true, webxr: false });
    }
  );
});
