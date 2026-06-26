#!/usr/bin/env node

import { mkdirSync, writeFileSync } from "node:fs";
import { resolve } from "node:path";

const DEFAULT_STRATEGIES = [
  "random",
  "tutorial_like",
  "idle",
  "slice_clean",
  "slice_greedy",
  "singing_clean",
  "singing_risky",
  "black_red",
  "steady",
  "commercial",
  "social",
  "dance_meme",
  "recovery",
];

const TARGET_ENDINGS = {
  idle: ["UNKNOWN"],
  slice_clean: ["SLICE_SAINT"],
  slice_greedy: ["SLICE_SAINT"],
  singing_clean: ["SINGING_IDOL"],
  singing_risky: ["SINGING_IDOL", "BLACK_RED_MAIN_STAGE"],
  black_red: ["BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING"],
  steady: ["ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION"],
  commercial: ["CYBER_GIRLFRIEND"],
  social: ["DD_BUS_STOP"],
  dance_meme: ["SLICE_SAINT"],
  recovery: ["ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION"],
};

const ACTION_PREFS = {
  idle: [
    ["REST"],
  ],
  tutorial_like: [
    ["TRAIN_SONG", "STREAM_PLAN", "FAN_GROUP_MAINTAIN", "REST"],
    ["TRAIN_TALK", "PUBLISH_VIDEO", "STREAM_PLAN", "REST"],
    ["PUBLISH_VIDEO", "PUBLISH_CLIP", "FAN_GROUP_MAINTAIN", "REST"],
    ["FAN_GROUP_MAINTAIN", "TRAIN_TALK", "STREAM_PLAN", "REST"],
  ],
  slice_clean: [
    ["PUBLISH_VIDEO", "TRAIN_DANCE", "FAN_GROUP_MAINTAIN", "REST"],
    ["PUBLISH_CLIP", "PUBLISH_VIDEO", "TRAIN_DANCE", "REST"],
    ["TRAIN_DANCE", "PUBLISH_VIDEO", "FAN_GROUP_MAINTAIN", "REST"],
  ],
  slice_greedy: [
    ["PUBLISH_VIDEO", "STREAM_PLAN", "PUBLISH_CLIP", "REST"],
    ["PUBLISH_CLIP", "STREAM_PLAN", "PUBLISH_VIDEO", "REST"],
    ["STREAM_PLAN", "PUBLISH_CLIP", "PUBLISH_VIDEO", "REST"],
  ],
  singing_clean: [
    ["TRAIN_SONG", "PUBLISH_VIDEO", "REST"],
    ["TRAIN_SONG", "STREAM_PLAN", "REST"],
    ["STREAM_PLAN", "TRAIN_SONG", "FAN_GROUP_MAINTAIN", "REST"],
    ["FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"],
  ],
  singing_risky: [
    ["TRAIN_SONG", "STREAM_PLAN", "REST"],
    ["STREAM_PLAN", "TRAIN_SONG", "PUBLISH_VIDEO", "REST"],
    ["PUBLISH_VIDEO", "STREAM_PLAN", "TRAIN_SONG", "REST"],
  ],
  black_red: [
    ["STREAM_PLAN", "PUBLISH_CLIP", "NPC_INTERACT", "REST"],
    ["PUBLISH_CLIP", "STREAM_PLAN", "NPC_INTERACT", "REST"],
    ["NPC_INTERACT", "STREAM_PLAN", "PUBLISH_CLIP", "REST"],
    ["TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST"],
  ],
  steady: [
    ["TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST"],
    ["FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"],
    ["STREAM_PLAN", "TRAIN_TALK", "REST"],
    ["REST", "FAN_GROUP_MAINTAIN", "TRAIN_TALK"],
  ],
  commercial: [
    ["STREAM_PLAN", "TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST"],
    ["TRAIN_TALK", "STREAM_PLAN", "FAN_GROUP_MAINTAIN", "REST"],
    ["STREAM_PLAN", "FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"],
    ["FAN_GROUP_MAINTAIN", "STREAM_PLAN", "TRAIN_TALK", "REST"],
  ],
  social: [
    ["NPC_INTERACT", "STREAM_PLAN", "TRAIN_TALK", "REST"],
    ["STREAM_PLAN", "NPC_INTERACT", "FAN_GROUP_MAINTAIN", "REST"],
    ["NPC_INTERACT", "TRAIN_TALK", "STREAM_PLAN", "REST"],
    ["NPC_INTERACT", "FAN_GROUP_MAINTAIN", "REST"],
  ],
  dance_meme: [
    ["TRAIN_DANCE", "STREAM_PLAN", "PUBLISH_VIDEO", "REST"],
    ["PUBLISH_VIDEO", "PUBLISH_CLIP", "TRAIN_DANCE", "REST"],
    ["PUBLISH_CLIP", "PUBLISH_VIDEO", "TRAIN_DANCE", "REST"],
    ["STREAM_PLAN", "PUBLISH_VIDEO", "TRAIN_DANCE", "REST"],
    ["PUBLISH_VIDEO", "TRAIN_DANCE", "PUBLISH_CLIP", "REST"],
  ],
  recovery: [
    ["TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST"],
    ["REST", "TRAIN_TALK", "FAN_GROUP_MAINTAIN"],
    ["TRAIN_TALK", "FAN_GROUP_MAINTAIN", "REST"],
    ["STREAM_PLAN", "TRAIN_TALK", "REST"],
  ],
};

const PLAN_PREFS = {
  tutorial_like: ["TALK", "SINGING", "SHORT_CHALLENGE", "SC_THANKS"],
  slice_clean: ["SHORT_CHALLENGE", "SING_TALK", "TALK"],
  slice_greedy: ["SHORT_CHALLENGE", "SURPRISE", "MARSHMALLOW", "TALK"],
  singing_clean: ["SINGING", "SING_TALK", "TALK"],
  singing_risky: ["SINGING", "SING_TALK", "SHARP_COMMENT"],
  black_red: ["SHARP_COMMENT", "MARSHMALLOW", "SURPRISE", "TALK"],
  steady: ["TALK", "SC_THANKS", "SING_TALK"],
  commercial: ["SC_THANKS", "ENDURANCE", "TALK"],
  social: ["COLLAB", "GAME", "TALK"],
  dance_meme: ["DANCE", "SHORT_CHALLENGE", "SURPRISE"],
  recovery: ["TALK", "SC_THANKS", "SING_TALK"],
};

const TITLE_STYLE_PREFS = {
  tutorial_like: ["SAFE", "BUSINESS_SAFE", "BAIT_TRAFFIC", "ABSTRACT_MEME", "HARD_MOUTH"],
  slice_clean: ["SAFE", "BAIT_TRAFFIC", "ABSTRACT_MEME"],
  slice_greedy: ["ABSTRACT_MEME", "BAIT_TRAFFIC", "HARD_MOUTH", "SAFE"],
  singing_clean: ["SAFE", "BAIT_TRAFFIC", "ABSTRACT_MEME"],
  singing_risky: ["BAIT_TRAFFIC", "HARD_MOUTH", "ABSTRACT_MEME", "SAFE"],
  black_red: ["HARD_MOUTH", "ABSTRACT_MEME", "BAIT_TRAFFIC", "SAFE"],
  steady: ["SAFE", "BUSINESS_SAFE", "BAIT_TRAFFIC"],
  commercial: ["FAN_SERVICE", "BUSINESS_SAFE", "SAFE", "BAIT_TRAFFIC"],
  social: ["BAIT_TRAFFIC", "SAFE", "HARD_MOUTH"],
  dance_meme: ["ABSTRACT_MEME", "BAIT_TRAFFIC", "SAFE"],
  recovery: ["SAFE", "BUSINESS_SAFE", "BAIT_TRAFFIC"],
};

const OFFSTREAM_PREFS = {
  idle: [],
  tutorial_like: ["READ_LETTERS", "SONG_SELECTION", "CLIP_SCOUTING", "TREND_WATCH"],
  slice_clean: ["CLIP_SCOUTING", "THUMBNAIL_DESIGN", "SHORT_VIDEO_IDEA", "READ_LETTERS"],
  slice_greedy: ["MEME_RESEARCH", "SHORT_VIDEO_IDEA", "CLIP_SCOUTING", "THUMBNAIL_DESIGN"],
  singing_clean: ["SONG_SELECTION", "READ_LETTERS", "TREND_WATCH"],
  singing_risky: ["SONG_SELECTION", "BROWSE_SOCIAL", "MEME_RESEARCH"],
  black_red: ["CRISIS_PR", "TREND_WATCH", "MEME_RESEARCH", "BROWSE_SOCIAL"],
  steady: ["READ_LETTERS", "TREND_WATCH", "SONG_SELECTION"],
  commercial: ["CHAT_ROOM", "THUMBNAIL_DESIGN", "READ_LETTERS", "ORGANIZE_MATERIALS"],
  social: ["COLLAB_PLAN", "DM_MAINTAIN", "BROWSE_SOCIAL"],
  dance_meme: ["SHORT_VIDEO_IDEA", "MEME_RESEARCH", "CLIP_SCOUTING", "THUMBNAIL_DESIGN"],
  recovery: ["READ_LETTERS", "TREND_WATCH", "SONG_SELECTION", "CHAT_ROOM"],
};

const EVENT_PREFS = {
  idle: ["safe", "traffic", "meme"],
  tutorial_like: ["safe", "meme", "traffic"],
  slice_clean: ["safe", "meme", "traffic"],
  slice_greedy: ["meme", "traffic", "safe"],
  singing_clean: ["safe", "traffic", "meme"],
  singing_risky: ["traffic", "meme", "safe"],
  black_red: ["traffic", "meme", "safe"],
  steady: ["safe", "traffic", "meme"],
  commercial: ["safe", "traffic", "meme"],
  social: ["safe", "traffic", "meme"],
  dance_meme: ["meme", "traffic", "safe"],
  recovery: ["safe", "traffic", "meme"],
};

const NPC_PREFS = {
  black_red: ["BORROW_HEAT", "RAID", "AVOID"],
  social: ["COLLAB", "RAID", "RAID", "AVOID"],
  recovery: ["AVOID", "RAID"],
};

const POLICY_DRIFT = {
  steady: [
    { mode: "focused", effectiveStrategy: "steady", slots: 9 },
    { mode: "commercial_drift", effectiveStrategy: "commercial", slots: 1 },
  ],
  slice_clean: [
    { mode: "focused", effectiveStrategy: "slice_clean", slots: 8 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 1 },
    { mode: "song_drift", effectiveStrategy: "singing_clean", slots: 1 },
  ],
  slice_greedy: [
    { mode: "focused", effectiveStrategy: "slice_greedy", slots: 7 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 2 },
    { mode: "burnout_drift", effectiveStrategy: "recovery", slots: 1 },
  ],
  singing_clean: [
    { mode: "focused", effectiveStrategy: "singing_clean", slots: 8 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 1 },
    { mode: "clip_drift", effectiveStrategy: "slice_clean", slots: 1 },
  ],
  singing_risky: [
    { mode: "focused", effectiveStrategy: "singing_risky", slots: 7 },
    { mode: "black_red_drift", effectiveStrategy: "black_red", slots: 1 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 2 },
  ],
  black_red: [
    { mode: "focused", effectiveStrategy: "black_red", slots: 7 },
    { mode: "pr_cleanup", effectiveStrategy: "recovery", slots: 1 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 2 },
  ],
  social: [
    { mode: "focused", effectiveStrategy: "social", slots: 8 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 1 },
    { mode: "song_drift", effectiveStrategy: "singing_clean", slots: 1 },
  ],
  commercial: [
    { mode: "focused", effectiveStrategy: "commercial", slots: 8 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 1 },
    { mode: "social_drift", effectiveStrategy: "social", slots: 1 },
  ],
  dance_meme: [
    { mode: "focused", effectiveStrategy: "dance_meme", slots: 7 },
    { mode: "safe_drift", effectiveStrategy: "steady", slots: 2 },
    { mode: "song_drift", effectiveStrategy: "singing_clean", slots: 1 },
  ],
  recovery: [
    { mode: "focused", effectiveStrategy: "recovery", slots: 9 },
    { mode: "traffic_relapse", effectiveStrategy: "black_red", slots: 1 },
  ],
};

const TARGET_HIT_MIN = 0.35;
const TARGET_HIT_MAX = 0.9;
const MIN_BASELINE_RUNS = 30;

function parseArgs(argv) {
  const args = {
    baseUrl: "http://localhost:18080",
    runs: 2,
    strategies: DEFAULT_STRATEGIES,
    outDir: "target/balance",
    seed: "balance",
    maxDays: 30,
    concurrency: 2,
    waitMs: 1000,
  };

  for (let index = 0; index < argv.length; index += 1) {
    const arg = argv[index];
    const [key, inlineValue] = arg.includes("=") ? arg.split(/=(.*)/s, 2) : [arg, null];
    const nextValue = () => inlineValue ?? argv[++index];

    switch (key) {
      case "--base-url":
        args.baseUrl = stripTrailingSlash(nextValue());
        break;
      case "--runs":
        args.runs = positiveInt(nextValue(), "--runs");
        break;
      case "--strategies":
        args.strategies = splitCsv(nextValue());
        break;
      case "--out":
      case "--out-dir":
        args.outDir = nextValue();
        break;
      case "--seed":
        args.seed = String(nextValue());
        break;
      case "--max-days":
        args.maxDays = positiveInt(nextValue(), "--max-days");
        break;
      case "--concurrency":
        args.concurrency = positiveInt(nextValue(), "--concurrency");
        break;
      case "--wait-ms":
        args.waitMs = positiveInt(nextValue(), "--wait-ms");
        break;
      case "--help":
      case "-h":
        printHelp();
        process.exit(0);
        break;
      default:
        throw new Error(`Unknown argument: ${arg}`);
    }
  }

  const unknown = args.strategies.filter((name) => !DEFAULT_STRATEGIES.includes(name));
  if (unknown.length) {
    throw new Error(`Unknown strategies: ${unknown.join(", ")}`);
  }
  return args;
}

function printHelp() {
  console.log(`Usage:
  node scripts/balance-sim.mjs [options]

Options:
  --base-url http://localhost:18080
  --runs 3
  --strategies singing_clean,slice_clean,random
  --concurrency 2
  --seed balance
  --out target/balance

The script drives the real HTTP API. Start the manual app first, for example:
  scripts\\run-latest-manual.cmd -Port 18080
`);
}

function positiveInt(raw, name) {
  const value = Number.parseInt(raw, 10);
  if (!Number.isFinite(value) || value < 1) {
    throw new Error(`${name} must be a positive integer.`);
  }
  return value;
}

function splitCsv(raw) {
  return String(raw)
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function stripTrailingSlash(raw) {
  return String(raw || "").replace(/\/+$/, "");
}

class ApiClient {
  constructor(baseUrl) {
    this.baseUrl = stripTrailingSlash(baseUrl);
    this.cookies = new Map();
  }

  async get(path) {
    return this.request("GET", path);
  }

  async post(path, body = {}) {
    return this.request("POST", path, body);
  }

  async request(method, path, body) {
    const headers = {};
    if (this.cookies.size) {
      headers.Cookie = [...this.cookies.entries()].map(([key, value]) => `${key}=${value}`).join("; ");
    }
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    const response = await fetch(`${this.baseUrl}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    this.captureCookies(response.headers);

    const text = await response.text();
    let payload = null;
    if (text) {
      try {
        payload = JSON.parse(text);
      } catch {
        payload = null;
      }
    }

    if (!response.ok) {
      throw new Error(`${method} ${path} failed with HTTP ${response.status}: ${text.slice(0, 500)}`);
    }
    if (payload && payload.success === false) {
      const code = payload.code ? `${payload.code}: ` : "";
      throw new Error(`${method} ${path} failed: ${code}${payload.message || "unknown API error"}`);
    }
    return payload && Object.hasOwn(payload, "data") ? payload.data : payload;
  }

  captureCookies(headers) {
    const raw = typeof headers.getSetCookie === "function"
      ? headers.getSetCookie()
      : [headers.get("set-cookie")].filter(Boolean);
    for (const header of raw) {
      for (const cookieText of splitSetCookie(header)) {
        const [pair] = cookieText.split(";");
        const eq = pair.indexOf("=");
        if (eq > 0) {
          this.cookies.set(pair.slice(0, eq).trim(), pair.slice(eq + 1).trim());
        }
      }
    }
  }
}

function splitSetCookie(header) {
  if (!header || !header.includes(",")) {
    return header ? [header] : [];
  }
  return header.split(/,(?=\s*[^;,=\s]+=[^;,]+)/g).map((item) => item.trim());
}

function makeRng(seedText) {
  let h = 2166136261 >>> 0;
  for (let index = 0; index < seedText.length; index += 1) {
    h ^= seedText.charCodeAt(index);
    h = Math.imul(h, 16777619);
  }
  return () => {
    h += 0x6D2B79F5;
    let t = h;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function pickRandom(items, rng) {
  if (!items.length) return null;
  return items[Math.floor(rng() * items.length)];
}

function policyFor(strategy, runNumber) {
  const policies = POLICY_DRIFT[strategy];
  if (!policies || !policies.length) {
    return { mode: "focused", effectiveStrategy: strategy };
  }
  const totalSlots = sum(policies.map((policy) => policy.slots));
  let slot = (runNumber - 1) % totalSlots;
  for (const policy of policies) {
    if (slot < policy.slots) {
      return {
        mode: policy.mode,
        effectiveStrategy: policy.effectiveStrategy,
      };
    }
    slot -= policy.slots;
  }
  return {
    mode: "focused",
    effectiveStrategy: strategy,
  };
}

function cycleFor(strategy, day) {
  const prefs = ACTION_PREFS[strategy] || ACTION_PREFS.tutorial_like;
  return prefs[(day - 1) % prefs.length];
}

function unique(items) {
  return [...new Set(items.filter(Boolean))];
}

function pressureFor(vup) {
  const debts = vup?.debts || [];
  return {
    stamina: Number(vup?.resources?.stamina || 0),
    watchHeat: Number(vup?.opinion?.watchHeat || 0),
    reputation: Number(vup?.opinion?.reputation || 100),
    openDebtCount: debts.length,
    severeDebtCount: debts.filter((debt) => Number(debt.severity || 0) >= 4 || debt.urgent).length,
  };
}

function recoveryMode(vup) {
  const pressure = pressureFor(vup);
  return pressure.openDebtCount > 0
    || pressure.watchHeat >= 65
    || pressure.reputation < 45
    || pressure.stamina <= 2;
}

function severeRecoveryMode(vup) {
  const pressure = pressureFor(vup);
  return pressure.stamina <= 1 || pressure.severeDebtCount > 0;
}

function recoveryPrefsFor(strategy, vup) {
  if (strategy === "black_red" || strategy === "slice_greedy") {
    return null;
  }
  if (strategy === "slice_clean") {
    const pressure = pressureFor(vup);
    return pressure.stamina <= 1 ? ["REST"] : null;
  }
  if (["commercial", "social", "dance_meme"].includes(strategy)) {
    const pressure = pressureFor(vup);
    if (pressure.stamina <= 1) {
      return ["REST"];
    }
    if (strategy === "commercial" && pressure.severeDebtCount > 0 && pressure.reputation < 55) {
      return ["FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"];
    }
    if (strategy === "social" && (pressure.severeDebtCount >= 3 || pressure.watchHeat >= 125)) {
      return ["REST", "NPC_INTERACT", "STREAM_PLAN"];
    }
    if (strategy === "dance_meme" && (pressure.severeDebtCount >= 3 || pressure.watchHeat >= 105)) {
      return ["REST", "TRAIN_DANCE", "PUBLISH_VIDEO"];
    }
    return null;
  }
  if (strategy === "recovery" && recoveryMode(vup)) {
    const pressure = pressureFor(vup);
    return pressure.severeDebtCount > 0 || pressure.reputation < 45
      ? ["FAN_GROUP_MAINTAIN", "TRAIN_TALK", "REST"]
      : ["TRAIN_TALK", "REST", "FAN_GROUP_MAINTAIN"];
  }
  return recoveryMode(vup) ? ["TRAIN_TALK", "REST", "FAN_GROUP_MAINTAIN"] : null;
}

function actionPrefsFor(strategy, context) {
  const recoveryPrefs = recoveryPrefsFor(strategy, context.vup);
  const prefs = recoveryPrefs
    ? [...recoveryPrefs, ...cycleFor(strategy, context.day)]
    : cycleFor(strategy, context.day);
  return unique(prefs);
}

function chooseAction(strategy, context) {
  const enabled = context.actions.filter((action) => action.enabled);
  if (!enabled.length) {
    throw new Error(`No enabled action on day ${context.day}.`);
  }
  if (strategy === "random") {
    return pickRandom(enabled, context.rng);
  }

  const prefs = actionPrefsFor(strategy, context);
  const byType = new Map(context.actions.map((action) => [action.actionType, action]));
  for (const type of prefs) {
    const action = byType.get(type);
    if (action?.enabled) {
      return action;
    }
  }
  return enabled.find((action) => action.actionType === "REST") || enabled[0];
}

function choosePlanType(strategy, day, plans, rng) {
  if (!plans.length) {
    throw new Error("No stream plans returned by /api/stream/plans.");
  }
  if (strategy === "random") {
    return pickRandom(plans, rng).planType;
  }
  const prefs = PLAN_PREFS[strategy] || PLAN_PREFS.tutorial_like;
  for (const planType of prefs) {
    if (plans.some((plan) => plan.planType === planType)) {
      return planType;
    }
  }
  return plans[0].planType;
}

function chooseTitle(strategy, titles, rng) {
  const enabled = titles.filter(Boolean);
  if (!enabled.length) {
    throw new Error("No title candidates returned by /api/stream/titles.");
  }
  if (strategy === "random") {
    return pickRandom(enabled, rng);
  }
  for (const style of TITLE_STYLE_PREFS[strategy] || TITLE_STYLE_PREFS.tutorial_like) {
    const match = enabled.find((title) => title.style === style);
    if (match) return match;
  }
  return enabled[0];
}

function chooseOffStream(strategy, options, rng) {
  if (strategy === "idle") {
    return null;
  }
  const enabled = options.filter((option) => option.enabled !== false);
  if (!enabled.length) return null;
  if (strategy === "random") {
    return pickRandom(enabled, rng);
  }
  for (const type of OFFSTREAM_PREFS[strategy] || OFFSTREAM_PREFS.tutorial_like) {
    const match = enabled.find((option) => option.type === type || option.routeBias === type);
    if (match) return match;
  }
  return enabled.find((option) => option.recommended) || enabled[0];
}

function choicePrefsFor(strategy, vup) {
  const pressure = pressureFor(vup);
  if (strategy === "dance_meme") {
    if (pressure.severeDebtCount >= 3 || pressure.watchHeat >= 105) {
      return ["safe", "traffic", "meme"];
    }
    if (pressure.openDebtCount >= 3 || pressure.watchHeat >= 85) {
      return ["traffic", "safe", "meme"];
    }
  }
  if (strategy === "social" && pressure.watchHeat >= 100) {
    return ["safe", "traffic", "meme"];
  }
  return EVENT_PREFS[strategy] || EVENT_PREFS.tutorial_like;
}

function chooseChoice(strategy, choices, rng, context = {}) {
  const enabled = choices.filter((choice) => choice.enabled !== false);
  if (!enabled.length) {
    throw new Error("No enabled event/interaction choice.");
  }
  if (strategy === "random") {
    return pickRandom(enabled, rng);
  }
  for (const choiceType of choicePrefsFor(strategy, context.vup)) {
    const match = enabled.find((choice) => choice.choiceType === choiceType || choice.choiceId === choiceType);
    if (match) return match;
  }
  return enabled[0];
}

function chooseNpcTendency(strategy, day, vup) {
  if (strategy === "social") {
    const pressure = pressureFor(vup);
    if (pressure.reputation < 55 || pressure.watchHeat >= 130 || pressure.severeDebtCount >= 3) {
      return "AVOID";
    }
    if (pressure.watchHeat >= 95 || pressure.severeDebtCount > 0) {
      return "RAID";
    }
    return (day - 1) % 3 === 0 ? "COLLAB" : "RAID";
  }
  const prefs = NPC_PREFS[strategy] || ["RAID", "AVOID"];
  return prefs[(day - 1) % prefs.length];
}

function idempotency(prefix, runId, day, extra = "") {
  return `${prefix}-${runId}-d${day}${extra ? `-${extra}` : ""}`;
}

async function bootstrap(client, runId, strategy) {
  const password = "pass1234";
  const username = `balance_${strategy}_${runId}`.replace(/[^a-zA-Z0-9_]/g, "_").slice(0, 60);
  await client.post("/api/auth/register", {
    username,
    password,
    nickname: `balance ${strategy}`,
  });
  await client.post("/api/auth/login", { username, password });
  return client.post("/api/vup/create", {
    name: `Balance${runId}`.slice(0, 64),
    persona: personaFor(strategy),
  });
}

function personaFor(strategy) {
  if (strategy.includes("singing")) return "low-pressure singing rookie";
  if (strategy.includes("slice") || strategy === "dance_meme") return "short video and meme-driven rookie";
  if (strategy === "black_red") return "sharp-tongued traffic-chasing rookie";
  if (strategy === "commercial") return "fan-service and commercial scheduling rookie";
  if (strategy === "social") return "collab-friendly DD magnet rookie";
  if (strategy === "steady" || strategy === "recovery") return "stable talk and fan-room maintenance rookie";
  return "curious all-rounder rookie";
}

async function runOne({ baseUrl, strategy, runNumber, seed, maxDays }) {
  const runId = `${Date.now().toString(36)}_${runNumber}_${Math.floor(Math.random() * 100000)}`;
  const rng = makeRng(`${seed}:${strategy}:${runNumber}`);
  const client = new ApiClient(baseUrl);
  const policy = policyFor(strategy, runNumber);
  const effectiveStrategy = policy.effectiveStrategy;

  const result = {
    runId,
    seed,
    strategy,
    effectiveStrategy,
    policyMode: policy.mode,
    runNumber,
    startedAt: new Date().toISOString(),
    daysPlayed: 0,
    actions: [],
    snapshots: {},
    actionCounts: {},
    titleStyleCounts: {},
    planCounts: {},
    offstreamCounts: {},
    eventChoiceCounts: {},
    interactionChoiceCounts: {},
    disabledActionCounts: {},
    fallbackCounts: {},
    fallbackReasonCounts: {},
    debtOpenedCount: 0,
    debtClosedOrExpiredCount: 0,
    formalEventCount: 0,
    interactionCount: 0,
    reportCount: 0,
    errors: [],
  };

  let previousOpenDebtIds = new Set();
  let vup = await bootstrap(client, runId, strategy);
  previousOpenDebtIds = debtIds(vup);

  for (let guard = 0; guard < maxDays * 8; guard += 1) {
    let session = await client.get("/api/day/session");
    vup = await client.get("/api/vup/current");
    syncDebtCounters(result, previousOpenDebtIds, debtIds(vup));
    previousOpenDebtIds = debtIds(vup);

    if (session.phase === "ENDING_READY") {
      await captureFinal(client, result, vup, session);
      break;
    }
    if (session.day > maxDays) {
      break;
    }

    if (session.phase === "READY") {
      await playReadyPhase(client, result, effectiveStrategy, session, vup, rng);
      continue;
    }
    if (session.phase === "NEED_TITLE") {
      await playTitlePhase(client, result, effectiveStrategy, session, rng);
      continue;
    }
    if (session.phase === "OFF_STREAM_READY") {
      await playOffStreamPhase(client, result, effectiveStrategy, session, rng);
      continue;
    }
    if (session.phase === "NEED_INTERACTION_CHOICE") {
      await playInteractionPhase(client, result, effectiveStrategy, session, rng);
      continue;
    }
    if (session.phase === "NEED_EVENT_CHOICE") {
      await playEventPhase(client, result, effectiveStrategy, session, rng);
      continue;
    }
    if (session.phase === "REPORT_READY") {
      await finishReportPhase(client, result, session);
      continue;
    }

    throw new Error(`Unsupported phase ${session.phase} on day ${session.day}.`);
  }

  if (!result.ending) {
    const session = await client.get("/api/day/session");
    const current = await client.get("/api/vup/current");
    await captureFinal(client, result, current, session);
  }

  result.finishedAt = new Date().toISOString();
  return result;
}

async function playReadyPhase(client, result, strategy, session, vup, rng) {
  const actions = await client.get("/api/actions");
  for (const action of actions) {
    if (!action.enabled && action.disabledReason) {
      inc(result.disabledActionCounts, `${action.actionType}:${action.disabledReason}`);
    }
  }

  const actionContext = { day: session.day, vup, actions, rng };
  const selected = chooseAction(strategy, actionContext);
  const prefs = strategy === "random" ? [selected.actionType] : actionPrefsFor(strategy, actionContext);
  const topPreferred = prefs[0];
  if (topPreferred && topPreferred !== selected.actionType) {
    inc(result.fallbackCounts, `${topPreferred}->${selected.actionType}`);
    const preferred = actions.find((action) => action.actionType === topPreferred);
    const reason = preferred?.disabledReason || (preferred?.enabled ? "LOWER_PRIORITY_AVAILABLE" : "NOT_RETURNED");
    inc(result.fallbackReasonCounts, `${topPreferred}:${reason}`);
  }

  let planType = null;
  if (selected.actionType === "STREAM_PLAN") {
    const plans = await client.get("/api/stream/plans");
    planType = choosePlanType(strategy, session.day, plans, rng);
    inc(result.planCounts, planType);
  } else if (selected.actionType === "NPC_INTERACT") {
    planType = chooseNpcTendency(strategy, session.day, vup);
  }

  const response = await client.post("/api/day/action", {
    actionType: selected.actionType,
    planType,
    idempotencyKey: idempotency("action", result.runId, session.day, selected.actionType),
  });
  recordActionResult(result, session.day, selected.actionType, planType, response?.actionResult);
}

async function playTitlePhase(client, result, strategy, session, rng) {
  const titles = await client.get("/api/stream/titles");
  const title = chooseTitle(strategy, titles, rng);
  inc(result.titleStyleCounts, title.style || "UNKNOWN");
  const response = await client.post("/api/stream/title/choose", {
    titleTemplateId: title.id,
    idempotencyKey: idempotency("title", result.runId, session.day, String(title.id)),
  });
  const last = result.actions[result.actions.length - 1];
  if (last && last.day === session.day) {
    last.titleId = title.id;
    last.titleStyle = title.style;
    last.titleText = title.titleText;
  }
  recordActionResult(result, session.day, "STREAM_TITLE", title.style, response?.actionResult);
}

async function playOffStreamPhase(client, result, strategy, session, rng) {
  const options = await client.get("/api/offstream/options");
  const choice = chooseOffStream(strategy, options, rng);
  if (!choice) {
    await client.post("/api/offstream/skip");
    inc(result.offstreamCounts, "SKIP");
    return;
  }
  inc(result.offstreamCounts, choice.type || "UNKNOWN");
  const response = await client.post("/api/offstream/action", { offStreamType: choice.type });
  recordActionResult(result, session.day, "OFFSTREAM", choice.type, response?.actionResult);
}

async function playInteractionPhase(client, result, strategy, session, rng) {
  const pending = await client.get("/api/interaction/pending");
  const vup = await client.get("/api/vup/current");
  const choice = chooseChoice(strategy, pending?.choices || [], rng, { vup });
  inc(result.interactionChoiceCounts, choice.choiceType || choice.choiceId || "UNKNOWN");
  result.interactionCount += 1;
  const response = await client.post("/api/interaction/choose", {
    choiceType: choice.choiceType,
    idempotencyKey: idempotency("interaction", result.runId, session.day, choice.choiceType),
  });
  recordActionResult(result, session.day, "INTERACTION", choice.choiceType, response?.actionResult);
}

async function playEventPhase(client, result, strategy, session, rng) {
  const pending = await client.get("/api/event/pending");
  const vup = await client.get("/api/vup/current");
  const choice = chooseChoice(strategy, pending?.choices || [], rng, { vup });
  const choiceKey = choice.choiceId || choice.choiceType;
  inc(result.eventChoiceCounts, choice.choiceType || choiceKey || "UNKNOWN");
  result.formalEventCount += 1;
  const response = await client.post("/api/event/choose", {
    choiceId: choiceKey,
    choiceType: choice.choiceType,
    idempotencyKey: idempotency("event", result.runId, session.day, choiceKey),
  });
  recordActionResult(result, session.day, "EVENT", choice.choiceType || choiceKey, response?.actionResult);
}

async function finishReportPhase(client, result, session) {
  const report = await client.get("/api/report/today");
  result.reportCount += 1;
  result.daysPlayed = Math.max(result.daysPlayed, session.day);
  const vup = await client.get("/api/vup/current");
  if ([7, 14, 21, 30].includes(session.day)) {
    result.snapshots[String(session.day)] = snapshotFrom(vup, report);
  }
  if (session.day >= Number(vup.maxDay || 30)) {
    return;
  }
  await client.post("/api/day/next", {
    idempotencyKey: idempotency("next", result.runId, session.day),
  });
}

async function captureFinal(client, result, vup, session) {
  const finalSnapshot = snapshotFrom(vup, null);
  result.finalState = finalSnapshot;
  result.finalSession = session;
  const maxDay = Number(vup?.maxDay || 30);
  const finalDay = Number(session?.day || vup?.day || finalSnapshot.day || maxDay);
  if (finalDay >= maxDay || session?.phase === "ENDING_READY") {
    result.snapshots[String(maxDay)] = finalSnapshot;
  }
  if (session.phase === "ENDING_READY") {
    try {
      const review = await client.get("/api/ending/review");
      result.ending = review?.endingType || "UNKNOWN";
      result.endingReview = review;
      result.failureReason = failureReasonFromReview(review);
    } catch (error) {
      result.errors.push(`ending review: ${error.message}`);
      result.ending = vup?.currentRoute || "UNKNOWN";
    }
  } else {
    try {
      const forecast = await client.get("/api/ending/forecast");
      result.ending = forecast?.ifEndedNowType || forecast?.likelyEndingType || vup?.currentRoute || "UNKNOWN";
      result.endingForecast = forecast;
      result.failureReason = forecast?.targetReason || "";
    } catch {
      result.ending = vup?.currentRoute || "UNKNOWN";
    }
  }
}

function recordActionResult(result, day, actionType, detail, actionResult) {
  if (actionType !== "STREAM_TITLE" && actionType !== "OFFSTREAM" && actionType !== "INTERACTION" && actionType !== "EVENT") {
    result.actions.push({ day, actionType, detail });
    inc(result.actionCounts, actionType);
  }
  if (actionResult?.debtCreated?.length) {
    result.debtOpenedCount += actionResult.debtCreated.length;
  }
  const last = result.actions[result.actions.length - 1];
  if (!last || last.day !== day) return;
  if (actionType === "STREAM_TITLE") {
    last.titleStyle = detail;
  } else if (actionType === "OFFSTREAM") {
    last.offstream = detail;
  } else if (actionType === "INTERACTION") {
    last.interactionChoice = detail;
  } else if (actionType === "EVENT") {
    last.eventChoice = detail;
  }
  if (actionResult) {
    last.result = compactActionResult(actionResult);
  }
}

function compactActionResult(actionResult) {
  return {
    actionType: actionResult.actionType,
    fanChange: actionResult.fanChange,
    routeScoreChange: actionResult.routeScoreChange,
    staminaChange: actionResult.staminaChange,
    debtCreated: actionResult.debtCreated || [],
    fatigueInfo: actionResult.fatigueInfo || null,
    evidenceType: actionResult.evidenceRef?.type,
  };
}

function snapshotFrom(vup, report) {
  return {
    day: vup?.day,
    phase: vup?.phase,
    currentRoute: vup?.currentRoute,
    fans: vup?.fanStructure?.fans,
    fanStructure: vup?.fanStructure || {},
    opinion: vup?.opinion || {},
    resources: vup?.resources || {},
    attributes: vup?.attributes || {},
    routeScore: vup?.route?.routeScore || {},
    openDebtCount: (vup?.debts || []).length,
    urgentDebtCount: (vup?.debts || []).filter((debt) => debt.urgent).length,
    reportDelta: report?.dataDelta || null,
    riskHint: report?.riskHint || null,
  };
}

function debtIds(vup) {
  return new Set((vup?.debts || []).map((debt) => String(debt.id)));
}

function syncDebtCounters(result, previous, current) {
  for (const id of current) {
    if (!previous.has(id)) result.debtOpenedCount += 1;
  }
  for (const id of previous) {
    if (!current.has(id)) result.debtClosedOrExpiredCount += 1;
  }
}

function failureReasonFromReview(review) {
  const reasonJson = review?.endingReasonJson || {};
  return reasonJson.conditions?.unknownFailureReason
    || reasonJson.rule
    || review?.endingReason
    || "";
}

function inc(bucket, key, amount = 1) {
  const safeKey = key || "UNKNOWN";
  bucket[safeKey] = (bucket[safeKey] || 0) + amount;
}

async function waitForReady(baseUrl, waitMs) {
  const started = Date.now();
  let lastError = null;
  do {
    try {
      const response = await fetch(`${baseUrl}/`);
      if (response.ok) return;
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await sleep(Math.min(500, waitMs));
  } while (Date.now() - started < waitMs);
  throw new Error(`Server is not ready at ${baseUrl}. Start it first, e.g. scripts\\run-latest-manual.cmd -Port ${portFrom(baseUrl)}. Last error: ${lastError?.message}`);
}

function sleep(ms) {
  return new Promise((resolvePromise) => setTimeout(resolvePromise, ms));
}

function portFrom(baseUrl) {
  try {
    return new URL(baseUrl).port || "18080";
  } catch {
    return "18080";
  }
}

async function runWithConcurrency(items, limit, worker) {
  const results = new Array(items.length);
  let next = 0;
  const workers = Array.from({ length: Math.min(limit, items.length) }, async () => {
    while (next < items.length) {
      const current = next;
      next += 1;
      results[current] = await worker(items[current], current);
    }
  });
  await Promise.all(workers);
  return results;
}

function summarize(runs) {
  const byStrategy = groupBy(runs, (run) => run.strategy);
  const strategies = {};
  for (const [strategy, items] of Object.entries(byStrategy)) {
    const fans = items.map((run) => run.finalState?.fans || 0);
    const endings = countBy(items, (run) => run.ending || "UNKNOWN");
    const target = TARGET_ENDINGS[strategy] || [];
    const targetHits = target.length
      ? items.filter((run) => target.includes(run.ending)).length
      : null;
    strategies[strategy] = {
      runs: items.length,
      targetEndings: target,
      targetHitRate: targetHits == null ? null : round(targetHits / items.length, 4),
      endingDistribution: endings,
      fans: stats(fans),
      popularity: stats(items.map((run) => run.finalState?.opinion?.popularity || 0)),
      reputation: stats(items.map((run) => run.finalState?.opinion?.reputation || 0)),
      watchHeat: stats(items.map((run) => run.finalState?.opinion?.watchHeat || 0)),
      openDebts: stats(items.map((run) => run.finalState?.openDebtCount || 0)),
      debtOpenedAvg: round(avg(items.map((run) => run.debtOpenedCount || 0)), 2),
      debtClosedOrExpiredAvg: round(avg(items.map((run) => run.debtClosedOrExpiredCount || 0)), 2),
      formalEventHitRate: round(sum(items.map((run) => run.formalEventCount || 0)) / Math.max(1, sum(items.map((run) => run.daysPlayed || 0))), 4),
      routeScoreMax: aggregateRouteMax(items),
      snapshots: summarizeSnapshots(items),
      actionCounts: mergeCounts(items.map((run) => run.actionCounts)),
      titleStyleCounts: mergeCounts(items.map((run) => run.titleStyleCounts)),
      policyModeCounts: mergeCounts(items.map((run) => ({
        [`${run.policyMode || "focused"}:${run.effectiveStrategy || run.strategy}`]: 1,
      }))),
      disabledActionCounts: mergeCounts(items.map((run) => run.disabledActionCounts)),
      fallbackCounts: mergeCounts(items.map((run) => run.fallbackCounts)),
      fallbackReasonCounts: mergeCounts(items.map((run) => run.fallbackReasonCounts)),
      topFailureReasons: topCounts(countBy(
        items.filter((run) => target.length && !target.includes(run.ending)),
        (run) => run.failureReason || "NO_REASON"
      ), 5),
    };
  }

  const summary = {
    generatedAt: new Date().toISOString(),
    totalRuns: runs.length,
    strategies,
    dominanceWarnings: dominanceWarnings(strategies),
  };
  summary.validation = validateBaseline(summary, runs);
  return summary;
}

function validateBaseline(summary, runs) {
  const checks = [];
  const add = (id, label, passed, details, skipped = false) => {
    checks.push({
      id,
      label,
      status: skipped ? "SKIP" : passed ? "PASS" : "FAIL",
      passed: skipped ? true : Boolean(passed),
      details,
    });
  };

  const failedRuns = runs.filter((run) => run.ending === "ERROR");
  add(
    "no-http-errors",
    "All simulated runs completed without HTTP/API errors",
    failedRuns.length === 0,
    failedRuns.length ? `${failedRuns.length} run(s) failed` : "no failed runs"
  );

  for (const [strategy, data] of Object.entries(summary.strategies)) {
    add(
      `min-runs-${strategy}`,
      `${strategy} has at least ${MIN_BASELINE_RUNS} runs`,
      data.runs >= MIN_BASELINE_RUNS,
      `${data.runs}/${MIN_BASELINE_RUNS}`
    );
  }

  for (const [strategy, data] of Object.entries(summary.strategies)) {
    if (!data.targetEndings.length || strategy === "idle") {
      continue;
    }
    const inRange = data.targetHitRate >= TARGET_HIT_MIN && data.targetHitRate <= TARGET_HIT_MAX;
    add(
      `target-hit-range-${strategy}`,
      `${strategy} target hit rate is ${percent(TARGET_HIT_MIN)}-${percent(TARGET_HIT_MAX)}`,
      inRange,
      `${percent(data.targetHitRate)} for ${data.targetEndings.join(" / ")}`
    );

    const maxEndingHits = Math.max(0, ...Object.values(data.endingDistribution || {}));
    add(
      `no-single-ending-lock-${strategy}`,
      `${strategy} does not produce one ending in 100% of runs`,
      maxEndingHits < data.runs,
      `${maxEndingHits}/${data.runs} on the most frequent ending`
    );
  }

  const idle = summary.strategies.idle;
  if (idle) {
    const unknownRate = (idle.endingDistribution.UNKNOWN || 0) / Math.max(1, idle.runs);
    add(
      "idle-unknown",
      "idle reasonably reaches UNKNOWN",
      unknownRate >= 0.8,
      `${percent(unknownRate)} UNKNOWN`
    );
  } else {
    add(
      "idle-unknown",
      "idle reasonably reaches UNKNOWN",
      true,
      "idle not included in this simulation command",
      true
    );
  }

  const blackRed = summary.strategies.black_red;
  const steady = summary.strategies.steady;
  if (blackRed && steady) {
    add(
      "high-risk-higher-score",
      "high-risk black_red earns more fans than steady",
      blackRed.fans.avg > steady.fans.avg,
      `black_red fans avg ${blackRed.fans.avg}, steady fans avg ${steady.fans.avg}`
    );
    add(
      "high-risk-more-debt",
      "high-risk black_red opens more debt than steady",
      blackRed.debtOpenedAvg > steady.debtOpenedAvg && blackRed.openDebts.avg > steady.openDebts.avg,
      `black_red debt opened avg ${blackRed.debtOpenedAvg}, open ${blackRed.openDebts.avg}; steady debt opened avg ${steady.debtOpenedAvg}, open ${steady.openDebts.avg}`
    );
    add(
      "defense-stable-not-highest",
      "steady is stable but not the highest scoring route",
      steady.targetHitRate <= TARGET_HIT_MAX && steady.fans.avg < blackRed.fans.avg,
      `steady target hit ${percent(steady.targetHitRate)}, steady fans avg ${steady.fans.avg}, black_red fans avg ${blackRed.fans.avg}`
    );
  } else {
    add(
      "high-risk-vs-steady",
      "black_red vs steady risk curve comparison",
      true,
      "black_red and steady were not both included",
      true
    );
  }

  const failed = checks.filter((check) => check.status === "FAIL");
  return {
    allPassed: failed.length === 0,
    failedCount: failed.length,
    checks,
  };
}

function groupBy(items, keyFn) {
  return items.reduce((acc, item) => {
    const key = keyFn(item);
    (acc[key] ||= []).push(item);
    return acc;
  }, {});
}

function countBy(items, keyFn) {
  return items.reduce((acc, item) => {
    inc(acc, keyFn(item));
    return acc;
  }, {});
}

function mergeCounts(items) {
  const merged = {};
  for (const item of items) {
    for (const [key, value] of Object.entries(item || {})) {
      inc(merged, key, value);
    }
  }
  return Object.fromEntries(Object.entries(merged).sort((left, right) => right[1] - left[1]));
}

function topCounts(counts, limit) {
  return Object.fromEntries(Object.entries(counts).sort((left, right) => right[1] - left[1]).slice(0, limit));
}

function stats(values) {
  const nums = values.filter((value) => Number.isFinite(value)).sort((a, b) => a - b);
  if (!nums.length) {
    return { min: 0, max: 0, avg: 0, median: 0 };
  }
  return {
    min: nums[0],
    max: nums[nums.length - 1],
    avg: round(avg(nums), 2),
    median: round(median(nums), 2),
  };
}

function avg(values) {
  return values.length ? sum(values) / values.length : 0;
}

function sum(values) {
  return values.reduce((total, value) => total + Number(value || 0), 0);
}

function median(values) {
  if (!values.length) return 0;
  const mid = Math.floor(values.length / 2);
  return values.length % 2 ? values[mid] : (values[mid - 1] + values[mid]) / 2;
}

function round(value, digits) {
  const scale = 10 ** digits;
  return Math.round(value * scale) / scale;
}

function percent(value) {
  if (!Number.isFinite(value)) {
    return "-";
  }
  return `${Math.round(value * 100)}%`;
}

function aggregateRouteMax(runs) {
  const maxes = {};
  for (const run of runs) {
    for (const [route, score] of Object.entries(run.finalState?.routeScore || {})) {
      maxes[route] = Math.max(maxes[route] || 0, Number(score || 0));
    }
  }
  return Object.fromEntries(Object.entries(maxes).sort((left, right) => right[1] - left[1]));
}

function summarizeSnapshots(runs) {
  const summary = {};
  for (const day of ["7", "14", "21", "30"]) {
    const snapshots = runs.map((run) => run.snapshots?.[day]).filter(Boolean);
    summary[day] = {
      fans: stats(snapshots.map((snapshot) => snapshot.fans || 0)),
      openDebts: stats(snapshots.map((snapshot) => snapshot.openDebtCount || 0)),
      watchHeat: stats(snapshots.map((snapshot) => snapshot.opinion?.watchHeat || 0)),
      reputation: stats(snapshots.map((snapshot) => snapshot.opinion?.reputation || 0)),
    };
  }
  return summary;
}

function dominanceWarnings(strategies) {
  const entries = Object.entries(strategies);
  const warnings = [];
  for (const [name, data] of entries) {
    const otherFans = entries
      .filter(([other]) => other !== name)
      .map(([, otherData]) => otherData.fans.median)
      .sort((a, b) => a - b);
    const otherMedian = median(otherFans);
    if (!otherMedian) continue;
    const leadRatio = data.fans.median / otherMedian;
    const popWins = entries.every(([, otherData]) => data.popularity.avg >= otherData.popularity.avg);
    const repNotBehind = entries.every(([, otherData]) => data.reputation.avg >= otherData.reputation.avg - 10);
    if (leadRatio > 1.5 && popWins && repNotBehind) {
      warnings.push({
        strategy: name,
        leadRatio: round(leadRatio, 2),
        reason: "median fans > 1.5x other-strategy median, while popularity wins and reputation is not meaningfully behind",
      });
    }
  }
  return warnings;
}

function markdownReport(summary) {
  const lines = [];
  lines.push("# Balance Simulation Report");
  lines.push("");
  lines.push(`Generated: ${summary.generatedAt}`);
  lines.push(`Total runs: ${summary.totalRuns}`);
  lines.push(`Validation: ${summary.validation.allPassed ? "PASS" : "FAIL"} (${summary.validation.failedCount} failed check${summary.validation.failedCount === 1 ? "" : "s"})`);
  lines.push("");
  lines.push("## Baseline Validation");
  lines.push("");
  lines.push("| Check | Status | Details |");
  lines.push("| --- | --- | --- |");
  for (const check of summary.validation.checks) {
    lines.push(`| ${check.label} | ${check.status} | ${check.details} |`);
  }
  lines.push("");
  lines.push("## Ending Distribution");
  lines.push("");
  lines.push("| Strategy | Runs | Target hit | Fans median | Fans avg | Policy modes | Ending distribution |");
  lines.push("| --- | ---: | ---: | ---: | ---: | --- | --- |");
  for (const [strategy, data] of Object.entries(summary.strategies)) {
    lines.push(`| ${strategy} | ${data.runs} | ${percent(data.targetHitRate)} | ${data.fans.median} | ${data.fans.avg} | ${inlineCounts(data.policyModeCounts)} | ${inlineCounts(data.endingDistribution)} |`);
  }
  lines.push("");
  lines.push("## Pressure");
  lines.push("");
  lines.push("| Strategy | Reputation avg | Watch heat avg | Open debts avg | Debt opened avg | Event hit rate |");
  lines.push("| --- | ---: | ---: | ---: | ---: | ---: |");
  for (const [strategy, data] of Object.entries(summary.strategies)) {
    lines.push(`| ${strategy} | ${data.reputation.avg} | ${data.watchHeat.avg} | ${data.openDebts.avg} | ${data.debtOpenedAvg} | ${Math.round(data.formalEventHitRate * 100)}% |`);
  }
  lines.push("");
  lines.push("## Day Snapshots");
  lines.push("");
  lines.push("| Strategy | D7 fans | D14 fans | D21 fans | D30 fans |");
  lines.push("| --- | ---: | ---: | ---: | ---: |");
  for (const [strategy, data] of Object.entries(summary.strategies)) {
    lines.push(`| ${strategy} | ${data.snapshots["7"].fans.avg} | ${data.snapshots["14"].fans.avg} | ${data.snapshots["21"].fans.avg} | ${data.snapshots["30"].fans.avg} |`);
  }
  lines.push("");
  lines.push("## Warnings");
  lines.push("");
  if (summary.dominanceWarnings.length) {
    for (const warning of summary.dominanceWarnings) {
      lines.push(`- ${warning.strategy}: ${warning.reason} (${warning.leadRatio}x).`);
    }
  } else {
    lines.push("- No broad dominant strategy detected by the simple median-fans/popularity/reputation check.");
  }
  lines.push("");
  lines.push("## Target Failures");
  lines.push("");
  for (const [strategy, data] of Object.entries(summary.strategies)) {
    if (!data.targetEndings.length) continue;
    lines.push(`- ${strategy} -> ${data.targetEndings.join(" / ")}: ${inlineCounts(data.topFailureReasons) || "no failures"}`);
  }
  lines.push("");
  lines.push("## Files");
  lines.push("");
  lines.push("- `runs.jsonl`: one full game record per line.");
  lines.push("- `summary.json`: machine-readable aggregate.");
  lines.push("- `report.md`: this report.");
  lines.push("");
  return `${lines.join("\n")}\n`;
}

function inlineCounts(counts) {
  return Object.entries(counts || {})
    .map(([key, value]) => `${key} ${value}`)
    .join(", ");
}

async function main() {
  if (typeof fetch !== "function") {
    throw new Error("This script requires Node 18+ with global fetch.");
  }
  const args = parseArgs(process.argv.slice(2));
  args.baseUrl = stripTrailingSlash(args.baseUrl);

  console.log(`[balance-sim] base=${args.baseUrl} strategies=${args.strategies.join(",")} runs=${args.runs} concurrency=${args.concurrency}`);
  await waitForReady(args.baseUrl, args.waitMs);

  const tasks = [];
  for (const strategy of args.strategies) {
    for (let run = 1; run <= args.runs; run += 1) {
      tasks.push({ baseUrl: args.baseUrl, strategy, runNumber: run, seed: args.seed, maxDays: args.maxDays });
    }
  }

  const runs = await runWithConcurrency(tasks, args.concurrency, async (task, index) => {
    const label = `${task.strategy}#${task.runNumber}`;
    process.stdout.write(`[balance-sim] ${index + 1}/${tasks.length} ${label} ... `);
    try {
      const run = await runOne(task);
      process.stdout.write(`${run.ending || "UNKNOWN"} fans=${run.finalState?.fans ?? 0}\n`);
      return run;
    } catch (error) {
      process.stdout.write(`FAILED ${error.message}\n`);
      return {
        runId: `${task.strategy}_${task.runNumber}_failed`,
        seed: task.seed,
        strategy: task.strategy,
        runNumber: task.runNumber,
        ending: "ERROR",
        daysPlayed: 0,
        finalState: {},
        errors: [error.stack || error.message],
      };
    }
  });

  const outDir = resolve(args.outDir);
  mkdirSync(outDir, { recursive: true });
  const runsPath = resolve(outDir, "runs.jsonl");
  const summaryPath = resolve(outDir, "summary.json");
  const reportPath = resolve(outDir, "report.md");
  const summary = summarize(runs);

  writeFileSync(runsPath, runs.map((run) => JSON.stringify(run)).join("\n") + "\n", "utf8");
  writeFileSync(summaryPath, JSON.stringify(summary, null, 2) + "\n", "utf8");
  writeFileSync(reportPath, markdownReport(summary), "utf8");

  console.log(`[balance-sim] wrote ${relativeToCwd(runsPath)}`);
  console.log(`[balance-sim] wrote ${relativeToCwd(summaryPath)}`);
  console.log(`[balance-sim] wrote ${relativeToCwd(reportPath)}`);

  const failed = runs.filter((run) => run.ending === "ERROR");
  if (failed.length || !summary.validation.allPassed) {
    if (!summary.validation.allPassed) {
      console.error(`[balance-sim] validation failed: ${summary.validation.failedCount} check(s) failed`);
    }
    process.exitCode = 1;
  }
}

function relativeToCwd(path) {
  const cwd = resolve(".");
  return path.startsWith(cwd) ? path.slice(cwd.length + 1) : path;
}

main().catch((error) => {
  console.error(`[balance-sim] ${error.stack || error.message}`);
  process.exit(1);
});
