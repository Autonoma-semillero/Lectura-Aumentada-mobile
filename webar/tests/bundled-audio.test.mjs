import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const bundledAudio = [
  {
    word: "gato",
    bytes: 11749,
    sha256: "4123ed880abeb1536e0ac947bb6649a2e42158708d032eb9bed218b65ad18a7e",
  },
  {
    word: "perro",
    bytes: 12274,
    sha256: "007f21beaca7f78ec4f135b25d4e52e926228e8f965694ab1103b6b4e7a4fe37",
  },
];

for (const asset of bundledAudio) {
  test(`bundles the verified CC BY-SA ${asset.word} pronunciation as an MP3 asset`, async () => {
    const audioPath = resolve(
      projectRoot,
      `../app/src/main/assets/audio/animals/${asset.word}.mp3`
    );
    const audio = await readFile(audioPath);

    assert.equal(audio.length, asset.bytes);
    assert.equal(audio.subarray(0, 3).toString("ascii"), "ID3");
    assert.doesNotMatch(audio.subarray(0, 256).toString("utf8"), /<!doctype|<html/iu);
    assert.equal(
      createHash("sha256").update(audio).digest("hex"),
      asset.sha256
    );
  });
}
