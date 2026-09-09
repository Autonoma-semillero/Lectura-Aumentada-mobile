import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const modelPath = resolve(
  projectRoot,
  "../app/src/main/assets/models/animals/animal-cat.glb"
);

test("bundles a valid glTF 2.0 cat model for the Hiro marker", async () => {
  const model = await readFile(modelPath);

  assert.equal(model.subarray(0, 4).toString("ascii"), "glTF");
  assert.equal(model.readUInt32LE(4), 2);
  assert.equal(model.readUInt32LE(8), model.length);
});
