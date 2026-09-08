import { copyFile, cp, mkdir } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const projectRoot = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const sourceRoot = resolve(projectRoot, "src");
const outputRoot = resolve(projectRoot, "../app/src/main/assets/webar");

await mkdir(resolve(outputRoot, "vendor"), { recursive: true });
await copyFile(resolve(sourceRoot, "index.html"), resolve(outputRoot, "index.html"));
await copyFile(resolve(sourceRoot, "styles.css"), resolve(outputRoot, "styles.css"));
await cp(resolve(sourceRoot, "js"), resolve(outputRoot, "js"), {
  recursive: true,
  force: true,
});
await cp(resolve(sourceRoot, "markers"), resolve(outputRoot, "markers"), {
  recursive: true,
  force: true,
});
await copyFile(
  resolve(projectRoot, "node_modules/aframe/dist/aframe-master.min.js"),
  resolve(outputRoot, "vendor/aframe.min.js"),
);
await copyFile(
  resolve(projectRoot, "node_modules/@ar-js-org/ar.js/aframe/build/aframe-ar.js"),
  resolve(outputRoot, "vendor/aframe-ar.js"),
);

console.log(`WebAR assets generated in ${outputRoot}`);
