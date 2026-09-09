import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
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

  const jsonChunkLength = model.readUInt32LE(12);
  assert.equal(model.readUInt32LE(16), 0x4e4f534a, "the first GLB chunk must be JSON");
  const manifest = JSON.parse(
    model.subarray(20, 20 + jsonChunkLength).toString("utf8").replace(/[\u0000 ]+$/u, "")
  );
  const externalUris = [
    ...(manifest.images ?? []).map(({ uri }) => uri),
    ...(manifest.buffers ?? []).map(({ uri }) => uri),
  ].filter((uri) => uri && !/^(?:data:|https?:|blob:)/iu.test(uri));

  assert.ok(externalUris.length > 0, "fixture should exercise external GLB resources");
  await Promise.all(
    externalUris.map((uri) => access(resolve(dirname(modelPath), decodeURIComponent(uri))))
  );
});
