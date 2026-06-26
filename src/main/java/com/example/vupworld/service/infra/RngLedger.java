package com.example.vupworld.service.infra;


import java.util.ArrayList;
import java.util.List;

/**
 * 记录一次事务中所有确定性随机调用的账本。
 * 用于 rngDetail 日志和 GET 无副作用验证。
 */
public class RngLedger {

    private final String seed;
    private final List<Entry> entries = new ArrayList<>();
    private int cursor;

    public RngLedger(String seed, int initialCursor) {
        this.seed = seed;
        this.cursor = initialCursor;
    }

    /**
     * 记录一次随机调用并推进游标。
     */
    public int nextInt(String slot, int bound) {
        int value = deterministicNext(slot, bound);
        entries.add(new Entry(slot, cursor, value, bound));
        cursor++;
        return value;
    }

    /**
     * 记录一次洗牌并推进游标（每元素一次）。
     */
    public <T> void shuffle(String slot, List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(slot + ":shuffle:" + i, i + 1);
            T temp = list.get(i);
            list.set(i, list.get(j));
            list.set(j, temp);
        }
    }

    public int getCursor() {
        return cursor;
    }

    public List<Entry> getEntries() {
        return List.copyOf(entries);
    }

    public String getSeed() {
        return seed;
    }

    /**
     * 基于 seed + cursor 的确定性哈希，生成 [0, bound) 的值。
     */
    private int deterministicNext(String slot, int bound) {
        String combined = seed + ":" + slot + ":" + cursor;
        int hash = combined.hashCode();
        return Math.floorMod(hash, bound);
    }

    public record Entry(String slot, int cursor, int value, int bound) {}
}
