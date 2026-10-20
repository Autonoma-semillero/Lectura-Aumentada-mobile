import test from "node:test";
import assert from "node:assert/strict";
import { FloatingCardController, drawCardBackground } from "../src/js/floating-card-controller.js";

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
        beginPath() {},
        moveTo() {},
        arcTo() {},
        closePath() {},
        createRadialGradient() {
          return { addColorStop() {} };
        },
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

test("mounts a canvas-backed plane as a child of the camera-relative card root", () => {
  withFakeDom(({ body }) => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    const plane = controller.mount();

    assert.equal(plane.tagName, "a-plane");
    assert.equal(cardRoot.children[0], plane);
    assert.equal(body.children[0].tagName, "canvas");
    assert.match(plane.attributes.get("material"), /src: #floating-card-canvas/);
  });
});

test("mounting twice reuses the same plane instead of duplicating it", () => {
  withFakeDom(() => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });

    const first = controller.mount();
    const second = controller.mount();

    assert.equal(first, second);
    assert.equal(cardRoot.children.length, 1);
  });
});

test("unmount removes both the plane and its backing canvas", () => {
  withFakeDom(({ body }) => {
    const cardRoot = createFakeElement("a-entity");
    const controller = new FloatingCardController({ cardRoot });
    controller.mount();

    controller.unmount();

    assert.equal(cardRoot.children.length, 0);
    assert.equal(body.children.length, 0);
  });
});

test("draws a rounded, shadowed card background without throwing", () => {
  const canvas = { width: 100, height: 60, getContext: createFakeElement("canvas").getContext };
  assert.doesNotThrow(() => drawCardBackground(canvas));
});
