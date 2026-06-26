// ================================================================
// B站直播间 UI 交互逻辑
// ================================================================

// --- 直播时长计时器 ---
(function() {
    const startTime = Date.now();
    function updateDuration() {
        const elapsed = Math.floor((Date.now() - startTime) / 1000);
        const h = String(Math.floor(elapsed / 3600)).padStart(2, '0');
        const m = String(Math.floor((elapsed % 3600) / 60)).padStart(2, '0');
        const s = String(elapsed % 60).padStart(2, '0');
        const el = document.getElementById('liveRoomDuration');
        if (el) el.textContent = h + ':' + m + ':' + s;
    }
    setInterval(updateDuration, 1000);
    updateDuration();
})();

// --- 更新直播间信息 ---
function updateLiveRoomInfo() {
    if (typeof state === 'undefined' || !state) return;
    const vup = state.vup;
    if (!vup) return;

    const nameEl = document.getElementById('liveRoomStreamerName');
    if (nameEl) nameEl.textContent = vup.name || 'VUP出道局';

    const idEl = document.getElementById('liveRoomStreamerId');
    if (idEl) idEl.textContent = '房间号: ' + (vup.id || '23333');

    const roomIdEl = document.querySelector('.live-room-room-id');
    if (roomIdEl) roomIdEl.textContent = 'ID: ' + (vup.id || '23333');

    // Update viewer count in top bar
    const viewerEl = document.getElementById('liveRoomViewerCount');
    if (viewerEl && danmakuViewerCountEl) {
        viewerEl.textContent = danmakuViewerCountEl.textContent;
    }

    // Update avatar
    const avatarEl = document.getElementById('liveRoomAvatar');
    if (avatarEl && vup.portrait) {
        avatarEl.src = vup.portrait;
    }
}

// --- 弹幕输入框回车发送 ---
(function() {
    const input = document.getElementById('danmakuInput');
    if (input) {
        input.addEventListener('keydown', function(e) {
            if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                sendDanmaku();
            }
        });
    }
})();

// --- 发送弹幕（视觉反馈）---
function sendDanmaku() {
    const input = document.getElementById('danmakuInput');
    if (!input || !input.value.trim()) return;

    const text = input.value.trim();
    input.value = '';

    // Spawn as overlay danmaku for visual feedback
    if (typeof spawnDanmakuOverlayMessage === 'function') {
        const persona = (state && state.user) ? (state.user.nickname || state.user.username || '我') : '我';
        spawnDanmakuOverlayMessage(text, persona, false);
    }
}

// --- 礼物面板状态 ---
let giftPanelOpen = false;
let selectedGift = null;
let selectedGiftQty = 1;
let giftComboCount = 0;
let giftComboTimer = null;

const GIFTS = [
    { id: 'flower', icon: '🌸', name: '小花花', price: 100 },
    { id: 'banana', icon: '🍌', name: '香蕉', price: 0 },
    { id: 'beer', icon: '🍺', name: '打call', price: 500 },
    { id: 'cake', icon: '🍰', name: '蛋糕', price: 9900 },
    { id: 'rocket', icon: '🚀', name: '小电视飞船', price: 1245000 },
    { id: 'heart', icon: '❤️', name: '小心心', price: 0 },
    { id: 'star', icon: '⭐', name: '星星', price: 0 },
    { id: 'gem', icon: '💎', name: '宝石', price: 5000 },
    { id: 'trophy', icon: '🏆', name: '奖杯', price: 9900 },
    { id: 'crown', icon: '👑', name: '皇冠', price: 50000 }
];

function toggleGiftPanel() {
    giftPanelOpen = !giftPanelOpen;
    const panel = document.getElementById('giftPanelOverlay');
    if (panel) {
        panel.classList.toggle('visible', giftPanelOpen);
    }
    if (giftPanelOpen) {
        renderGiftGrid();
        updateGiftBalance();
    }
}

function renderGiftGrid() {
    const grid = document.getElementById('giftGrid');
    if (!grid) return;

    grid.innerHTML = GIFTS.map(g => `
        <button class="gift-item${selectedGift === g.id ? ' selected' : ''}"
                type="button" data-action="select-gift" data-gift-id="${g.id}">
            <span class="gift-item-icon">${g.icon}</span>
            <span class="gift-item-name">${g.name}</span>
        </button>
    `).join('');
}

function selectGift(giftId) {
    selectedGift = giftId;
    renderGiftGrid();
    document.getElementById('giftSendBtn').disabled = false;
}

function selectGiftQty(qty) {
    selectedGiftQty = qty;
    document.querySelectorAll('.gift-qty-btn').forEach(btn => {
        btn.classList.toggle('selected', parseInt(btn.dataset.qty) === qty);
    });
}

function updateGiftBalance() {
    const el = document.getElementById('giftBalance');
    if (!el) return;
    if (typeof state !== 'undefined' && state.vup) {
        el.textContent = (state.vup.resources?.coin || 0);
    } else {
        el.textContent = '0';
    }
}

function sendGift() {
    if (!selectedGift) return;

    const gift = GIFTS.find(g => g.id === selectedGift);
    if (!gift) return;

    // Visual: float animation
    spawnGiftFloat(gift.icon, gift.name);

    // Combo logic
    giftComboCount += selectedGiftQty;
    showGiftCombo(giftComboCount);

    // Spawn a gift danmaku
    if (typeof spawnDanmakuOverlayMessage === 'function') {
        const persona = (state && state.user) ? (state.user.nickname || state.user.username || '我') : '我';
        spawnDanmakuOverlayMessage(
            '赠送了 ' + gift.name + ' x' + selectedGiftQty,
            persona,
            false
        );
    }

    // Reset combo after 3 seconds of inactivity
    if (giftComboTimer) clearTimeout(giftComboTimer);
    giftComboTimer = setTimeout(() => {
        giftComboCount = 0;
        const combo = document.getElementById('giftCombo');
        if (combo) combo.classList.add('hidden');
    }, 3000);
}

function spawnGiftFloat(icon, name) {
    const container = document.getElementById('giftFloatContainer');
    if (!container) return;

    const item = document.createElement('div');
    item.className = 'gift-float-item';
    item.innerHTML = '<span class="gift-float-icon">' + icon + '</span>' +
                     '<span class="gift-float-name">' + name + '</span>';
    container.appendChild(item);

    setTimeout(() => {
        if (item.parentNode === container) {
            container.removeChild(item);
        }
    }, 3200);
}

function showGiftCombo(count) {
    const combo = document.getElementById('giftCombo');
    const countEl = document.getElementById('giftComboCount');
    if (!combo || !countEl) return;

    combo.classList.remove('hidden');
    countEl.textContent = 'x' + count;

    // Set combo tier class
    countEl.className = 'gift-combo-count';
    if (count >= 99) countEl.classList.add('x99');
    else if (count >= 66) countEl.classList.add('x66');
    else if (count >= 10) countEl.classList.add('x10');
    else if (count >= 5) countEl.classList.add('x5');
    else if (count >= 2) countEl.classList.add('x2');

    // Re-trigger animation
    countEl.style.animation = 'none';
    countEl.offsetHeight; // trigger reflow
    countEl.style.animation = '';
}

// --- 右侧排行榜 ---
let rightSidebarOpen = false;

function toggleRightSidebar() {
    rightSidebarOpen = !rightSidebarOpen;
    const sidebar = document.getElementById('liveRightSidebar');
    const toggleBtn = document.getElementById('sidebarToggleFloat');
    if (sidebar) {
        sidebar.classList.toggle('visible', rightSidebarOpen);
    }
    if (toggleBtn) {
        toggleBtn.style.display = rightSidebarOpen ? 'none' : '';
    }
    if (rightSidebarOpen) {
        updateSidebarContent();
    }
}

function switchSidebarTab(btn, tabId) {
    document.querySelectorAll('.sidebar-tab').forEach(t => t.classList.remove('active'));
    btn.classList.add('active');

    document.getElementById('sidebarRankFans').classList.toggle('hidden', tabId !== 'rankFans');
    document.getElementById('sidebarRankGifts').classList.toggle('hidden', tabId !== 'rankGifts');
    document.getElementById('sidebarRankDm').classList.toggle('hidden', tabId !== 'rankDm');
}

function updateSidebarContent() {
    if (typeof state === 'undefined' || !state) return;

    // Fans ranking (from fan structure)
    const fansEl = document.getElementById('sidebarRankFans');
    if (fansEl && state.vup) {
        const fans = state.vup.fanStructure?.fans || state.vup.fans || 0;
        const name = state.vup.name || 'VUP';

        // Build ranking list: current VUP + rivals from platformData or mock
        var rivals = [];
        if (state.platformData && Array.isArray(state.platformData.npcs)) {
            state.platformData.npcs.forEach(function(npc) {
                var npcFans = npc.fans || Math.floor(fans * (0.3 + Math.abs(hashCode(npc.name || '')) % 70) / 100);
                rivals.push({ name: npc.name || '???', fans: npcFans, color: npc.color || null });
            });
        }
        // Sort rivals by fans descending, pick top 2
        rivals.sort(function(a, b) { return b.fans - a.fans; });
        var top2 = rivals.slice(0, 2);

        // If fewer than 2 rivals, fill with mock data
        var mockNames = ['星街すいせい', '天音かなた', '湊あくあ', '白上フブ夏', '兎田ぺこら'];
        var seed = hashCode(name);
        while (top2.length < 2) {
            var idx = (seed + top2.length) % mockNames.length;
            var mockFans = Math.max(1, Math.floor(fans * (0.4 + ((seed * (top2.length + 1)) % 60) / 100)));
            top2.push({ name: mockNames[idx], fans: mockFans, color: null });
        }

        // Sort all 3 by fans to determine actual ranking
        var all = [{ name: name, fans: fans, isMe: true }].concat(top2);
        all.sort(function(a, b) { return b.fans - a.fans; });

        var rankColors = ['var(--bili-pink)', '#FFD700', '#C0C0C0'];
        fansEl.innerHTML = all.slice(0, 3).map(function(item, i) {
            var rankClass = i === 0 ? 'top1' : i === 1 ? 'top2' : 'top3';
            var avatarContent = item.isMe
                ? '<div class="sidebar-rank-avatar" style="background: linear-gradient(135deg, var(--bili-pink), var(--bili-blue)); display:flex; align-items:center; justify-content:center; font-size:12px; color:#fff; font-weight:700;">' + (item.name.charAt(0)) + '</div>'
                : '<div class="sidebar-rank-avatar" style="background: ' + rankColors[i] + '; display:flex; align-items:center; justify-content:center; font-size:12px; color:#fff; font-weight:700;">' + html(item.name.charAt(0)) + '</div>';
            return '<div class="sidebar-rank-item">' +
                '<span class="sidebar-rank-num ' + rankClass + '">' + (i + 1) + '</span>' +
                avatarContent +
                '<div class="sidebar-rank-info">' +
                    '<div class="sidebar-rank-name">' + html(item.name) + (item.isMe ? ' (你)' : '') + '</div>' +
                    '<div class="sidebar-rank-value">粉丝: ' + formatNumber(item.fans) + '</div>' +
                '</div>' +
            '</div>';
        }).join('');
    }
}

function hashCode(str) {
    var h = 0;
    for (var i = 0; i < str.length; i++) {
        h = ((h << 5) - h) + str.charCodeAt(i);
        h = h & h;
    }
    return Math.abs(h);
}

function formatNumber(n) {
    if (n >= 10000) return (n / 10000).toFixed(1) + '万';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
    return String(n);
}

// --- B站样式初始化 ---
window.updateLiveRoomInfo = updateLiveRoomInfo;
window.sendDanmaku = sendDanmaku;
window.toggleGiftPanel = toggleGiftPanel;
window.selectGift = selectGift;
window.selectGiftQty = selectGiftQty;
window.sendGift = sendGift;
window.toggleRightSidebar = toggleRightSidebar;
window.switchSidebarTab = switchSidebarTab;

function initBiliStyle() {
    updateLiveRoomInfo();

    // Add bili-style classes
    const header = document.querySelector('.game-header');
    if (header) header.classList.add('bili-style');

    const tabBar = document.querySelector('.main-tab-bar');
    if (tabBar) tabBar.classList.add('bili-style');
}

// Hook into existing renderHeader to update live room info
(function() {
    const origRenderHeader = typeof renderHeader === 'function' ? renderHeader : null;
    if (origRenderHeader) {
        window._origRenderHeader = origRenderHeader;
        window.renderHeader = function() {
            window._origRenderHeader();
            updateLiveRoomInfo();
        };
    }
})();

// Initialize B站 style on DOM ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initBiliStyle);
} else {
    initBiliStyle();
}

// --- Event delegation for gift panel (replaces inline onclick) ---
(function() {
    document.addEventListener('click', function(e) {
        const btn = e.target.closest('[data-action="select-gift"]');
        if (btn && btn.dataset.giftId) {
            selectGift(btn.dataset.giftId);
        }
    });
})();

// --- Tab sliding indicator ---
(function initTabIndicator() {
    const tabBar = document.getElementById('mainTabBar');
    const indicator = document.getElementById('mainTabIndicator');
    if (!tabBar || !indicator) return;

    function updateIndicator() {
        const activeTab = tabBar.querySelector('.main-tab.active');
        if (!activeTab) return;
        indicator.style.left = activeTab.offsetLeft + 'px';
        indicator.style.width = activeTab.offsetWidth + 'px';
    }

    // Observe tab clicks
    tabBar.addEventListener('click', function() {
        requestAnimationFrame(updateIndicator);
    });

    // Initial position
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', updateIndicator);
    } else {
        updateIndicator();
    }

    // Recalculate on resize
    window.addEventListener('resize', updateIndicator);
})();

// --- Stat value change highlight (observes DOM changes) ---
(function initStatHighlight() {
    const observer = new MutationObserver(function(mutations) {
        mutations.forEach(function(m) {
            if (m.type === 'childList' && m.target.classList && m.target.classList.contains('stat-value')) {
                const el = m.target;
                const newVal = el.textContent;
                const oldVal = el.dataset.prevValue || '';

                // Remove old animation classes
                el.classList.remove('changed-positive', 'changed-negative');

                if (oldVal && newVal !== oldVal) {
                    const numNew = parseFloat(newVal.replace(/[^0-9.\-]/g, ''));
                    const numOld = parseFloat(oldVal.replace(/[^0-9.\-]/g, ''));

                    if (!isNaN(numNew) && !isNaN(numOld)) {
                        if (numNew > numOld) {
                            el.classList.add('changed-positive');
                        } else if (numNew < numOld) {
                            el.classList.add('changed-negative');
                        }
                        // Clean up class after animation
                        setTimeout(function() {
                            el.classList.remove('changed-positive', 'changed-negative');
                        }, 500);
                    }
                }
                el.dataset.prevValue = newVal;
            }
        });
    });

    // Observe all current and future .stat-value elements
    function observeStatValues() {
        document.querySelectorAll('.stat-value').forEach(function(el) {
            if (!el.dataset.prevValue) {
                el.dataset.prevValue = el.textContent;
                observer.observe(el, { childList: true, characterData: true, subtree: true });
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', observeStatValues);
    } else {
        observeStatValues();
    }

    // Re-observe after renders (panels are rebuilt frequently)
    setInterval(observeStatValues, 2000);
})();

// --- 骨架屏显示/隐藏 ---
window.showSkeleton = function(panelId) {
    const skeletonId = panelId + 'Skeleton';
    const panel = document.getElementById(panelId);
    const skeleton = document.getElementById(skeletonId);
    if (panel) panel.style.display = 'none';
    if (skeleton) skeleton.style.display = '';
};

window.hideSkeleton = function(panelId) {
    const skeletonId = panelId + 'Skeleton';
    const panel = document.getElementById(panelId);
    const skeleton = document.getElementById(skeletonId);
    if (skeleton) skeleton.style.display = 'none';
    if (panel) panel.style.display = '';
};
