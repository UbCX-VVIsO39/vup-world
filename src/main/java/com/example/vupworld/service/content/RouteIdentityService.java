package com.example.vupworld.service.content;

import com.example.vupworld.domain.RouteType;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 路线身份单一数据源：从 content/route-identity.json 加载每条路线的身份卡片，
 * 包含幻想、代价、玩家承诺、主播钩子，以及每日日报使用的路线三件套
 * （wantLine 想要 / costLine 代价 / nextStepLine 下一手）。
 * 加载失败时优雅降级为空表，调用方拿不到身份时会得到 Optional.empty()。
 */
@Component
public class RouteIdentityService {

    private static final Logger log = LoggerFactory.getLogger(RouteIdentityService.class);
    private static final String CONTENT_PATH = "content/route-identity.json";

    private final JsonService jsonService;
    private Map<String, RouteIdentity> identities = Collections.emptyMap();

    public RouteIdentityService(JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @PostConstruct
    public void loadAll() {
        try {
            String json = readClasspath(CONTENT_PATH);
            IdentityPack pack = jsonService.read(json, IdentityPack.class);
            Map<String, RouteIdentity> loaded = new LinkedHashMap<>();
            if (pack != null && pack.routeIdentities() != null) {
                for (RouteIdentity identity : pack.routeIdentities()) {
                    if (identity.routeType() != null && !identity.routeType().isBlank()) {
                        loaded.put(identity.routeType(), identity);
                    }
                }
            }
            identities = Collections.unmodifiableMap(loaded);
            log.info("RouteIdentity loaded {} entries from {}", identities.size(), CONTENT_PATH);
        } catch (Exception e) {
            log.warn("RouteIdentity failed to load from {}: {}", CONTENT_PATH, e.getMessage());
            identities = new LinkedHashMap<>();
        }
    }

    /**
     * 返回指定路线的完整身份卡片。找不到时返回 Optional.empty()。
     */
    public Optional<RouteIdentity> getIdentity(RouteType routeType) {
        if (routeType == null) {
            return Optional.empty();
        }
        return getIdentity(routeType.name());
    }

    /**
     * 返回指定路线类型的完整身份卡片，接受字符串以兼容派生结局类型
     * （如 CYBER_GIRLFRIEND/DD_BUS_STOP 等不在 RouteType 枚举里的派生路线）。
     */
    public Optional<RouteIdentity> getIdentity(String routeType) {
        if (routeType == null || routeType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(identities.get(routeType));
    }

    public String getWantLine(RouteType routeType) {
        return getIdentity(routeType).map(RouteIdentity::wantLine).orElse("");
    }

    public String getCostLine(RouteType routeType) {
        return getIdentity(routeType).map(RouteIdentity::costLine).orElse("");
    }

    public String getNextStepLine(RouteType routeType) {
        return getIdentity(routeType).map(RouteIdentity::nextStepLine).orElse("");
    }

    /**
     * 接受字符串路线类型的便捷重载，供 ReportService 在路线可能是派生结局类型时使用。
     */
    public String getWantLine(String routeType) {
        return getIdentity(routeType).map(RouteIdentity::wantLine).orElse("");
    }

    public String getCostLine(String routeType) {
        return getIdentity(routeType).map(RouteIdentity::costLine).orElse("");
    }

    public String getNextStepLine(String routeType) {
        return getIdentity(routeType).map(RouteIdentity::nextStepLine).orElse("");
    }

    /**
     * 返回路线身份卡片单行文案，供每日日报注入：
     * "路线身份卡片：想要{wantLine}；代价{costLine}；下一手{nextStepLine}"。
     * 路线未加载或为空时返回空字符串，调用方自行判断是否注入。
     */
    public String dailyIdentityCard(String routeType) {
        Optional<RouteIdentity> identity = getIdentity(routeType);
        if (identity.isEmpty()) {
            return "";
        }
        RouteIdentity ri = identity.get();
        return "路线身份卡片【" + ri.label() + "】：想要" + ri.wantLine()
                + "；代价" + ri.costLine()
                + "；下一手" + ri.nextStepLine();
    }

    private String readClasspath(String path) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record IdentityPack(String version, List<RouteIdentity> routeIdentities) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteIdentity(
            String routeType,
            String strategy,
            String label,
            String fantasy,
            String cost,
            String playerPromise,
            String streamerHook,
            String wantLine,
            String costLine,
            String nextStepLine
    ) {
    }
}
