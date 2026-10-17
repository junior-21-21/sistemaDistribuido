// Node 22+ expone un `localStorage` global que devuelve `undefined` si no se inicia con
// --localstorage-file, y ese getter oculta el de jsdom. Se reemplaza por un Storage en memoria
// para que las pruebas se comporten igual en cualquier versión de Node.
class MemoryStorage implements Storage {
  private readonly data = new Map<string, string>();

  get length(): number {
    return this.data.size;
  }

  clear(): void {
    this.data.clear();
  }

  getItem(key: string): string | null {
    return this.data.has(key) ? this.data.get(key)! : null;
  }

  key(index: number): string | null {
    return Array.from(this.data.keys())[index] ?? null;
  }

  removeItem(key: string): void {
    this.data.delete(key);
  }

  setItem(key: string, value: string): void {
    this.data.set(key, String(value));
  }
}

for (const name of ['localStorage', 'sessionStorage'] as const) {
  let usable = false;
  try {
    usable = typeof globalThis[name]?.getItem === 'function';
  } catch {
    usable = false;
  }
  if (!usable) {
    Object.defineProperty(globalThis, name, {
      configurable: true,
      value: new MemoryStorage(),
    });
  }
}
