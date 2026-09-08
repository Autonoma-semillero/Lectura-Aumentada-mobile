export class MarkerGate {
  #activeMarkerId = null;

  get activeMarkerId() {
    return this.#activeMarkerId;
  }

  found(markerId) {
    if (!markerId || markerId === this.#activeMarkerId) return false;
    const previousMarkerId = this.#activeMarkerId;
    this.#activeMarkerId = markerId;
    return { markerId, previousMarkerId };
  }

  lost(markerId) {
    if (!markerId || markerId !== this.#activeMarkerId) return false;
    this.#activeMarkerId = null;
    return { markerId };
  }

  reset() {
    const markerId = this.#activeMarkerId;
    this.#activeMarkerId = null;
    return markerId;
  }
}
