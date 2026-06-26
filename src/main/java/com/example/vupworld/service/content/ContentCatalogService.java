package com.example.vupworld.service.content;

import com.example.vupworld.service.infra.JsonService;

import com.example.vupworld.mapper.ContentCatalogMapper;
import com.example.vupworld.model.GameContent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified content catalog that loads all game content from the database into memory.
 * Falls back gracefully if the database has no content.
 */
@Service
public class ContentCatalogService {

    private static final Logger log = LoggerFactory.getLogger(ContentCatalogService.class);

    private final ContentCatalogMapper contentCatalogMapper;

    // category -> content_key -> List<ContentEntry>
    private Map<String, Map<String, List<ContentEntry>>> catalog = new LinkedHashMap<>();

    public ContentCatalogService(ContentCatalogMapper contentCatalogMapper) {
        this.contentCatalogMapper = contentCatalogMapper;
    }

    @PostConstruct
    public void loadAll() {
        try {
            List<GameContent> rows = contentCatalogMapper.selectAll();
            Map<String, Map<String, List<ContentEntry>>> loaded = new LinkedHashMap<>();
            for (GameContent row : rows) {
                loaded.computeIfAbsent(row.getCategory(), k -> new LinkedHashMap<>())
                        .computeIfAbsent(row.getContentKey(), k -> new ArrayList<>())
                        .add(new ContentEntry(row.getContentKey(), row.getSubKey(),
                                row.getContentText(), row.getContentJson(), row.getSortOrder()));
            }
            // Sort entries by sortOrder within each key
            for (Map<String, List<ContentEntry>> keyMap : loaded.values()) {
                for (List<ContentEntry> entries : keyMap.values()) {
                    entries.sort((a, b) -> Integer.compare(a.sortOrder(), b.sortOrder()));
                }
            }
            catalog = Collections.unmodifiableMap(loaded);
            log.info("ContentCatalog loaded {} categories, {} total entries",
                    catalog.size(), rows.size());
        } catch (Exception e) {
            log.warn("ContentCatalog failed to load from database, using empty catalog: {}", e.getMessage());
            catalog = new LinkedHashMap<>();
        }
    }

    /**
     * Returns all content organized as category -> key -> entries.
     */
    public Map<String, Map<String, List<ContentEntry>>> getCatalog() {
        return catalog;
    }

    /**
     * Returns all entries for a given category, as key -> entries map.
     */
    public Map<String, List<ContentEntry>> getEntries(String category) {
        return catalog.getOrDefault(category, Collections.emptyMap());
    }

    /**
     * Returns a single entry for the given category and key.
     * Returns null if not found.
     */
    public ContentEntry getEntry(String category, String key) {
        Map<String, List<ContentEntry>> keyMap = catalog.get(category);
        if (keyMap == null) {
            return null;
        }
        List<ContentEntry> entries = keyMap.get(key);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        return entries.get(0);
    }

    /**
     * Returns the content_text for the given category and key.
     * Returns null if not found.
     */
    public String getString(String category, String key) {
        ContentEntry entry = getEntry(category, key);
        return entry != null ? entry.text() : null;
    }

    /**
     * Returns all content_text values for entries under a given category and key.
     */
    public List<String> getStringList(String category, String key) {
        Map<String, List<ContentEntry>> keyMap = catalog.get(category);
        if (keyMap == null) {
            return Collections.emptyList();
        }
        List<ContentEntry> entries = keyMap.get(key);
        if (entries == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (ContentEntry entry : entries) {
            if (entry.text() != null) {
                result.add(entry.text());
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns true if the catalog has entries for the given category and key.
     */
    public boolean hasEntries(String category, String key) {
        Map<String, List<ContentEntry>> keyMap = catalog.get(category);
        if (keyMap == null) {
            return false;
        }
        List<ContentEntry> entries = keyMap.get(key);
        return entries != null && !entries.isEmpty();
    }

    public record ContentEntry(
            String key,
            String subKey,
            String text,
            String json,
            int sortOrder
    ) {}
}
