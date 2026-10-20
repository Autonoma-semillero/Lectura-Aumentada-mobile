const CANVAS_ID = "floating-card-canvas";
const MAX_WORD_LENGTH = 48;
const CARD_WIDTH = 1.1;
const CARD_HEIGHT = 0.7333; // matches the approved mockup's 300x200 card ratio

// Depth stack colors and z-offsets straight from the approved mockup (back to front),
// scaled from its 300px-wide card down to our CARD_WIDTH-unit a-plane.
const DEPTH_LAYERS = [
  { color: "#D9D2C0", z: -0.0513 },
  { color: "#DDD6C5", z: -0.0403 },
  { color: "#E3DDCD", z: -0.0293 },
  { color: "#E9E4D6", z: -0.0183 },
  { color: "#F0ECE0", z: -0.0073 },
];

export function drawRoundedFill(canvas, fillStyle) {
  const ctx = canvas.getContext("2d");
  const { width, height } = canvas;
  const radius = Math.min(width, height) * 0.08;
  ctx.clearRect(0, 0, width, height);
  roundedRect(ctx, 0, 0, width, height, radius);
  ctx.fillStyle = fillStyle;
  ctx.fill();
}

export function drawCardFront(canvas) {
  const ctx = canvas.getContext("2d");
  const { width, height } = canvas;
  const radius = Math.min(width, height) * 0.08;

  ctx.clearRect(0, 0, width, height);
  ctx.save();
  ctx.shadowColor = "rgba(0, 0, 0, 0.3)";
  ctx.shadowBlur = width * 0.02;
  roundedRect(ctx, 0, 0, width, height, radius);
  ctx.fillStyle = "#ffffff";
  ctx.fill();
  ctx.restore();

  ctx.lineWidth = Math.max(2, width * 0.006);
  ctx.strokeStyle = "#F1EEE4";
  roundedRect(ctx, width * 0.006, height * 0.006, width * 0.988, height * 0.988, radius);
  ctx.stroke();
}

export function drawCardWord(canvas, word) {
  const ctx = canvas.getContext("2d");
  const { width, height } = canvas;
  const safeWord = sanitizeWord(word);
  if (!safeWord) return;

  ctx.font = `800 ${Math.round(height * 0.32)}px 'Nunito', system-ui, sans-serif`;
  ctx.fillStyle = "#C62828";
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(safeWord, width / 2, height / 2);
}

export function sanitizeWord(word) {
  if (typeof word !== "string") return "";
  return word.trim().slice(0, MAX_WORD_LENGTH);
}

function roundedRect(ctx, x, y, width, height, radius) {
  ctx.beginPath();
  ctx.moveTo(x + radius, y);
  ctx.arcTo(x + width, y, x + width, y + height, radius);
  ctx.arcTo(x + width, y + height, x, y + height, radius);
  ctx.arcTo(x, y + height, x, y, radius);
  ctx.arcTo(x, y, x + width, y, radius);
  ctx.closePath();
}

function createFilledPlane(fillStyle, z) {
  const canvas = document.createElement("canvas");
  canvas.id = `floating-card-layer-${Math.round(z * 1000)}`;
  canvas.width = 160;
  canvas.height = 107;
  canvas.style.display = "none";
  document.body.appendChild(canvas);
  drawRoundedFill(canvas, fillStyle);

  const plane = document.createElement("a-plane");
  plane.setAttribute("width", String(CARD_WIDTH));
  plane.setAttribute("height", String(CARD_HEIGHT));
  plane.setAttribute("position", `0 0 ${z}`);
  plane.setAttribute("material", `src: #${canvas.id}; transparent: true; shader: flat;`);
  return { plane, canvas };
}

/**
 * Renders the live session word as a card that looks like it has real physical thickness
 * (a stack of depth layers, matching the approved mockup) and gently bobs/tilts in place,
 * **inside** a billboard entity that keeps generally facing the viewer — the floaty motion
 * is layered on top of, not instead of, that facing behavior.
 */
export class FloatingCardController {
  constructor({ cardRoot, width = CARD_WIDTH, height = CARD_HEIGHT }) {
    this.cardRoot = cardRoot;
    this.width = width;
    this.height = height;
    this.canvases = [];
    this.facing = null;
    this.wobble = null;
    this.frontCanvas = null;
    this.frontPlane = null;
    this.word = "";
  }

  get plane() {
    return this.frontPlane;
  }

  mount(initialWord = "") {
    if (this.facing) return this.frontPlane;

    const facing = document.createElement("a-entity");
    facing.setAttribute("id", "floating-card-facing");
    facing.setAttribute("look-at", "#float-camera");

    const wobble = document.createElement("a-entity");
    wobble.setAttribute("id", "floating-card-wobble");
    wobble.setAttribute(
      "animation__rotx",
      "property: rotation.x; from: 10; to: 14; dir: alternate; dur: 2100; loop: true; easing: easeInOutSine"
    );
    wobble.setAttribute(
      "animation__roty",
      "property: rotation.y; from: -16; to: -9; dir: alternate; dur: 2100; loop: true; easing: easeInOutSine"
    );
    wobble.setAttribute(
      "animation__rotz",
      "property: rotation.z; from: -2; to: -1; dir: alternate; dur: 2100; loop: true; easing: easeInOutSine"
    );
    wobble.setAttribute(
      "animation__posy",
      "property: position.y; from: 0; to: 0.08; dir: alternate; dur: 2100; loop: true; easing: easeInOutSine"
    );

    for (const layer of DEPTH_LAYERS) {
      const { plane, canvas } = createFilledPlane(layer.color, layer.z);
      wobble.appendChild(plane);
      this.canvases.push(canvas);
    }

    const frontCanvas = document.createElement("canvas");
    frontCanvas.id = CANVAS_ID;
    frontCanvas.width = 1024;
    frontCanvas.height = 683;
    frontCanvas.style.display = "none";
    document.body.appendChild(frontCanvas);
    this.frontCanvas = frontCanvas;
    this.canvases.push(frontCanvas);

    const frontPlane = document.createElement("a-plane");
    frontPlane.setAttribute("id", "floating-card");
    frontPlane.setAttribute("width", String(this.width));
    frontPlane.setAttribute("height", String(this.height));
    frontPlane.setAttribute("position", "0 0 0");
    frontPlane.setAttribute("material", `src: #${CANVAS_ID}; transparent: true; shader: flat;`);
    wobble.appendChild(frontPlane);
    this.frontPlane = frontPlane;

    facing.appendChild(wobble);
    this.cardRoot.appendChild(facing);
    this.facing = facing;
    this.wobble = wobble;

    this.setWord(initialWord);
    return frontPlane;
  }

  setWord(word) {
    const safeWord = sanitizeWord(word);
    this.word = safeWord;
    if (!this.frontCanvas) return;
    drawCardFront(this.frontCanvas);
    drawCardWord(this.frontCanvas, safeWord);
    const mesh = this.frontPlane?.getObject3D?.("mesh");
    const map = mesh?.material?.map;
    if (map) map.needsUpdate = true;
  }

  unmount() {
    this.facing?.remove?.();
    for (const canvas of this.canvases) canvas?.remove?.();
    this.canvases = [];
    this.facing = null;
    this.wobble = null;
    this.frontCanvas = null;
    this.frontPlane = null;
    this.word = "";
  }
}
