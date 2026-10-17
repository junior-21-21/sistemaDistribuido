const target = process.env.LOAD_URL ?? 'http://localhost:8080/actuator/health';
const total = Number(process.env.LOAD_REQUESTS ?? 500);
const concurrency = Number(process.env.LOAD_CONCURRENCY ?? 20);
const maxP95 = Number(process.env.LOAD_MAX_P95_MS ?? 750);
const maxErrorRate = Number(process.env.LOAD_MAX_ERROR_RATE ?? 0.01);
const token = process.env.LOAD_TOKEN ?? '';

if (!Number.isInteger(total) || total < 1 || !Number.isInteger(concurrency) || concurrency < 1) {
  throw new Error('LOAD_REQUESTS y LOAD_CONCURRENCY deben ser enteros positivos');
}

const durations = [];
let next = 0;
let failures = 0;
const started = performance.now();

async function worker() {
  while (true) {
    const current = next++;
    if (current >= total) return;
    const requestStarted = performance.now();
    try {
      const response = await fetch(target, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        signal: AbortSignal.timeout(5000),
      });
      if (!response.ok) failures++;
      await response.arrayBuffer();
    } catch {
      failures++;
    } finally {
      durations.push(performance.now() - requestStarted);
    }
  }
}

await Promise.all(Array.from({ length: Math.min(concurrency, total) }, () => worker()));
durations.sort((a, b) => a - b);
const percentile = (p) => durations[Math.min(durations.length - 1, Math.ceil(durations.length * p) - 1)];
const elapsedMs = performance.now() - started;
const result = {
  target,
  requests: total,
  concurrency,
  failures,
  errorRate: failures / total,
  p50Ms: Number(percentile(0.50).toFixed(2)),
  p95Ms: Number(percentile(0.95).toFixed(2)),
  p99Ms: Number(percentile(0.99).toFixed(2)),
  requestsPerSecond: Number((total / (elapsedMs / 1000)).toFixed(2)),
};
console.log(JSON.stringify(result, null, 2));

if (result.errorRate > maxErrorRate || result.p95Ms > maxP95) {
  console.error(`Umbral incumplido: errorRate<=${maxErrorRate}, p95<=${maxP95}ms`);
  process.exit(1);
}
