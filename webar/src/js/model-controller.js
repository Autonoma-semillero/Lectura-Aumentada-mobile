const WORD_TARGET_TYPE = "word";
const WORD_MODEL_MAX_SIZE = 0.45;
const WORD_MODEL_FALLBACK_SCALE = 0.25;

export class ModelController {
  constructor(onStateChange, { onModelReady = () => {}, onModelError = () => {} } = {}) {
    this.onStateChange = onStateChange;
    this.onModelReady = onModelReady;
    this.onModelError = onModelError;
    this.rootsByMarker = new Map();
    this.activeModel = null;
    this.onLoaded = null;
    this.onError = null;
  }

  registerMarker(markerId, root) {
    this.rootsByMarker.set(markerId, root);
  }

  show(markerId, modelUrl, assetId) {
    this.clear();
    const anchor = this.rootsByMarker.get(markerId);
    if (!anchor || !modelUrl) {
      this.onStateChange("sin modelo");
      this.onModelError(
        anchor
          ? "El contenido no tiene un modelo 3D disponible"
          : "No fue posible preparar el destino del modelo 3D",
        assetId
      );
      return;
    }

    // The A-Frame loader cannot cancel an in-flight GLB request. Isolate every
    // request in a fresh child so a stale completion cannot replace a newer
    // model that targets the same marker anchor.
    const model = anchor.ownerDocument?.createElement?.("a-entity")
      ?? document.createElement("a-entity");
    anchor.appendChild(model);
    const isWordTarget = anchor.dataset?.targetType === WORD_TARGET_TYPE;
    if (isWordTarget) {
      model.setAttribute("scale", uniformScale(WORD_MODEL_FALLBACK_SCALE));
    }
    this.activeModel = model;
    this.onLoaded = () => {
      if (this.activeModel !== model) return;
      if (isWordTarget) this.#fitWordModel(model);
      this.onStateChange("visible");
      this.onModelReady(assetId);
    };
    this.onError = () => {
      if (this.activeModel !== model) return;
      this.onStateChange("error de carga");
      this.#removeModel(model, { watchLateLoad: false });
      this.activeModel = null;
      this.onModelError("No fue posible cargar el modelo 3D", assetId);
    };
    model.addEventListener("model-loaded", this.onLoaded, { once: true });
    model.addEventListener("model-error", this.onError, { once: true });
    this.onStateChange("cargando");
    model.setAttribute("gltf-model", modelUrl);
  }

  #fitWordModel(model) {
    const object = model.getObject3D?.("mesh");
    const Three = globalThis.THREE ?? globalThis.AFRAME?.THREE;
    if (!object || !model.object3D?.worldToLocal || !Three?.Box3 || !Three?.Vector3) return false;

    try {
      model.object3D.updateWorldMatrix?.(true, true);
      object.updateWorldMatrix?.(true, true);
      const worldBounds = new Three.Box3().setFromObject(object);
      if (worldBounds.isEmpty?.()) return false;

      // Convert all eight world-AABB corners back to the model entity. This is
      // conservative even if an ancestor is rotated, so the fitted model stays
      // inside the requested camera-space size instead of being clipped.
      const localBounds = new Three.Box3().makeEmpty();
      for (const x of [worldBounds.min.x, worldBounds.max.x]) {
        for (const y of [worldBounds.min.y, worldBounds.max.y]) {
          for (const z of [worldBounds.min.z, worldBounds.max.z]) {
            localBounds.expandByPoint(
              model.object3D.worldToLocal(new Three.Vector3(x, y, z))
            );
          }
        }
      }

      const transform = calculateWordModelTransform(localBounds);
      if (!transform) return false;
      model.setAttribute("scale", uniformScale(transform.scale));
      model.setAttribute("position", transform.position);
      return true;
    } catch {
      // The known-safe fallback remains applied when malformed geometry cannot
      // produce a finite bounding box.
      return false;
    }
  }

  clear() {
    if (this.activeModel) this.#removeModel(this.activeModel);
    this.activeModel = null;
    this.onStateChange("sin contenido");
  }

  destroy() {
    this.clear();
    this.rootsByMarker.clear();
  }

  #removeModel(model, { watchLateLoad = true } = {}) {
    if (this.onLoaded) model.removeEventListener("model-loaded", this.onLoaded);
    if (this.onError) model.removeEventListener("model-error", this.onError);
    this.onLoaded = null;
    this.onError = null;
    const hadMesh = Boolean(model.getObject3D?.("mesh"));
    this.#disposeMesh(model);

    // Detach before removing the component. A late loader callback may still
    // mutate this orphan, but it can no longer affect the active anchor.
    model.remove?.();
    if (watchLateLoad && !hadMesh) this.#disposeIfLateLoadCompletes(model);
    model.removeAttribute("gltf-model");
  }

  #disposeIfLateLoadCompletes(model) {
    const onLateLoaded = () => {
      model.removeEventListener("model-error", onLateError);
      this.#disposeMesh(model);
    };
    const onLateError = () => {
      model.removeEventListener("model-loaded", onLateLoaded);
    };
    model.addEventListener("model-loaded", onLateLoaded, { once: true });
    model.addEventListener("model-error", onLateError, { once: true });
  }

  #disposeMesh(model) {
    const object = model.getObject3D?.("mesh");
    object?.traverse?.((node) => {
      node.geometry?.dispose?.();
      const materials = Array.isArray(node.material) ? node.material : [node.material];
      for (const material of materials.filter(Boolean)) {
        for (const value of Object.values(material)) value?.isTexture && value.dispose?.();
        material.dispose?.();
      }
    });
    model.removeObject3D?.("mesh");
  }
}

export function calculateWordModelTransform(bounds) {
  const values = [
    bounds?.min?.x,
    bounds?.min?.y,
    bounds?.min?.z,
    bounds?.max?.x,
    bounds?.max?.y,
    bounds?.max?.z,
  ];
  if (!values.every(Number.isFinite)) return null;

  const sizeX = bounds.max.x - bounds.min.x;
  const sizeY = bounds.max.y - bounds.min.y;
  const sizeZ = bounds.max.z - bounds.min.z;
  const maxSize = Math.max(sizeX, sizeY, sizeZ);
  if (!Number.isFinite(maxSize) || maxSize <= 0) return null;

  const scale = WORD_MODEL_MAX_SIZE / maxSize;
  return {
    scale,
    position: {
      x: -((bounds.min.x + bounds.max.x) / 2) * scale,
      // Keep the model's base on the OCR anchor above the detected word.
      y: -bounds.min.y * scale,
      z: -((bounds.min.z + bounds.max.z) / 2) * scale,
    },
  };
}

function uniformScale(value) {
  return { x: value, y: value, z: value };
}
