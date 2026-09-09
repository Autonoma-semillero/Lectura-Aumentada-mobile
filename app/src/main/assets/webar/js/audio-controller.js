export class AudioController {
  constructor({ onStateChange, repeatButton, volume = 0.85 }) {
    this.onStateChange = onStateChange;
    this.repeatButton = repeatButton;
    this.volume = volume;
    this.generation = 0;
    this.audio = null;
    this.audioBinding = null;
    this.repeatButton.addEventListener("click", () => void this.play());
  }

  async unlock() {
    try {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) return true;
      this.context ??= new AudioContextClass();
      await this.context.resume();
      const source = this.context.createBufferSource();
      source.buffer = this.context.createBuffer(1, 1, 22050);
      source.connect(this.context.destination);
      source.start(0);
      return true;
    } catch {
      // Pronunciation is optional and must never prevent the camera from starting.
      return false;
    }
  }

  async loadAndPlay(audioUrl) {
    this.clear();
    if (!audioUrl) {
      this.onStateChange("sin audio");
      return;
    }
    const generation = this.generation;
    const audio = this.#createAudio(generation);
    audio.src = audioUrl;
    audio.load();
    this.repeatButton.classList.remove("hidden");
    this.onStateChange("cargando");
    await this.play(generation, audio);
  }

  async play(generation = this.generation, audio = this.audio) {
    if (!audio?.src || !this.#isCurrent(generation, audio)) return;
    try {
      audio.currentTime = 0;
      await audio.play();
    } catch {
      if (!this.#isCurrent(generation, audio)) return;
      this.onStateChange("toca «Repetir audio»");
      this.repeatButton.focus({ preventScroll: true });
    }
  }

  clear() {
    this.generation += 1;
    const audio = this.audio;
    this.audio = null;
    if (audio) {
      this.#removeAudioListeners(audio);
      audio.pause();
      audio.removeAttribute("src");
      audio.load();
    }
    this.repeatButton.classList.add("hidden");
    this.onStateChange("sin contenido");
  }

  destroy() {
    this.clear();
    void this.context?.close?.();
  }

  #createAudio(generation) {
    const audio = new Audio();
    audio.preload = "metadata";
    audio.crossOrigin = "anonymous";
    audio.volume = this.volume;
    const updateIfCurrent = (state) => {
      if (this.#isCurrent(generation, audio)) this.onStateChange(state);
    };
    const listeners = {
      playing: () => updateIfCurrent("reproduciendo"),
      ended: () => updateIfCurrent("listo para repetir"),
      error: () => updateIfCurrent("error de carga"),
    };
    for (const [type, listener] of Object.entries(listeners)) {
      audio.addEventListener(type, listener);
    }
    this.audio = audio;
    this.audioBinding = { audio, listeners };
    return audio;
  }

  #removeAudioListeners(audio) {
    if (this.audioBinding?.audio !== audio) return;
    for (const [type, listener] of Object.entries(this.audioBinding.listeners)) {
      audio.removeEventListener?.(type, listener);
    }
    this.audioBinding = null;
  }

  #isCurrent(generation, audio) {
    return generation === this.generation && audio === this.audio;
  }
}
