import { copyFile, cp, mkdir, readFile, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

// A-Frame incrusta el webvr-polyfill, que al iniciar pide la Device Parameters
// Database a un dominio muerto (el TLS se corta: ERR_CONNECTION_CLOSED). Esos
// datos solo sirven para corregir distorsion de lentes Cardboard, no aplican a
// AR por marcadores, y el polyfill ya trae la misma base embebida y la aplica
// antes de salir a la red. Su cargador solo pide la URL si es truthy, asi que
// vaciarla elimina la peticion sin cambiar el comportamiento.
const DPDB_URL = "https://dpdb.webvr.rocks/dpdb.json";

async function copyAframeWithoutDpdbFetch(from, to) {
  const source = await readFile(from, "utf8");
  const occurrences = source.split(DPDB_URL).length - 1;
  if (occurrences === 0) {
    throw new Error(
      `No se encontro ${DPDB_URL} en ${from}. Si A-Frame cambio de version, ` +
        `revisa si el polyfill sigue saliendo a la red antes de quitar este parche.`,
    );
  }
  await writeFile(to, source.replaceAll(DPDB_URL, ""), "utf8");
  return occurrences;
}

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
const patchedDpdbUrls = await copyAframeWithoutDpdbFetch(
  resolve(projectRoot, "node_modules/aframe/dist/aframe-master.min.js"),
  resolve(outputRoot, "vendor/aframe.min.js"),
);
await copyFile(
  resolve(projectRoot, "node_modules/@ar-js-org/ar.js/aframe/build/aframe-ar.js"),
  resolve(outputRoot, "vendor/aframe-ar.js"),
);

console.log(`WebAR assets generated in ${outputRoot}`);
console.log(`DPDB remoto desactivado en aframe.min.js (${patchedDpdbUrls} URL)`);
