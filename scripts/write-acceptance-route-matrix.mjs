#!/usr/bin/env node

import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const args = parseArgs(process.argv.slice(2));
const inputPath = path.resolve(root, args.input || 'target/acceptance-routes.json');
const mdOutputPath = path.resolve(root, args.out || 'target/acceptance-route-matrix.md');
const jsonOutputPath = path.resolve(root, args.json || 'target/acceptance-route-matrix.json');

const MATRIX_SPECS = [
    {
        strategy: 'steady',
        routeType: 'ELECTRONIC_PICKLE',
        targetEnding: 'ELECTRONIC_PICKLE',
        scoreRange: [120, 180],
        actionSequence: '低压 TALK 直播开局 -> TRAIN_TALK 日常复盘 -> 每 3 天 FAN_GROUP_MAINTAIN',
        uniqueGain: '稳定真粉、低旧账、结局证据链厚',
        uniqueCost: '爆点少，出圈慢，粉丝规模不会暴涨',
        uniqueFailure: '全程过稳会卡在低热度电子榨菜，缺少高阶结局材料',
        nextRunAdvice: '在稳定盘上补一次歌回或切片，测试是否能转入更高声量路线。',
        antiNoBrain: false
    },
    {
        strategy: 'clip',
        routeType: 'SLICE_SAINT / DANCE_MEME',
        targetEnding: 'SLICE_SAINT',
        scoreRange: [105, 170],
        actionSequence: 'PUBLISH_VIDEO 补素材 -> PUBLISH_CLIP 放大传播 -> TRAIN_DANCE 兜底新鲜度',
        uniqueGain: '素材、梗浓度、乐子粉增长快',
        uniqueCost: '旧账和疲劳会进结局复盘，梗浓度越高越容易被推着走',
        uniqueFailure: '只切不补素材会断粮，只堆梗会被素材反向定义',
        nextRunAdvice: '保留视频-切片节奏，但每周插入一次关系维护或低压复盘。',
        antiNoBrain: true
    },
    {
        strategy: 'black_red',
        routeType: 'BLACK_RED_MAIN_STAGE',
        targetEnding: 'BLACK_RED_MAIN_STAGE',
        scoreRange: [80, 125],
        actionSequence: 'Day 1-14 STREAM_PLAN + 硬嘴标题 -> Day 15-30 REST 降温还债',
        uniqueGain: '热度和争议识别度来得快',
        uniqueCost: '旧账、口碑和风险控制分会一起拉扯',
        uniqueFailure: '不降温会翻车，降温太晚会被主会场反噬',
        nextRunAdvice: '高风险标题后接下播降温、风险工具或粉丝群维护。',
        antiNoBrain: true
    },
    {
        strategy: 'social',
        routeType: 'DD_BUS_STOP / SOCIAL_COLLAB',
        targetEnding: 'DD_BUS_STOP',
        scoreRange: [55, 110],
        actionSequence: 'NPC_INTERACT 联动接车 -> Day 22 后穿插 FAN_GROUP_MAINTAIN 留人',
        uniqueGain: 'DD 占比和关系网扩张明显',
        uniqueCost: '自我标签会变弱，观众记住关系多于本人',
        uniqueFailure: '只联动不沉淀会变成路过站点',
        nextRunAdvice: '联动后补个人内容，把路过粉转成可记忆标签。',
        antiNoBrain: false
    },
    {
        strategy: 'singing',
        routeType: 'SINGING_IDOL',
        targetEnding: 'SINGING_IDOL',
        scoreRange: [110, 170],
        actionSequence: 'TRAIN_SONG 连续基本功 -> 歌势事件安全选择 -> 收官继续补作品证据',
        uniqueGain: '真粉、作品记忆点和低压成长稳定',
        uniqueCost: '成长慢，断档会被其他路线稀释',
        uniqueFailure: '只有练习没有传播时声量不足',
        nextRunAdvice: '在练歌基础上插入歌回或投稿，提升可见证据。',
        antiNoBrain: false
    },
    {
        strategy: 'cyber_girlfriend',
        routeType: 'CYBER_GIRLFRIEND',
        targetEnding: 'CYBER_GIRLFRIEND',
        scoreRange: [115, 165],
        actionSequence: 'STREAM_PLAN + SC_THANKS 陪伴营业 -> 持续回应高亮互动 -> 收官看边界压力',
        uniqueGain: '陪伴感、黏性和高互动反馈强',
        uniqueCost: '陪伴债和边界压力会累积',
        uniqueFailure: '只拉陪伴不维护边界会反扑',
        nextRunAdvice: '陪伴营业后安排边界维护和粉丝群秩序，不让关系债滚雪球。',
        antiNoBrain: true
    },
    {
        strategy: 'main_stage_king',
        routeType: 'MAIN_STAGE_KING / BLACK_RED_MAIN_STAGE',
        targetEnding: 'MAIN_STAGE_KING',
        scoreRange: [150, 210],
        actionSequence: '全程 STREAM_PLAN + 硬嘴标题 -> meme 事件选择 -> 依靠素材和控场压住热度',
        uniqueGain: '热度、梗浓度和乐子粉同时冲顶',
        uniqueCost: '高维维护路线，旧账、素材和口碑都要扛住',
        uniqueFailure: '热度足但控场不足时会退化成黑红主会场',
        nextRunAdvice: '争议后补切片、控场和降温，别只靠标题续命。',
        antiNoBrain: true
    },
    {
        strategy: 'glorious_graduation',
        routeType: 'GLORIOUS_GRADUATION',
        targetEnding: 'GLORIOUS_GRADUATION',
        scoreRange: [90, 145],
        actionSequence: 'FAN_GROUP_MAINTAIN 全程守口碑 -> 稳关系 -> 收官保持低债',
        uniqueGain: '结局尊严感、低旧账和粉丝关系稳定',
        uniqueCost: '爆发不足，不一定爽，但收束可信',
        uniqueFailure: '太早放弃增长会停在低粉毕业线',
        nextRunAdvice: '守住口碑后，在前两周补一个明确主路线证据。',
        antiNoBrain: false
    },
    {
        strategy: 'idle',
        routeType: 'UNKNOWN',
        targetEnding: 'UNKNOWN',
        scoreRange: [0, 25],
        actionSequence: 'REST 全程摸鱼 -> 不主动补路线证据 -> 低热度收束',
        uniqueGain: '验证失败/低光结局可达，不会被系统硬扶上岸',
        uniqueCost: '粉丝、路线证据和声量都不足',
        uniqueFailure: '查无此V，下一局必须先定方向',
        nextRunAdvice: 'Day 1-3 先连做同一条主路线行动，别再空转。',
        antiNoBrain: false
    },
    {
        strategy: 'defense',
        routeType: 'GLORIOUS_GRADUATION_DEFENSE',
        targetEnding: 'GLORIOUS_GRADUATION',
        scoreRange: [85, 135],
        actionSequence: 'Day 1 高风险暴露 -> Day 2 风险工具 -> REST / TRAIN_TALK / FAN_GROUP_MAINTAIN 循环拆债',
        uniqueGain: '证明有旧账时仍可靠防守毕业',
        uniqueCost: '速度慢，路线分不高，爽感让位于止损',
        uniqueFailure: '只休息不补谈力/维护会恢复不足',
        nextRunAdvice: '防守成功后补一个内容主轴，避免第二局继续只当救火队。',
        antiNoBrain: true
    },
    {
        strategy: 'random',
        routeType: 'MIXED_RANDOM',
        targetEnding: 'ANY_FORMAL',
        scoreRange: [75, 150],
        actionSequence: '按种子轮换 PUBLISH_VIDEO / PUBLISH_CLIP / NPC_INTERACT / FAN_GROUP_MAINTAIN / TRAIN_SONG / REST / TRAIN_DANCE / TRAIN_TALK',
        uniqueGain: '验证随机整活不会卡死，能自然落入正式结局',
        uniqueCost: '路线不可控，旧账和疲劳不会被抹平',
        uniqueFailure: '频繁换线会丢专属结局，靠素材偶然收束',
        nextRunAdvice: '保留随机乐趣，但每 5 天确认一次主路线，不要全天气球式飘走。',
        antiNoBrain: true
    }
];

const ACTION_LABELS = {
    TRAIN_SONG: '练歌',
    TRAIN_DANCE: '练舞',
    TRAIN_TALK: '杂谈复盘',
    STREAM_PLAN: '直播企划',
    PUBLISH_VIDEO: '投稿视频',
    PUBLISH_CLIP: '发布切片',
    FAN_GROUP_MAINTAIN: '粉丝群维护',
    NPC_INTERACT: '同台互动',
    REST: '休息'
};

const evidence = readJson(inputPath);
const strategies = Array.isArray(evidence.strategies) ? evidence.strategies : [];
const byStrategy = new Map(strategies.map(item => [item.strategy, item]));

const rows = MATRIX_SPECS.map(spec => {
    const result = byStrategy.get(spec.strategy) || {};
    const score = Number(result.routeScore ?? NaN);
    const scoreInRange = Number.isFinite(score)
        && score >= spec.scoreRange[0]
        && score <= spec.scoreRange[1];
    const targetMatches = spec.targetEnding === 'ANY_FORMAL'
        ? Boolean(result.formalEndingType)
        : result.endingType === spec.targetEnding;
    return {
        ...spec,
        label: result.label || spec.strategy,
        passed: Boolean(result.passed),
        actualEnding: result.endingType || '',
        actualRoute: result.routeReviewCurrentRoute || '',
        routeScore: Number.isFinite(score) ? score : null,
        routeEvidenceCount: Number(result.routeEvidenceCount || 0),
        routeEvidenceTrailCount: Number(result.routeEvidenceTrailCount || 0),
        debtRefCount: Number(result.debtRefCount || 0),
        memeLevel: Number(result.memeLevel || 0),
        funFans: Number(result.funFans || 0),
        currentDay: Number(result.currentDay || 0),
        phase: result.phase || '',
        scoreInRange,
        targetMatches,
        matrixReady: Boolean(result.passed) && targetMatches && scoreInRange
    };
});

const sequenceCounts = rows.reduce((map, row) => {
    map.set(row.actionSequence, (map.get(row.actionSequence) || 0) + 1);
    return map;
}, new Map());
const duplicateSequences = [...sequenceCounts.entries()]
    .filter(([, count]) => count > 1)
    .map(([sequence]) => sequence);
const missingStrategies = MATRIX_SPECS.map(spec => spec.strategy).filter(strategy => !byStrategy.has(strategy));
const outOfRangeRows = rows.filter(row => !row.scoreInRange).map(row => `${row.strategy}:${row.routeScore} not in ${row.scoreRange.join('-')}`);
const endingMismatchRows = rows.filter(row => !row.targetMatches).map(row => `${row.strategy}:${row.actualEnding} != ${row.targetEnding}`);
const failedRows = rows.filter(row => !row.passed).map(row => row.strategy);
const antiNoBrainCount = rows.filter(row => row.antiNoBrain).length;
const allPassed = Boolean(evidence.allPassed)
    && missingStrategies.length === 0
    && failedRows.length === 0
    && outOfRangeRows.length === 0
    && endingMismatchRows.length === 0
    && duplicateSequences.length === 0
    && antiNoBrainCount >= 3;

const summary = {
    generatedAt: new Date().toISOString(),
    source: path.relative(root, inputPath).replaceAll('\\', '/'),
    allPassed,
    evidenceAllPassed: Boolean(evidence.allPassed),
    routeCount: rows.length,
    uniqueActionSequenceCount: sequenceCounts.size,
    antiNoBrainCount,
    missingStrategies,
    failedRows,
    outOfRangeRows,
    endingMismatchRows,
    duplicateSequences,
    rows
};

writeJson(jsonOutputPath, summary);
writeMarkdown(mdOutputPath, summary);

console.log(JSON.stringify({
    allPassed,
    routeCount: rows.length,
    uniqueActionSequenceCount: sequenceCounts.size,
    antiNoBrainCount,
    report: path.relative(root, mdOutputPath).replaceAll('\\', '/')
}, null, 2));

if (!allPassed) {
    process.exitCode = 1;
}

function parseArgs(argv) {
    const parsed = {};
    for (let index = 0; index < argv.length; index++) {
        const arg = argv[index];
        if (!arg.startsWith('--')) continue;
        const key = arg.slice(2);
        const next = argv[index + 1];
        if (!next || next.startsWith('--')) {
            parsed[key] = true;
            continue;
        }
        parsed[key] = next;
        index++;
    }
    return parsed;
}

function readJson(filePath) {
    const text = readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
    return JSON.parse(text);
}

function writeJson(filePath, value) {
    mkdirSync(path.dirname(filePath), { recursive: true });
    writeFileSync(filePath, `${JSON.stringify(value, null, 2)}\n`, 'utf8');
}

function writeMarkdown(filePath, data) {
    mkdirSync(path.dirname(filePath), { recursive: true });
    const lines = [];
    lines.push('# 11 路线打法矩阵验收');
    lines.push('');
    lines.push(`生成时间：${data.generatedAt}`);
    lines.push(`证据来源：\`${data.source}\``);
    lines.push('');
    lines.push(`总体验收：${data.allPassed ? 'PASS' : 'FAIL'}`);
    lines.push('');
    lines.push('| 检查项 | 结果 | 证据 |');
    lines.push('|---|---|---|');
    lines.push(`| acceptance-routes allPassed | ${status(data.evidenceAllPassed)} | ${data.evidenceAllPassed} |`);
    lines.push(`| 11 条路线矩阵完整 | ${status(data.routeCount === 11 && data.missingStrategies.length === 0)} | ${data.routeCount}/11 |`);
    lines.push(`| 推荐行动序列不重复 | ${status(data.duplicateSequences.length === 0)} | ${data.uniqueActionSequenceCount}/11 unique |`);
    lines.push(`| 至少 3 条可达但不宜无脑 S | ${status(data.antiNoBrainCount >= 3)} | ${data.antiNoBrainCount}/3 |`);
    lines.push(`| 实际结局匹配目标 | ${status(data.endingMismatchRows.length === 0)} | ${data.endingMismatchRows.length ? data.endingMismatchRows.join('; ') : '全部匹配'} |`);
    lines.push(`| 实际路线分落在可接受区间 | ${status(data.outOfRangeRows.length === 0)} | ${data.outOfRangeRows.length ? data.outOfRangeRows.join('; ') : '全部落入区间'} |`);
    lines.push('');
    lines.push('## 路线矩阵');
    lines.push('');
    lines.push('| 路线 | 策略 | routeType | 目标结局 | 实际结局 | 分数区间/实际 | 推荐行动序列 | 独特收益 | 独特代价 | 独特失败方式 | 下一局建议 |');
    lines.push('|---|---|---|---|---|---|---|---|---|---|---|');
    for (const row of data.rows) {
        lines.push([
            row.label,
            row.strategy,
            row.routeType,
            row.targetEnding,
            row.actualEnding,
            `${row.scoreRange[0]}-${row.scoreRange[1]} / ${row.routeScore ?? ''}`,
            labelActionSequence(row.actionSequence),
            row.uniqueGain,
            row.uniqueCost,
            row.uniqueFailure,
            row.nextRunAdvice
        ].map(escapeMarkdownCell).join(' | ').replace(/^/, '| ').replace(/$/, ' |'));
    }
    lines.push('');
    lines.push('## 数值证据');
    lines.push('');
    lines.push('| 策略 | Day | 阶段 | 实际主路线 | 路线分 | 路线证据 | 证据链 | 旧账 | 梗浓度 | 乐子粉 |');
    lines.push('|---|---:|---|---|---:|---:|---:|---:|---:|---:|');
    for (const row of data.rows) {
        lines.push(`| ${escapeMarkdownCell(row.strategy)} | ${row.currentDay} | ${escapeMarkdownCell(row.phase)} | ${escapeMarkdownCell(row.actualRoute)} | ${row.routeScore ?? ''} | ${row.routeEvidenceCount} | ${row.routeEvidenceTrailCount} | ${row.debtRefCount} | ${row.memeLevel} | ${row.funFans} |`);
    }
    lines.push('');
    lines.push('## 运行方式');
    lines.push('');
    lines.push('```powershell');
    lines.push('scripts\\acceptance-routes.cmd');
    lines.push('node scripts\\write-acceptance-route-matrix.mjs');
    lines.push('node scripts\\quality-readiness-check.mjs');
    lines.push('```');
    lines.push('');
    writeFileSync(filePath, `${lines.join('\n')}\n`, 'utf8');
}

function status(pass) {
    return pass ? 'PASS' : 'FAIL';
}

function labelActionSequence(value) {
    return String(value).replace(/\b[A-Z_]{4,}\b/g, token => ACTION_LABELS[token] || token);
}

function escapeMarkdownCell(value) {
    return String(value ?? '')
        .replaceAll('|', '\\|')
        .replaceAll('\r', ' ')
        .replaceAll('\n', ' ');
}
