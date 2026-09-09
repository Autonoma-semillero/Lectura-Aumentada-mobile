export const WORD_TARGET_MODEL_ID = "__camera-word-target__";
const MAX_WORD_LENGTH = 64;
const WHITESPACE = /\s+/gu;
const OCR_APOSTROPHES = /[\u2018\u2019\u02BC\uFF07]/gu;
const OCR_HYPHENS = /[\u2010\u2011\u2012\u2013\u2014\u2212\uFF0D]/gu;
const UNSUPPORTED_CHARACTERS = /[^\p{Letter}\p{Number} '\-]+/gu;
const OUTER_SEPARATORS = /^[ '\-]+|[ '\-]+$/gu;
const VALID_CANONICAL_WORD = /^[\p{Letter}\p{Number}]+(?:(?: +|['-])[\p{Letter}\p{Number}]+)*$/u;

export function normalizeWord(word) {
  if (typeof word !== "string") return "";
  const canonical = word
    .normalize("NFKC")
    .trim()
    .replace(WHITESPACE, " ")
    .toLocaleLowerCase("es-CO")
    .replace(OCR_APOSTROPHES, "'")
    .replace(OCR_HYPHENS, "-")
    .normalize("NFKD")
    .replace(/n\u0303/gu, "ñ")
    .replace(/\p{Mark}+/gu, "")
    .replace(UNSUPPORTED_CHARACTERS, "")
    .replace(WHITESPACE, " ")
    .trim()
    .replace(OUTER_SEPARATORS, "")
    .normalize("NFC");
  return canonical.length >= 1 && canonical.length <= MAX_WORD_LENGTH &&
    VALID_CANONICAL_WORD.test(canonical)
    ? canonical
    : "";
}

export class WordTargetGate {
  #activeTarget = null;

  get activeTarget() {
    return this.#activeTarget;
  }

  activate(word, { centerX, centerY } = {}) {
    const target = createTarget(word, centerX, centerY);
    const previousTarget = this.#activeTarget;
    const wordChanged = target.normalizedWord !== previousTarget?.normalizedWord;
    const positionChanged =
      target.centerX !== previousTarget?.centerX || target.centerY !== previousTarget?.centerY;
    if (!wordChanged && !positionChanged) return false;

    this.#activeTarget = target;
    return { ...target, previousTarget, wordChanged, positionChanged };
  }

  matches(word) {
    return Boolean(
      this.#activeTarget &&
      normalizeWord(word) === this.#activeTarget.normalizedWord
    );
  }

  clear(word) {
    if (!this.#activeTarget) return false;
    if (word !== undefined && !this.matches(word)) return false;

    const target = this.#activeTarget;
    this.#activeTarget = null;
    return target;
  }

  reset() {
    return this.clear();
  }
}

export function resolveAssetTarget({ asset, activeWordTarget, activeMarkerId }) {
  if (activeWordTarget) {
    return normalizeWord(asset?.word) === activeWordTarget.normalizedWord
      ? WORD_TARGET_MODEL_ID
      : null;
  }
  return asset?.markerId === activeMarkerId ? activeMarkerId : null;
}

function createTarget(word, centerX, centerY) {
  if (typeof word !== "string") throw new TypeError("El objetivo de palabra no es válido");

  const displayWord = word.trim().replace(/\s+/g, " ");
  const normalizedWord = normalizeWord(displayWord);
  if (!normalizedWord) {
    throw new TypeError("El objetivo de palabra no es válido");
  }
  return Object.freeze({
    word: displayWord,
    normalizedWord,
    centerX: normalizeCoordinate(centerX),
    centerY: normalizeCoordinate(centerY),
  });
}

function normalizeCoordinate(value) {
  if (value === undefined || value === null) return 0.5;
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw new TypeError("La posición del objetivo de palabra no es válida");
  }
  return Math.min(1, Math.max(0, value));
}
