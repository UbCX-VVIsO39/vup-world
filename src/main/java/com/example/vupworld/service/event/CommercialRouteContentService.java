package com.example.vupworld.service.event;

import com.example.vupworld.model.BusinessLog;
import com.example.vupworld.model.DaySession;
import com.example.vupworld.model.Vup;
import com.example.vupworld.service.content.RouteContentMappingService;
import com.example.vupworld.service.core.DayFlowService;
import com.example.vupworld.service.infra.JsonService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CommercialRouteContentService {
    private static final String CONTENT_PATH = "content/commercial-route-content.json";
    private static final String CONTENT_SOURCE = "commercial_route_content_pack";
    private static final List<String> REQUIRED_LATE_WINDOWS = List.of("FINAL_PRESSURE", "FINAL_LOCK", "FINAL_SCREENSHOT");
    private static final List<String> REQUIRED_ROUTE_TYPES = RouteContentMappingService.PLAYER_ROUTE_TYPES;

    private final JsonService jsonService;
    private final DayFlowService dayFlowService;
    private final RouteContentMappingService routeContentMappingService;
    private final ContentPack contentPack;

    public CommercialRouteContentService(
            JsonService jsonService,
            DayFlowService dayFlowService,
            RouteContentMappingService routeContentMappingService
    ) {
        this.jsonService = jsonService;
        this.dayFlowService = dayFlowService;
        this.routeContentMappingService = routeContentMappingService;
        this.contentPack = loadContentPack();
    }

    public Optional<SelectedLateGameEvent> selectLateGameFor(Vup vup, DaySession session, List<BusinessLog> recentLogs) {
        if (vup == null || session == null) {
            return Optional.empty();
        }
        WindowDef window = lateGameWindowForDay(session.getDay());
        if (window == null || alreadyResolvedWindow(window.key(), recentLogs)) {
            return Optional.empty();
        }
        String routeGroup = routeGroupFor(vup, recentLogs);
        String preferredRouteType = preferredRouteType(vup);
        return contentPack.lateGameEvents().stream()
                .filter(event -> window.key().equals(event.window()))
                .filter(event -> routeGroup.equals(event.routeGroup()))
                .findFirst()
                .map(event -> new SelectedLateGameEvent(
                        contentPack.version(),
                        CONTENT_SOURCE,
                        window,
                        event,
                        selectedRouteType(event, preferredRouteType)
                ));
    }

    public Map<String, Object> toLateGameRollDetail(SelectedLateGameEvent selected, String actionType) {
        LateGameEvent event = selected.event();
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("rollType", "late_game_event");
        detail.put("source", selected.source());
        detail.put("contentVersion", selected.contentVersion());
        detail.put("lateGameEventId", event.id());
        detail.put("lateGameWindow", selected.window().key());
        detail.put("lateGameWindowLabel", selected.window().label());
        detail.put("windowStartDay", selected.window().startDay());
        detail.put("windowEndDay", selected.window().endDay());
        detail.put("routeGroup", event.routeGroup());
        detail.put("routeTypes", event.routeTypes());
        detail.put("primaryRouteType", selected.primaryRouteType());
        detail.put("coreRouteType", coreRouteType(selected.primaryRouteType()));
        detail.put("baseRouteType", routeContentMappingService.baseRouteType(selected.primaryRouteType()));
        detail.put("scoreRouteType", routeContentMappingService.scoreRouteType(selected.primaryRouteType()));
        detail.put("formalEndingKey", routeContentMappingService.formalEndingKey(selected.primaryRouteType()));
        detail.put("routeMapping", routeContentMappingService.evidenceFor(selected.primaryRouteType()));
        detail.put("eventKey", event.id());
        detail.put("hitEventKey", event.id());
        detail.put("eventTitle", event.title());
        detail.put("eventDescription", event.description());
        detail.put("eventEffect", event.effect());
        detail.put("eventType", event.eventType());
        detail.put("screenshotLine", event.screenshotLine());
        detail.put("streamerLine", event.streamerLine());
        detail.put("heckleLine", event.heckleLine());
        detail.put("actionType", actionType);
        detail.put("choices", event.choices());
        detail.put("hit", true);
        detail.put("eventCandidates", contentPack.lateGameEvents().stream()
                .filter(candidate -> selected.window().key().equals(candidate.window()))
                .filter(candidate -> event.routeGroup().equals(candidate.routeGroup()))
                .map(candidate -> Map.of(
                        "eventKey", candidate.id(),
                        "eventType", candidate.eventType(),
                        "routeGroup", candidate.routeGroup(),
                        "routeTypes", candidate.routeTypes()
                ))
                .toList());
        detail.put("weights", Map.of(event.id(), 1));
        return detail;
    }

    public String contentVersion() {
        return contentPack.version();
    }

    public boolean isLateGameRoll(Map<String, Object> rollDetail) {
        return "late_game_event".equals(textValue(rollDetail, "rollType"))
                || rollDetail.containsKey("lateGameEventId");
    }

    public String coreRouteType(String routeType) {
        return routeContentMappingService.scoreRouteType(routeType);
    }

    private ContentPack loadContentPack() {
        try {
            ClassPathResource resource = new ClassPathResource(CONTENT_PATH);
            String json = resource.getContentAsString(StandardCharsets.UTF_8);
            ContentPack pack = jsonService.read(json, ContentPack.class);
            validate(pack);
            return pack;
        } catch (IOException exception) {
            throw new IllegalStateException("Commercial route content pack load failed: " + CONTENT_PATH, exception);
        }
    }

    private void validate(ContentPack pack) {
        if (pack == null || pack.version() == null || pack.version().isBlank()) {
            throw new IllegalStateException("Commercial route content pack version missing");
        }
        if (pack.routeIdentities() == null || pack.routeIdentities().isEmpty()) {
            throw new IllegalStateException("Commercial route identities missing");
        }
        if (pack.lateGameWindows() == null || pack.lateGameWindows().isEmpty()) {
            throw new IllegalStateException("Commercial late-game windows missing");
        }
        if (pack.lateGameEvents() == null || pack.lateGameEvents().isEmpty()) {
            throw new IllegalStateException("Commercial late-game events missing");
        }
        for (String window : REQUIRED_LATE_WINDOWS) {
            boolean exists = pack.lateGameWindows().stream().anyMatch(item -> window.equals(item.key()));
            if (!exists) {
                throw new IllegalStateException("Commercial route content missing late-game window: " + window);
            }
        }
        for (String routeType : REQUIRED_ROUTE_TYPES) {
            for (String window : REQUIRED_LATE_WINDOWS) {
                boolean covered = pack.lateGameEvents().stream()
                        .filter(event -> window.equals(event.window()))
                        .anyMatch(event -> event.routeTypes() != null && event.routeTypes().contains(routeType));
                if (!covered) {
                    throw new IllegalStateException("Commercial route content missing route/window coverage: "
                            + routeType + " " + window);
                }
            }
        }
    }

    private WindowDef lateGameWindowForDay(int day) {
        return contentPack.lateGameWindows().stream()
                .filter(window -> day >= window.startDay() && day <= window.endDay())
                .findFirst()
                .orElse(null);
    }

    private boolean alreadyResolvedWindow(String windowKey, List<BusinessLog> recentLogs) {
        if (windowKey == null || recentLogs == null || recentLogs.isEmpty()) {
            return false;
        }
        return recentLogs.stream()
                .map(BusinessLog::getWeightDetail)
                .filter(value -> value != null && !value.isBlank())
                .map(jsonService::readMap)
                .anyMatch(detail -> windowKey.equals(textValue(detail, "lateGameWindow")));
    }

    private String routeGroupFor(Vup vup, List<BusinessLog> recentLogs) {
        String target = expectationText(vup, "restartTargetType");
        if (target != null && !target.isBlank()) {
            return groupForRoute(target);
        }
        if (isUnformedIdle(vup, recentLogs)) {
            return "unknown";
        }
        String currentRoute = vup.getCurrentRoute();
        if (currentRoute != null && !"UNKNOWN".equals(currentRoute)) {
            if ("ELECTRONIC_PICKLE".equals(currentRoute) && (vup.getCommercialLevel() >= 35 || unicornRatio(vup) >= 30)) {
                return "relationship";
            }
            if ("SOCIAL_COLLAB".equals(currentRoute) && ddRatio(vup) >= 35) {
                return "social";
            }
            return groupForRoute(currentRoute);
        }
        return inferredGroup(vup);
    }

    private String inferredGroup(Vup vup) {
        Map<String, Integer> routeScores = dayFlowService.routeScores(vup);
        String route = routeScores.entrySet().stream()
                .filter(entry -> !"UNKNOWN".equals(entry.getKey()))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse("ELECTRONIC_PICKLE");
        if (vup.getSongPower() >= vup.getDancePower()
                && vup.getSongPower() >= vup.getPlanPower()
                && vup.getSongPower() >= vup.getTalkPower()) {
            return "singing";
        }
        if (vup.getMemeLevel() >= 45 || routeScores.getOrDefault("SLICE_SAINT", 0) >= 8
                || routeScores.getOrDefault("DANCE_MEME", 0) >= 8) {
            return "burst";
        }
        if (vup.getWatchHeat() >= 60 || routeScores.getOrDefault("BLACK_RED_MAIN_STAGE", 0) >= 8) {
            return "heat";
        }
        if (vup.getCommercialLevel() >= 35 || unicornRatio(vup) >= 30) {
            return "relationship";
        }
        if (vup.getDdFans() >= vup.getTrueFans() && vup.getDdFans() >= vup.getFunFans()) {
            return "social";
        }
        if ("UNKNOWN".equals(route) || routeScores.getOrDefault(route, 0) <= 0) {
            return "unknown";
        }
        return groupForRoute(route);
    }

    private String groupForRoute(String routeType) {
        return routeContentMappingService.routeGroupFor(routeType);
    }

    private boolean isUnformedIdle(Vup vup, List<BusinessLog> recentLogs) {
        Map<String, Integer> scores = dayFlowService.routeScores(vup);
        int bestRouteScore = scores.entrySet().stream()
                .filter(entry -> !"UNKNOWN".equals(entry.getKey()))
                .mapToInt(Map.Entry::getValue)
                .max()
                .orElse(0);
        long proactiveEvidenceCount = recentLogs == null ? 0 : recentLogs.stream()
                .filter(log -> log != null && isProactiveRouteAction(log.getAction()))
                .count();
        return proactiveEvidenceCount == 0
                && bestRouteScore <= 12
                && vup.getWatchHeat() <= 12
                && vup.getPopularity() <= 30
                && vup.getMemeLevel() <= 10
                && vup.getCommercialLevel() <= 10;
    }

    private boolean isProactiveRouteAction(String action) {
        return switch (action == null ? "" : action) {
            case "TRAIN_SONG", "TRAIN_DANCE", "TRAIN_TALK", "STREAM_PLAN", "PUBLISH_VIDEO",
                    "PUBLISH_CLIP", "FAN_GROUP_MAINTAIN", "NPC_INTERACT" -> true;
            default -> false;
        };
    }

    private String primaryRouteType(LateGameEvent event) {
        if (event.routeTypes() == null || event.routeTypes().isEmpty()) {
            return "ELECTRONIC_PICKLE";
        }
        return event.routeTypes().get(0);
    }

    private String selectedRouteType(LateGameEvent event, String preferredRouteType) {
        if (event.routeTypes() != null && preferredRouteType != null && event.routeTypes().contains(preferredRouteType)) {
            return preferredRouteType;
        }
        return primaryRouteType(event);
    }

    private String preferredRouteType(Vup vup) {
        String target = expectationText(vup, "restartTargetType");
        if (target != null && !target.isBlank()) {
            return target;
        }
        String currentRoute = vup.getCurrentRoute();
        if (currentRoute != null && !currentRoute.isBlank() && !"UNKNOWN".equals(currentRoute)) {
            return currentRoute;
        }
        return null;
    }

    private String expectationText(Vup vup, String key) {
        if (vup.getExpectationJson() == null || vup.getExpectationJson().isBlank()) {
            return null;
        }
        try {
            Object value = jsonService.readMap(vup.getExpectationJson()).get(key);
            return value instanceof String text && !text.isBlank() ? text : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private int unicornRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getUnicornFans() * 100 / vup.getFans();
    }

    private int ddRatio(Vup vup) {
        return vup.getFans() <= 0 ? 0 : vup.getDdFans() * 100 / vup.getFans();
    }

    private String textValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value instanceof String text ? text : "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ContentPack(
            String version,
            List<RouteIdentity> routeIdentities,
            List<WindowDef> lateGameWindows,
            List<LateGameEvent> lateGameEvents
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RouteIdentity(
            String routeType,
            String strategy,
            String label,
            String fantasy,
            String cost,
            String playerPromise,
            String streamerHook
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WindowDef(String key, String label, int startDay, int endDay) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LateGameEvent(
            String id,
            String window,
            String routeGroup,
            List<String> routeTypes,
            String eventType,
            String title,
            String description,
            String effect,
            String screenshotLine,
            String streamerLine,
            String heckleLine,
            Map<String, ChoiceCopy> choices
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChoiceCopy(String label, String costPreview, String riskPreview, String effectPreview) {
    }

    public record SelectedLateGameEvent(
            String contentVersion,
            String source,
            WindowDef window,
            LateGameEvent event,
            String primaryRouteType
    ) {
    }
}
