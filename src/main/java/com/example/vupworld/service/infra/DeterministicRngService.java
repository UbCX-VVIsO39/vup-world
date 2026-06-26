package com.example.vupworld.service.infra;

import com.example.vupworld.service.infra.RngLedger;


import com.example.vupworld.model.DaySession;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 确定性随机服务。所有随机调用必须通过此服务，输入包含 seed + cursor。
 * <p>
 * 使用方式：
 * <pre>
 *   RngLedger ledger = deterministicRngService.createLedger(session);
 *   int value = ledger.nextInt("event-pick", candidates.size());
 *   // ... 事务结束时 ...
 *   deterministicRngService.finalizeLedger(session, ledger);
 * </pre>
 */
@Component
public class DeterministicRngService {

    private final DaySessionMapperHelper daySessionMapperHelper;

    public DeterministicRngService(DaySessionMapperHelper daySessionMapperHelper) {
        this.daySessionMapperHelper = daySessionMapperHelper;
    }

    /**
     * 为当前 day session 创建一个 RngLedger，从 rngCursor 开始。
     */
    public RngLedger createLedger(DaySession session) {
        return new RngLedger(session.getRandomSeed(), session.getRngCursor());
    }

    /**
     * 事务结束时将最终游标写回 day session。
     * 只在写事务中调用。
     */
    public void finalizeLedger(DaySession session, RngLedger ledger) {
        session.setRngCursor(ledger.getCursor());
        daySessionMapperHelper.updateRngCursor(session.getId(), ledger.getCursor());
    }

    /**
     * 获取 rngDetail JSON（用于 BusinessLog）。
     */
    public String toRngDetail(RngLedger ledger) {
        return "{\"seed\":\"" + ledger.getSeed() + "\",\"cursor\":" + ledger.getCursor()
                + ",\"calls\":" + ledger.getEntries().size() + "}";
    }

    /**
     * Helper interface to avoid circular dependency with DaySessionMapper.
     */
    public interface DaySessionMapperHelper {
        void updateRngCursor(Long sessionId, int cursor);
    }
}
