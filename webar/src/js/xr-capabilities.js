// Permanent runtime spike: Android WebView does not expose navigator.xr by
// design (confirmed against the Chromium issue tracker, see
// docs/plan-tarjeta-ar-flotante.md, Revision tecnica independiente, punto 2).
// This probe stays part of the live flow -- not a disposable script -- so the
// floating-card document can keep deciding HUD-3D vs. a future WebXR path
// from a real runtime check instead of a hardcoded assumption.
export async function probeXrCapabilities() {
  if (!navigator.mediaDevices?.getUserMedia) {
    return { camera: false, webxr: false };
  }
  let webxr = false;
  try {
    webxr = Boolean(await navigator.xr?.isSessionSupported?.("immersive-ar"));
  } catch {
    webxr = false;
  }
  return { camera: true, webxr };
}
