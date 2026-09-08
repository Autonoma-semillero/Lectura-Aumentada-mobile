import { MarkerGate } from "./marker-gate.js";

export class DetectionController {
  constructor({ onFound, onLost, lossGraceMs = 450 }) {
    this.onFound = onFound;
    this.onLost = onLost;
    this.lossGraceMs = lossGraceMs;
    this.gate = new MarkerGate();
    this.listeners = [];
    this.lossTimers = new Map();
  }

  attach(markerEntries) {
    this.detach();
    for (const { markerId, element } of markerEntries) {
      const found = () => {
        this.#cancelPendingLoss(markerId);
        const transition = this.gate.found(markerId);
        if (!transition) return;
        if (transition.previousMarkerId) this.onLost(transition.previousMarkerId);
        this.onFound(markerId);
      };
      const lost = () => {
        this.#cancelPendingLoss(markerId);
        const timer = window.setTimeout(() => {
          this.lossTimers.delete(markerId);
          if (this.gate.lost(markerId)) this.onLost(markerId);
        }, this.lossGraceMs);
        this.lossTimers.set(markerId, timer);
      };
      element.addEventListener("markerFound", found);
      element.addEventListener("markerLost", lost);
      this.listeners.push({ element, found, lost });
    }
  }

  detach() {
    for (const { element, found, lost } of this.listeners) {
      element.removeEventListener("markerFound", found);
      element.removeEventListener("markerLost", lost);
    }
    this.listeners = [];
    for (const timer of this.lossTimers.values()) window.clearTimeout(timer);
    this.lossTimers.clear();
    const markerId = this.gate.reset();
    if (markerId) this.onLost(markerId);
  }

  #cancelPendingLoss(markerId) {
    const timer = this.lossTimers.get(markerId);
    if (timer !== undefined) window.clearTimeout(timer);
    this.lossTimers.delete(markerId);
  }
}
