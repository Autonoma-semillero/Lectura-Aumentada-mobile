import test from "node:test";
import assert from "node:assert/strict";
import { CardTextController, drawCardWord, sanitizeWord } from "../src/js/card-text-controller.js";

function createFakeElement(tagName, { fillTextCalls } = {}) {
  return {
    tagName,
    attributes: new Map(),
    children: [],
    style: {},
    width: 1024,
    height: 320,
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
        fillText(text) {
          fillTextCalls?.push(text);
        },
      };
    },
  };
}

function withFakeDom(run) {
  const previousDocument = globalThis.document;
  const fillTextCalls = [];
  const body = createFakeElement("body");
  globalThis.document = {
    createElement: (tagName) => createFakeElement(tagName, { fillTextCalls }),
    body,
  };
  try {
    return run({ body, fillTextCalls });
  } finally {
    globalThis.document = previousDocument;
  }
}

test("sanitizeWord trims and caps the drawn word length", () => {
  assert.equal(sanitizeWord("  gato  "), "gato");
  assert.equal(sanitizeWord(42), "");
  assert.equal(sanitizeWord("a".repeat(80)).length, 48);
});

test("drawCardWord skips drawing an empty sanitized word", () => {
  const calls = [];
  const canvas = { width: 100, height: 40, getContext: () => ({ clearRect() {}, fillText(text) { calls.push(text); } }) };
  drawCardWord(canvas, "   ");
  assert.deepEqual(calls, []);
});

test("mount draws the initial word and parents the plane under the card", () => {
  withFakeDom(({ body, fillTextCalls }) => {
    const parent = createFakeElement("a-plane");
    const controller = new CardTextController({ parent });

    const plane = controller.mount("gato");

    assert.equal(parent.children[0], plane);
    assert.equal(body.children[0].tagName, "canvas");
    assert.deepEqual(fillTextCalls, ["gato"]);
  });
});

test("setWord redraws the canvas and flags the live mesh texture for update", () => {
  withFakeDom(({ fillTextCalls }) => {
    const parent = createFakeElement("a-plane");
    const controller = new CardTextController({ parent });
    controller.mount("gato");

    const map = { needsUpdate: false };
    controller.plane.getObject3D = (name) =>
      name === "mesh" ? { material: { map } } : null;

    controller.setWord("perro");

    assert.deepEqual(fillTextCalls, ["gato", "perro"]);
    assert.equal(map.needsUpdate, true);
    assert.equal(controller.word, "perro");
  });
});

test("unmount removes the plane and the backing canvas", () => {
  withFakeDom(({ body }) => {
    const parent = createFakeElement("a-plane");
    const controller = new CardTextController({ parent });
    controller.mount("gato");

    controller.unmount();

    assert.equal(parent.children.length, 0);
    assert.equal(body.children.length, 0);
  });
});
