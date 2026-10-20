const CANVAS_ID = "card-word-canvas";
const MAX_WORD_LENGTH = 48;

export function drawCardWord(canvas, word) {
  const ctx = canvas.getContext("2d");
  const { width, height } = canvas;
  ctx.clearRect(0, 0, width, height);
  const safeWord = sanitizeWord(word);
  if (!safeWord) return;

  ctx.font = `bold ${Math.round(height * 0.32)}px system-ui, sans-serif`;
  ctx.fillStyle = "#1e293b";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(safeWord, width / 2, height / 2);
}

export function sanitizeWord(word) {
  if (typeof word !== "string") return "";
  return word.trim().slice(0, MAX_WORD_LENGTH);
}

export class CardTextController {
  constructor({ parent, width = 1.4, height = 0.5 }) {
    this.parent = parent;
    this.width = width;
    this.height = height;
    this.canvas = null;
    this.plane = null;
    this.word = "";
  }

  mount(initialWord = "") {
    if (this.plane) return this.plane;

    const canvas = document.createElement("canvas");
    canvas.id = CANVAS_ID;
    canvas.width = 1024;
    canvas.height = 320;
    canvas.style.display = "none";
    document.body.appendChild(canvas);
    this.canvas = canvas;

    const plane = document.createElement("a-plane");
    plane.setAttribute("id", "card-word");
    plane.setAttribute("width", String(this.width));
    plane.setAttribute("height", String(this.height));
    plane.setAttribute("position", "0 0 0.01");
    plane.setAttribute("material", `src: #${CANVAS_ID}; transparent: true; shader: flat;`);
    this.parent.appendChild(plane);
    this.plane = plane;

    this.setWord(initialWord);
    return plane;
  }

  setWord(word) {
    const safeWord = sanitizeWord(word);
    this.word = safeWord;
    if (!this.canvas) return;
    drawCardWord(this.canvas, safeWord);
    const mesh = this.plane?.getObject3D?.("mesh");
    const map = mesh?.material?.map;
    if (map) map.needsUpdate = true;
  }

  unmount() {
    this.plane?.remove?.();
    this.canvas?.remove?.();
    this.plane = null;
    this.canvas = null;
    this.word = "";
  }
}
