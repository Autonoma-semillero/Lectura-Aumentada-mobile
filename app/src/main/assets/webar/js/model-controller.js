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
    this.activeModel = model;
    this.onLoaded = () => {
      if (this.activeModel !== model) return;
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
