import test from "node:test";
import assert from "node:assert/strict";
import { access, readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const outputRoot = resolve(projectRoot, "../app/src/main/assets/webar");
const vendorRoot = resolve(outputRoot, "vendor");

test("build vendorizes the marker build but not the unused NFT build", async () => {
  const markerVendored = await readFile(resolve(vendorRoot, "aframe-ar.js"));
  assert.ok(markerVendored.length > 0, "aframe-ar.js (marker build) must still be vendorized");

  await assert.rejects(
    access(resolve(vendorRoot, "aframe-ar-nft.js")),
    "aframe-ar-nft.js has no caller in this bundle and must not be vendorized",
  );
});

test("build copies the floating-card document alongside the marker document", async () => {
  const floatDocument = await readFile(resolve(outputRoot, "index-float.html"), "utf8");
  assert.match(floatDocument, /<title>/i);
});
