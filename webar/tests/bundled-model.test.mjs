import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const animalsRoot = resolve(projectRoot, "../app/src/main/assets/models/animals");
const expectedAnimations = [
  "static",
  "idle",
  "walk",
  "run",
  "eat",
  "dance",
  "gesture-positive",
  "gesture-negative",
];
const expectedResources = new Map([
  [
    "Textures/colormap.png",
    "2f6d0032e51c2d49b13b8e689d59eb39aa3ee6c32f2a1851213e11fc7593177d",
  ],
]);
const bundledModels = [
  {
    animal: "cat",
    file: "animal-cat.glb",
    scene: "animal-cat",
    sha256: "1e73aaf7d497dd7d2946badf92d189d2333f163e66dced91feb446bc9bdee850",
  },
  {
    animal: "dog",
    file: "animal-dog.glb",
    scene: "animal-dog",
    sha256: "c127d71ac8794f9710313bbb44800ae33a79f1182cf5c523c4233cc10a93851f",
  },
];

function sha256(content) {
  return createHash("sha256").update(content).digest("hex");
}

function parseGlbManifest(model) {
  assert.equal(model.subarray(0, 4).toString("ascii"), "glTF");
  assert.equal(model.readUInt32LE(4), 2);
  assert.equal(model.readUInt32LE(8), model.length);

  const jsonChunkLength = model.readUInt32LE(12);
  assert.equal(model.readUInt32LE(16), 0x4e4f534a, "the first GLB chunk must be JSON");
  return JSON.parse(
    model.subarray(20, 20 + jsonChunkLength).toString("utf8").replace(/[\u0000 ]+$/u, "")
  );
}

test("bundles verified glTF 2.0 animal models and their external resources", async (t) => {
  for (const expected of bundledModels) {
    await t.test(expected.animal, async () => {
      const modelPath = resolve(animalsRoot, expected.file);
      const model = await readFile(modelPath);

      assert.equal(sha256(model), expected.sha256);

      const manifest = parseGlbManifest(model);
      assert.equal(manifest.asset?.version, "2.0");
      assert.equal(manifest.scenes?.[manifest.scene]?.name, expected.scene);
      assert.deepEqual(
        (manifest.animations ?? []).map(({ name }) => name),
        expectedAnimations
      );

      const externalUris = [
        ...(manifest.images ?? []).map(({ uri }) => uri),
        ...(manifest.buffers ?? []).map(({ uri }) => uri),
      ].filter((uri) => uri && !/^(?:data:|https?:|blob:)/iu.test(uri));

      assert.deepEqual([...new Set(externalUris)].sort(), [...expectedResources.keys()].sort());
      await Promise.all(
        externalUris.map(async (uri) => {
          const resource = await readFile(resolve(dirname(modelPath), decodeURIComponent(uri)));
          assert.equal(sha256(resource), expectedResources.get(uri), `${uri} must be unmodified`);
        })
      );
    });
  }
});
