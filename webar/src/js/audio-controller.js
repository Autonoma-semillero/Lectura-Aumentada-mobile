export class AudioController {
  constructor({ onStateChange, repeatButton, volume = 0.85 }) {
    this.onStateChange = onStateChange;
    this.repeatButton = repeatButton;
    this.audio = new Audio();
    this.audio.preload = "metadata";
    this.audio.crossOrigin = "anonymous";
    this.audio.volume = volume;
    this.audio.addEventListener("playing", () => this.onStateChange("reproduciendo"));
    this.audio.addEventListener("ended", () => this.onStateChange("listo para repetir"));
    this.audio.addEventListener("error", () => this.onStateChange("error de carga"));
    this.repeatButton.addEventListener("click", () => void this.play());
  }

  async unlock() {
    const AudioContextClass = window.AudioContext || window.webkitAudioContext;
    if (!AudioContextClass) return;
    this.context ??= new AudioContextClass();
    await this.context.resume();
    const source = this.context.createBufferSource();
    source.buffer = this.context.createBuffer(1, 1, 22050);
    source.connect(this.context.destination);
    source.start(0);
  }

  async loadAndPlay(audioUrl) {
    this.clear();
    if (!audioUrl) {
      this.onStateChange("sin audio");
      return;
    }
    this.audio.src = audioUrl;
    this.audio.load();
    this.repeatButton.classList.remove("hidden");
    this.onStateChange("cargando");
    await this.play();
  }

  async play() {
    if (!this.audio.src) return;
    try {
      this.audio.currentTime = 0;
      await this.audio.play();
    } catch {
      this.onStateChange("toca «Repetir audio»");
      this.repeatButton.focus({ preventScroll: true });
    }
  }

  clear() {
    this.audio.pause();
    this.audio.removeAttribute("src");
    this.audio.load();
    this.repeatButton.classList.add("hidden");
    this.onStateChange("sin contenido");
  }

  destroy() {
    this.clear();
    void this.context?.close?.();
  }
}
