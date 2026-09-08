import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");

test("allows the trusted bundled AR runtime to evaluate JavaScript and WebAssembly", async () => {
  const html = await readFile(resolve(projectRoot, "src/index.html"), "utf8");
  const policy = html.match(/Content-Security-Policy[\s\S]*?content="([^"]+)"/)?.[1];

  assert.ok(policy, "the WebAR page must declare a content security policy");
  assert.match(policy, /script-src[^;]*'unsafe-eval'/);
  assert.match(policy, /script-src[^;]*'wasm-unsafe-eval'/);
});
