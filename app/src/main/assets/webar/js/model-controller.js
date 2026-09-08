export class ModelController {
  constructor(onStateChange) {
    this.onStateChange = onStateChange;
    this.rootsByMarker = new Map();
    this.activeRoot = null;
    this.onLoaded = null;
    this.onError = null;
  }

  registerMarker(markerId, root) {
    this.rootsByMarker.set(markerId, root);
  }

  show(markerId, modelUrl) {
    this.clear();
    const root = this.rootsByMarker.get(markerId);
    if (!root || !modelUrl) {
      this.onStateChange("sin modelo");
      return;
    }
    this.activeRoot = root;
    this.onLoaded = () => this.onStateChange("visible");
    this.onError = () => {
      this.onStateChange("error de carga");
      this.#removeModel(root);
    };
    root.addEventListener("model-loaded", this.onLoaded, { once: true });
    root.addEventListener("model-error", this.onError, { once: true });
    this.onStateChange("cargando");
    root.setAttribute("gltf-model", modelUrl);
  }

  clear() {
    if (this.activeRoot) this.#removeModel(this.activeRoot);
    this.activeRoot = null;
    this.onStateChange("sin contenido");
  }

  destroy() {
    this.clear();
    this.rootsByMarker.clear();
  }

  #removeModel(root) {
    if (this.onLoaded) root.removeEventListener("model-loaded", this.onLoaded);
    if (this.onError) root.removeEventListener("model-error", this.onError);
    this.onLoaded = null;
    this.onError = null;
    const object = root.getObject3D?.("mesh");
    object?.traverse?.((node) => {
      node.geometry?.dispose?.();
      const materials = Array.isArray(node.material) ? node.material : [node.material];
      for (const material of materials.filter(Boolean)) {
        for (const value of Object.values(material)) value?.isTexture && value.dispose?.();
        material.dispose?.();
      }
    });
    root.removeAttribute("gltf-model");
    root.removeObject3D?.("mesh");
  }
}
