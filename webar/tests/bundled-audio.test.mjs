import test from "node:test";
import assert from "node:assert/strict";
import { createHash } from "node:crypto";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const audioPath = resolve(
  projectRoot,
  "../app/src/main/assets/audio/animals/gato.mp3"
);

test("bundles the verified CC BY-SA cat pronunciation as an MP3 asset", async () => {
  const audio = await readFile(audioPath);

  assert.equal(audio.length, 11749);
  assert.equal(audio.subarray(0, 3).toString("ascii"), "ID3");
  assert.doesNotMatch(audio.subarray(0, 256).toString("utf8"), /<!doctype|<html/iu);
  assert.equal(
    createHash("sha256").update(audio).digest("hex"),
    "4123ed880abeb1536e0ac947bb6649a2e42158708d032eb9bed218b65ad18a7e"
  );
});
