package com.example.vupworld.service.content;

import com.example.vupworld.domain.RouteType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Content-layer route taxonomy for translating between base RouteType values,
 * player-facing route identities, event route groups, formal ending keys, and
 * acceptance strategies.
 */
@Component
public class RouteContentMappingService {

    private static final String DEFAULT_ROUTE_TYPE = RouteType.ELECTRONIC_PICKLE.name();

    public static final List<String> BASE_ROUTE_TYPES = List.of(
            RouteType.ELECTRONIC_PICKLE.name(),
            RouteType.SINGING_IDOL.name(),
            RouteType.SLICE_SAINT.name(),
            RouteType.SOCIAL_COLLAB.name(),
            RouteType.DANCE_MEME.name(),
            RouteType.BLACK_RED_MAIN_STAGE.name(),
            RouteType.UNKNOWN.name()
    );

    public static final List<String> PLAYER_ROUTE_TYPES = List.of(
            "ELECTRONIC_PICKLE",
            "SINGING_IDOL",
            "SLICE_SAINT",
            "SOCIAL_COLLAB",
            "DANCE_MEME",
            "BLACK_RED_MAIN_STAGE",
            "DD_BUS_STOP",
            "CYBER_GIRLFRIEND",
            "MAIN_STAGE_KING",
            "GLORIOUS_GRADUATION",
            "UNKNOWN"
    );

    public static final List<String> FORMAL_ENDING_KEYS = List.of(
            "UNKNOWN",
            "ELECTRONIC_PICKLE",
            "SINGING_IDOL",
            "SLICE_SAINT",
            "BLACK_RED_MAIN_STAGE",
            "CYBER_GIRLFRIEND",
            "DD_BUS_STOP",
            "MAIN_STAGE_KING",
            "GLORIOUS_GRADUATION"
    );

    private static final Map<String, RouteMapping> ROUTE_MAPPINGS = buildRouteMappings();
    private static final Map<String, AcceptanceStrategyMapping> ACCEPTANCE_STRATEGIES = buildAcceptanceStrategies();

    public List<String> baseRouteTypes() {
        return BASE_ROUTE_TYPES;
    }

    public List<String> playerRouteTypes() {
        return PLAYER_ROUTE_TYPES;
    }

    public List<String> formalEndingKeys() {
        return FORMAL_ENDING_KEYS;
    }

    public List<String> acceptanceStrategyKeys() {
        return List.copyOf(ACCEPTANCE_STRATEGIES.keySet());
    }

    public Optional<AcceptanceStrategyMapping> acceptanceStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ACCEPTANCE_STRATEGIES.get(strategy.trim().toLowerCase()));
    }

    public Optional<RouteMapping> findMapping(String routeType) {
        if (routeType == null || routeType.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(ROUTE_MAPPINGS.get(canonicalRouteType(routeType)));
    }

    public RouteMapping mappingFor(String routeType) {
        return findMapping(routeType).orElse(ROUTE_MAPPINGS.get(DEFAULT_ROUTE_TYPE));
    }

    public String routeGroupFor(String routeType) {
        return mappingFor(routeType).routeGroup();
    }

    public String scoreRouteType(String routeType) {
        return mappingFor(routeType).scoreRouteType();
    }

    public String baseRouteType(String routeType) {
        return mappingFor(routeType).baseRouteType();
    }

    public String formalEndingKey(String routeType) {
        return mappingFor(routeType).formalEndingKey();
    }

    public Map<String, Object> evidenceFor(String routeType) {
        RouteMapping mapping = mappingFor(routeType);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("routeIdentityType", mapping.routeIdentityType());
        evidence.put("identityStrategy", mapping.identityStrategy());
        evidence.put("acceptanceStrategy", mapping.acceptanceStrategy());
        evidence.put("routeGroup", mapping.routeGroup());
        evidence.put("baseRouteType", mapping.baseRouteType());
        evidence.put("scoreRouteType", mapping.scoreRouteType());
        evidence.put("formalEndingKey", mapping.formalEndingKey());
        evidence.put("formalEndingRoute", mapping.formalEndingRoute());
        evidence.put("explanation", mapping.explanation());
        return evidence;
    }

    private static String canonicalRouteType(String routeType) {
        String key = routeType.trim().toUpperCase();
        return switch (key) {
            case "LEGEND" -> "MAIN_STAGE_KING";
            case "GRADUATION" -> "GLORIOUS_GRADUATION";
            case "BLACK_RED" -> "BLACK_RED_MAIN_STAGE";
            case "CYBER_FAN_SERVICE" -> "CYBER_GIRLFRIEND";
            case "SOCIAL_COLLAB_ENDING" -> "DD_BUS_STOP";
            case "DANCE_MEME_ENDING" -> "SLICE_SAINT";
            default -> key;
        };
    }

    private static Map<String, RouteMapping> buildRouteMappings() {
        Map<String, RouteMapping> mappings = new LinkedHashMap<>();
        add(mappings, "ELECTRONIC_PICKLE", "steady", "steady", "steady",
                "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE", true,
                "稳健基础路线本身就是电子榨菜正式结局。");
        add(mappings, "SINGING_IDOL", "singing", "singing", "singing",
                "SINGING_IDOL", "SINGING_IDOL", "SINGING_IDOL", true,
                "歌势身份、基础路线和正式结局一一对应。");
        add(mappings, "SLICE_SAINT", "clip", "clip", "burst",
                "SLICE_SAINT", "SLICE_SAINT", "SLICE_SAINT", true,
                "切片身份进入 burst 事件组，并结算为切片圣体。");
        add(mappings, "SOCIAL_COLLAB", "social_collab", "social", "social",
                "SOCIAL_COLLAB", "SOCIAL_COLLAB", "DD_BUS_STOP", false,
                "社交联动是基础路线身份，正式社交结局以 DD 公交站收束。");
        add(mappings, "DANCE_MEME", "dance_meme", "clip", "burst",
                "DANCE_MEME", "SLICE_SAINT", "SLICE_SAINT", false,
                "梗舞整活是基础路线身份，事件加分并入切片桶，正式结局归切片圣体。");
        add(mappings, "BLACK_RED_MAIN_STAGE", "black_red", "black_red", "heat",
                "BLACK_RED_MAIN_STAGE", "BLACK_RED_MAIN_STAGE", "BLACK_RED_MAIN_STAGE", true,
                "黑红基础路线可直接结算为黑红主会场。");
        add(mappings, "DD_BUS_STOP", "social", "social", "social",
                "SOCIAL_COLLAB", "SOCIAL_COLLAB", "DD_BUS_STOP", true,
                "DD 公交站是社交联动基础路线的正式结局身份。");
        add(mappings, "CYBER_GIRLFRIEND", "cyber_girlfriend", "cyber_girlfriend", "relationship",
                "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE", "CYBER_GIRLFRIEND", true,
                "赛博女友是稳健陪伴基础路线的商业/陪伴派生结局。");
        add(mappings, "MAIN_STAGE_KING", "main_stage_king", "main_stage_king", "heat",
                "BLACK_RED_MAIN_STAGE", "BLACK_RED_MAIN_STAGE", "MAIN_STAGE_KING", true,
                "主会场之王是黑红基础路线的高阶控场结局。");
        add(mappings, "GLORIOUS_GRADUATION", "glorious_graduation", "glorious_graduation", "steady",
                "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE", "GLORIOUS_GRADUATION", true,
                "光荣毕业是稳健基础路线的体面收束结局。");
        add(mappings, "UNKNOWN", "idle", "idle", "unknown",
                "UNKNOWN", "UNKNOWN", "UNKNOWN", true,
                "查无此 V 是未定型基础路线和正式失败归档。");
        return Collections.unmodifiableMap(mappings);
    }

    private static void add(
            Map<String, RouteMapping> mappings,
            String routeIdentityType,
            String identityStrategy,
            String acceptanceStrategy,
            String routeGroup,
            String baseRouteType,
            String scoreRouteType,
            String formalEndingKey,
            boolean formalEndingRoute,
            String explanation
    ) {
        mappings.put(routeIdentityType, new RouteMapping(
                routeIdentityType,
                identityStrategy,
                acceptanceStrategy,
                routeGroup,
                baseRouteType,
                scoreRouteType,
                formalEndingKey,
                formalEndingRoute,
                explanation
        ));
    }

    private static Map<String, AcceptanceStrategyMapping> buildAcceptanceStrategies() {
        Map<String, AcceptanceStrategyMapping> mappings = new LinkedHashMap<>();
        strategy(mappings, "steady", "ELECTRONIC_PICKLE", "ELECTRONIC_PICKLE");
        strategy(mappings, "clip", "SLICE_SAINT", "SLICE_SAINT");
        strategy(mappings, "black_red", "BLACK_RED_MAIN_STAGE", "BLACK_RED_MAIN_STAGE");
        strategy(mappings, "social", "DD_BUS_STOP", "DD_BUS_STOP");
        strategy(mappings, "singing", "SINGING_IDOL", "SINGING_IDOL");
        strategy(mappings, "cyber_girlfriend", "CYBER_GIRLFRIEND", "CYBER_GIRLFRIEND");
        strategy(mappings, "main_stage_king", "MAIN_STAGE_KING", "MAIN_STAGE_KING");
        strategy(mappings, "glorious_graduation", "GLORIOUS_GRADUATION", "GLORIOUS_GRADUATION");
        strategy(mappings, "idle", "UNKNOWN", "UNKNOWN");
        strategy(mappings, "defense", "GLORIOUS_GRADUATION", "GLORIOUS_GRADUATION");
        mappings.put("random", new AcceptanceStrategyMapping("random", "", "", "", "", ""));
        return Collections.unmodifiableMap(mappings);
    }

    private static void strategy(
            Map<String, AcceptanceStrategyMapping> mappings,
            String strategy,
            String routeIdentityType,
            String expectedEndingKey
    ) {
        RouteMapping route = ROUTE_MAPPINGS.get(routeIdentityType);
        mappings.put(strategy, new AcceptanceStrategyMapping(
                strategy,
                route.routeIdentityType(),
                route.routeGroup(),
                route.baseRouteType(),
                route.scoreRouteType(),
                expectedEndingKey
        ));
    }

    public record RouteMapping(
            String routeIdentityType,
            String identityStrategy,
            String acceptanceStrategy,
            String routeGroup,
            String baseRouteType,
            String scoreRouteType,
            String formalEndingKey,
            boolean formalEndingRoute,
            String explanation
    ) {
    }

    public record AcceptanceStrategyMapping(
            String strategy,
            String routeIdentityType,
            String routeGroup,
            String baseRouteType,
            String scoreRouteType,
            String expectedEndingKey
    ) {
    }
}
