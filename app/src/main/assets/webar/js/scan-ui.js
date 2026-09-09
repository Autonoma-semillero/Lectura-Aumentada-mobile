export function beginOcrScanning(panel, notifyCameraReady) {
  setScanDiagnosticsVisible(panel, false);
  notifyCameraReady();
}

export function endOcrScanning(panel) {
  setScanDiagnosticsVisible(panel, true);
}

function setScanDiagnosticsVisible(panel, visible) {
  if (!panel) return;

  panel.classList.toggle("hidden", !visible);
  if (visible) {
    panel.removeAttribute("aria-hidden");
  } else {
    panel.setAttribute("aria-hidden", "true");
  }
}
