#!/usr/bin/env node
// Terminal chat with a Hoplite cloud agent over the official ACP endpoint.
// Usage: HOPLITE_API_KEY=hop_... node hoplite-chat.mjs [--project <name|id>] [--continue | --resume <sessionId>]
import readline from "node:readline";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const API = "https://api.hoplite.sh";
const KEY = process.env.HOPLITE_API_KEY;
if (!KEY) {
  console.error("Set HOPLITE_API_KEY first (Settings -> Workspace -> API keys).");
  process.exit(1);
}
const args = process.argv.slice(2);
const flag = (n) => { const i = args.indexOf(n); return i >= 0 ? args[i + 1] : undefined; };
const STATE = path.join(os.homedir(), ".hoplite-chat.json");
const state = fs.existsSync(STATE) ? JSON.parse(fs.readFileSync(STATE, "utf8")) : {};

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
const ask = (q) => new Promise((r) => rl.question(q, r));

async function pickProject() {
  const res = await fetch(`${API}/api/projects`, { headers: { "X-Api-Key": KEY } });
  if (!res.ok) throw new Error(`GET /api/projects -> ${res.status} ${await res.text()}`);
  const { projects } = await res.json();
  if (!projects?.length) throw new Error("No projects in this workspace. Create one at app.hoplite.sh.");
  const want = flag("--project");
  if (want) {
    const p = projects.find((p) => p.id === want || p.name === want || p.name.endsWith("/" + want));
    if (!p) throw new Error(`Project "${want}" not found. Have: ${projects.map((p) => p.name).join(", ")}`);
    return p;
  }
  if (projects.length === 1) return projects[0];
  projects.forEach((p, i) => console.log(`${i + 1}) ${p.name}`));
  const n = Number(await ask("Project number: "));
  return projects[n - 1] ?? projects[0];
}

const project = await pickProject();

// The WebSocket API hides the HTTP status of a failed upgrade, so probe auth over HTTP first.
const probe = await fetch(`${API}/acp?projectId=${encodeURIComponent(project.id)}`, {
  method: "POST",
  headers: { Authorization: `Bearer ${KEY}`, "Content-Type": "application/json", Accept: "application/json, text/event-stream" },
  body: JSON.stringify({ jsonrpc: "2.0", id: 0, method: "initialize", params: { protocolVersion: 1, clientCapabilities: {}, clientInfo: { name: "hoplite-chat", version: "1.0.0" } } }),
});
if (probe.status === 401 || probe.status === 403) {
  console.error(`ACP rejected the key (${probe.status} ${await probe.text()}).`);
  console.error("The key needs: project:read, thread:create, thread:read, thread:update, thread:stop.");
  process.exit(1);
}
const connId = probe.headers.get("acp-connection-id");
if (connId) fetch(`${API}/acp`, { method: "DELETE", headers: { Authorization: `Bearer ${KEY}`, "Acp-Connection-Id": connId } }).catch(() => {});

// Node's built-in (undici) WebSocket accepts custom upgrade headers.
const ws = new WebSocket(`wss://api.hoplite.sh/acp?projectId=${encodeURIComponent(project.id)}`, {
  headers: { Authorization: `Bearer ${KEY}` },
});
let nextId = 1;
const pending = new Map();
let busy = false;

function rpc(method, params) {
  const id = nextId++;
  ws.send(JSON.stringify({ jsonrpc: "2.0", id, method, params }));
  return new Promise((resolve, reject) => pending.set(id, { resolve, reject }));
}

ws.addEventListener("message", (ev) => {
  const msg = JSON.parse(typeof ev.data === "string" ? ev.data : Buffer.from(ev.data).toString());
  if (msg.id != null && pending.has(msg.id)) {
    const { resolve, reject } = pending.get(msg.id);
    pending.delete(msg.id);
    return msg.error ? reject(new Error(JSON.stringify(msg.error))) : resolve(msg.result);
  }
  if (msg.method === "session/update") {
    const u = msg.params?.update ?? {};
    if (u.sessionUpdate === "agent_message_chunk" && u.content?.type === "text") process.stdout.write(u.content.text);
    else if (u.sessionUpdate === "user_message_chunk" && u.content?.type === "text") process.stdout.write(`\n> ${u.content.text}\n`);
  }
});
ws.addEventListener("close", (ev) => { console.log(`\n[connection closed ${ev.code} ${ev.reason || ""}]`); process.exit(0); });
await new Promise((resolve, reject) => {
  ws.addEventListener("open", resolve, { once: true });
  ws.addEventListener("error", (e) => reject(new Error(`WebSocket failed: ${e.message ?? "check key/permissions"}`)), { once: true });
});

await rpc("initialize", { protocolVersion: 1, clientCapabilities: {}, clientInfo: { name: "hoplite-chat", version: "1.0.0" } });
await rpc("authenticate", { methodId: "hoplite-api-key" });

let sessionId = flag("--resume") ?? (args.includes("--continue") ? state[project.id] : undefined);
if (sessionId) {
  await rpc("session/load", { sessionId, cwd: "/workspace", mcpServers: [] });
  console.log(`\n[resumed ${sessionId}]`);
} else {
  ({ sessionId } = await rpc("session/new", { cwd: "/workspace", mcpServers: [] }));
}
state[project.id] = sessionId;
fs.writeFileSync(STATE, JSON.stringify(state, null, 2));
console.log(`Hoplite · ${project.name} · session ${sessionId}`);
console.log("Type a message. Ctrl+C stops the current run; /exit quits.\n");

rl.on("SIGINT", () => {
  if (busy) {
    ws.send(JSON.stringify({ jsonrpc: "2.0", method: "session/cancel", params: { sessionId } }));
    console.log("\n[cancelling…]");
  } else ws.close();
});

while (true) {
  const text = (await ask("\nyou> ")).trim();
  if (!text) continue;
  if (text === "/exit") break;
  busy = true;
  process.stdout.write("\nhoplite> ");
  try {
    const r = await rpc("session/prompt", { sessionId, prompt: [{ type: "text", text }] });
    if (r?.stopReason && r.stopReason !== "end_turn") console.log(`\n[${r.stopReason}]`);
  } catch (e) {
    console.log(`\n[error] ${e.message}`);
  }
  busy = false;
  console.log();
}
ws.close();
