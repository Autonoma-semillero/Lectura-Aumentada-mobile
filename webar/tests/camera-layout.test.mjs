import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");

test("pins the AR.js camera feed to the complete viewport", async () => {
  const css = await readFile(resolve(projectRoot, "src/styles.css"), "utf8");
  const viewportRule = css.match(/html,\s*body,\s*#experience\s*{([^}]+)}/)?.[1];
  const cameraRule = css.match(/#ar-root > video\s*{([^}]+)}/)?.[1];

  assert.ok(viewportRule, "the WebAR document must have a dedicated viewport rule");
  assert.match(viewportRule, /width:\s*100%\s*!important/);
  assert.match(viewportRule, /height:\s*100%\s*;/);
  assert.doesNotMatch(viewportRule, /height:\s*100%\s*!important/);
  assert.match(viewportRule, /margin:\s*0\s*!important/);

  assert.ok(cameraRule, "the mounted camera video must have a dedicated layout rule");
  assert.match(cameraRule, /inset:\s*0\s*!important/);
  assert.match(cameraRule, /width:\s*100%\s*!important/);
  assert.match(cameraRule, /height:\s*100%\s*!important/);
  assert.match(cameraRule, /margin:\s*0\s*!important/);
  assert.match(cameraRule, /object-fit:\s*cover\s*!important/);
});
