export class NativeBridge {
  constructor(bridgeName = "lecturaAumentada", sessionId = readNativeSessionId()) {
    this.bridgeName = bridgeName;
    this.sessionId = sessionId;
  }

  send(type, payload = {}) {
    const bridge = window[this.bridgeName];
    if (!bridge || typeof bridge.postMessage !== "function") {
      throw new Error("El puente con la aplicación no está disponible");
    }
    if (!isValidSessionId(this.sessionId)) {
      throw new Error("La sesión del documento AR no es válida");
    }
    // Protocol-owned fields go last so a payload cannot spoof another page or event.
    bridge.postMessage(JSON.stringify({ ...payload, type, sessionId: this.sessionId }));
  }
}

function readNativeSessionId() {
  try {
    return new URL(window.location.href).searchParams.get("nativeSession") ?? "";
  } catch {
    return "";
  }
}

function isValidSessionId(value) {
  return typeof value === "string" && /^[A-Za-z0-9_-]{1,128}$/u.test(value);
}

export function parseNativeMessage(rawMessage) {
  const message = typeof rawMessage === "string" ? JSON.parse(rawMessage) : rawMessage;
  if (!message || typeof message.type !== "string") {
    throw new TypeError("Mensaje nativo inválido");
  }
  return message;
}

export function parseAsset(rawAsset) {
  if (!rawAsset || typeof rawAsset !== "object") throw new TypeError("Activo AR inválido");
  for (const key of ["id", "learningUnitId", "markerId", "word"]) {
    if (typeof rawAsset[key] !== "string" || rawAsset[key].trim() === "") {
      throw new TypeError(`El activo AR no contiene ${key}`);
    }
  }
  for (const key of ["model3dUrl", "audioUrl"]) {
    const value = rawAsset[key];
    if (value !== undefined && (typeof value !== "string" || !/^https:\/\//i.test(value))) {
      throw new TypeError(`URL insegura en ${key}`);
    }
  }
  return Object.freeze({ ...rawAsset });
}
