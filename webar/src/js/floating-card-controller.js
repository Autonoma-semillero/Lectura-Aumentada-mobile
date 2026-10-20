const CANVAS_ID = "floating-card-canvas";

export function drawCardBackground(canvas) {
  const ctx = canvas.getContext("2d");
  const { width, height } = canvas;
  const radius = Math.min(width, height) * 0.08;

  ctx.clearRect(0, 0, width, height);
  ctx.save();
  ctx.shadowColor = "rgba(0, 0, 0, 0.45)";
  ctx.shadowBlur = width * 0.03;
  roundedRect(ctx, width * 0.03, height * 0.05, width * 0.94, height * 0.9, radius);
  const gradient = ctx.createRadialGradient(
    width * 0.5,
    height * 0.35,
    width * 0.1,
    width * 0.5,
    height * 0.5,
    width * 0.6
  );
  gradient.addColorStop(0, "#f8fafc");
  gradient.addColorStop(1, "#cbd5f5");
  ctx.fillStyle = gradient;
  ctx.fill();
  ctx.restore();

  ctx.lineWidth = Math.max(2, width * 0.006);
  ctx.strokeStyle = "rgba(37, 56, 110, 0.55)";
  roundedRect(ctx, width * 0.03, height * 0.05, width * 0.94, height * 0.9, radius);
  ctx.stroke();
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

export class FloatingCardController {
  constructor({ cardRoot, width = 1.6, height = 1 }) {
    this.cardRoot = cardRoot;
    this.width = width;
    this.height = height;
    this.canvas = null;
    this.plane = null;
  }

  mount() {
    if (this.plane) return this.plane;

    const canvas = document.createElement("canvas");
    canvas.id = CANVAS_ID;
    canvas.width = 1024;
    canvas.height = 640;
    canvas.style.display = "none";
    document.body.appendChild(canvas);
    drawCardBackground(canvas);
    this.canvas = canvas;

    const plane = document.createElement("a-plane");
    plane.setAttribute("id", "floating-card");
    plane.setAttribute("width", String(this.width));
    plane.setAttribute("height", String(this.height));
    plane.setAttribute("material", `src: #${CANVAS_ID}; transparent: true; shader: flat;`);
    this.cardRoot.appendChild(plane);
    this.plane = plane;
    return plane;
  }

  unmount() {
    this.plane?.remove?.();
    this.canvas?.remove?.();
    this.plane = null;
    this.canvas = null;
  }
}
