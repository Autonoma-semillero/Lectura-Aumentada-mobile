import test from "node:test";
import assert from "node:assert/strict";
import {
  FloatingCardController,
  drawRoundedFill,
  drawCardFront,
  drawCardWord,
  sanitizeWord,
} from "../src/js/floating-card-controller.js";

function createFakeElement(tagName) {
  return {
    tagName,
    attributes: new Map(),
    children: [],
    style: {},
    setAttribute(name, value) {
      this.attributes.set(name, value);
    },
    appendChild(child) {
      this.children.push(child);
      child.parentElement = this;
      return child;
    },
    remove() {
      this.parentElement?.children.splice(this.parentElement.children.indexOf(this), 1);
      this.parentElement = null;
    },
    getContext() {
      return {
        clearRect() {},
        save() {},
        restore() {},
        fill() {},
        stroke() {},
        fillText() {},
        beginPath() {},
        moveTo() {},
        arcTo() {},
        closePath() {},
      };
    },
  };
}

function withFakeDom(run) {
  const previousDocument = globalThis.document;
  const body = createFakeElement("body");
  globalThis.document = {
    createElement: createFakeElement,
    body,
  };
  try {
    return run({ body });
  } finally {
    globalThis.document = previousDocument;
  }
}

test("mount nests a billboarded facing entity (and a wobble entity inside it) as the card root's only child", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    controller.mount();

    assert.equal(cardRoot.children.length, 1);
    const facing = cardRoot.children[0];
    assert.equal(facing.attributes.get("look-at"), "#float-camera");
    assert.equal(facing.children.length, 1);
    const wobble = facing.children[0];
    assert.match(wobble.attributes.get("animation__rotx"), /property: rotation\.x/);
    assert.match(wobble.attributes.get("animation__posy"), /property: position\.y/);
  });
});

test("mount builds five depth-layer planes plus the front face, all inside the wobble entity", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    const frontPlane = controller.mount();

    const wobble = cardRoot.children[0].children[0];
    // 5 depth layers + the front face.
    assert.equal(wobble.children.length, 6);
    assert.equal(wobble.children.at(-1), frontPlane);
    assert.equal(frontPlane.tagName, "a-plane");
    assert.match(frontPlane.attributes.get("material"), /src: #floating-card-canvas/);
  });
});

test("mounting twice reuses the same front plane instead of duplicating the whole card", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    const first = controller.mount();
    const second = controller.mount();

    assert.equal(first, second);
    assert.equal(cardRoot.children.length, 1);
  });
});

test("unmount removes the facing entity and every backing canvas", () => {
  withFakeDom(({ body }) => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });
    controller.mount();
    assert.ok(body.children.length > 0);

    controller.unmount();

    assert.equal(cardRoot.children.length, 0);
    assert.equal(body.children.length, 0);
  });
});

test("mounts with an initial word drawn onto the front canvas", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    controller.mount("gato");

    assert.equal(controller.word, "gato");
  });
});

test("setWord redraws the front canvas and flags its texture for an update", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });
    controller.mount();

    const map = { needsUpdate: false };
    controller.plane.getObject3D = () => ({ material: { map } });

    controller.setWord("perro");

    assert.equal(controller.word, "perro");
    assert.equal(map.needsUpdate, true);
  });
});

test("draws a rounded fill without throwing", () => {
  const canvas = { width: 100, height: 60, getContext: createFakeElement("canvas").getContext };
  assert.doesNotThrow(() => drawRoundedFill(canvas, "#D9D2C0"));
});

test("draws the card front (shadow + border) without throwing", () => {
  const canvas = { width: 100, height: 60, getContext: createFakeElement("canvas").getContext };
  assert.doesNotThrow(() => drawCardFront(canvas));
});

test("draws the word without throwing and skips an empty word", () => {
  const canvas = { width: 100, height: 60, getContext: createFakeElement("canvas").getContext };
  assert.doesNotThrow(() => drawCardWord(canvas, "hola"));
  assert.doesNotThrow(() => drawCardWord(canvas, ""));
});

test("sanitizeWord trims and caps word length", () => {
  assert.equal(sanitizeWord("  hola  "), "hola");
  assert.equal(sanitizeWord(42), "");
  assert.equal(sanitizeWord("a".repeat(60)).length, 48);
});
