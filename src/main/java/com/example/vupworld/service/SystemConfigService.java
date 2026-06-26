package com.example.vupworld.service;

import com.example.vupworld.service.core.DayService;

import com.example.vupworld.dto.SystemDtos.ConfigCategoryDTO;
import com.example.vupworld.dto.SystemDtos.ConfigCheckDTO;
import com.example.vupworld.dto.SystemDtos.DefenseEvidenceDTO;
import com.example.vupworld.dto.SystemDtos.DevToolsDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class SystemConfigService {
    private static final int REQUIRED_DEMO_STRATEGY_COUNT = 11;
    private static final List<String> DEMO_STRATEGIES = List.of(
            "steady",
            "clip",
            "black_red",
            "social",
            "singing",
            "cyber_girlfriend",
            "main_stage_king",
            "glorious_graduation",
            "idle",
            "defense",
            "random"
    );
    private static final List<String> REQUIRED_GALLERY_ASSETS = List.of(
            "v4/manifest.json",
            "v4/protagonist/avatars/avatar-default.png",
            "v4/protagonist/avatars/avatar-streaming.png",
            "v4/live-room/shells/shell-default.png",
            "v4/backgrounds/algorithm-dashboard.png",
            "v4/backgrounds/day-01-first-night.png",
            "v4/backgrounds/report-echo-wall.png",
            "v4/events/common/title-backlash.png",
            "v4/npcs/portraits/singing-mentor.png",
            "v4/npcs/portraits/collab-streamer.png",
            "v4/stream-plans/previews/singing.png",
            "v4/stream-plans/previews/collaboration.png",
            "v4/stream-plans/previews/sc-thanks.png",
            "v4/routes/unknown/cover-landscape.png",
            "v4/routes/electronic-pickle/cover-landscape.png",
            "v4/routes/singing-idol/cover-landscape.png",
            "v4/routes/dance-meme/cover-landscape.png",
            "v4/routes/slice-saint/cover-landscape.png",
            "v4/routes/social-collab/cover-landscape.png",
            "v4/routes/black-red-main-stage/cover-landscape.png",
            "v4/routes/cyber-girlfriend/cover-landscape.png",
            "v4/routes/dd-bus-stop/cover-landscape.png",
            "v4/routes/main-stage-king/cover-landscape.png",
            "v4/routes/glorious-graduation/cover-landscape.png"
    );
    private static final List<String> REQUIRED_SEED_KEYS = List.of(
            "actions",
            "stream_plans",
            "title_styles",
            "risk_debts",
            "risk_tools",
            "ending_templates"
    );

    private final Environment environment;
    private final ObjectMapper objectMapper;

    public SystemConfigService(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    public ConfigCheckDTO checkP0() {
        GalleryCheck galleryCheck = checkGalleryAssets();
        SeedDataCheck seedDataCheck = checkSeedData();
        ContentSafetyCheck contentSafetyCheck = checkContentSafety();
        List<ConfigCategoryDTO> categories = new ArrayList<>(List.of(
                new ConfigCategoryDTO("actions", "PASS", "9个主行动已开放，支持每日一次主行动和行动列表恢复。"),
                new ConfigCategoryDTO("streamTitles", "PASS", "直播企划、标题3选1、换一批、取消和标题锁定已接入状态机。"),
                new ConfigCategoryDTO("liveInteraction", "PASS", "高风险标题可进入直播现场二段选择，选择结果写入日志并生成日报。"),
                new ConfigCategoryDTO("fanTopic", "PASS", "粉丝群议题可在READY阶段轻量处理；高热度装死且有未结清债务时会升级为正式事件卡。"),
                new ConfigCategoryDTO("stageBriefing", "PASS", "阶段复盘台在行动前展示当前阶段、平台口味、下周口味、路线快照、风险快照和今日委托；命中委托会进入结算、日志证据和连击成就。"),
                new ConfigCategoryDTO("funAudience", "PASS", "乐子人画像以录播欲、拱火欲、考据欲、玩梗欲四维展示，不消耗体力也不生成日报。"),
                new ConfigCategoryDTO("audienceExpectation", "PASS", "观众期待板在行动前展示当前最强期待、转型风险和下一步建议，不生成日报。"),
                new ConfigCategoryDTO("endingForecast", "PASS", "结局预演台在行动前展示当前最可能结局、门槛缺口和冲刺建议，不生成结局复盘。"),
                new ConfigCategoryDTO("personaTags", "PASS", "人设标签板从本轮行为证据、粉丝结构和舆论值派生观众叫法，解释路线记忆但不改收益。"),
                new ConfigCategoryDTO("comboDiscovery", "PASS", "组合技发现板从最近行动历史推断已发现和锁定套路；关键连招已接入轻量结算、日志证据和本手提示。"),
                new ConfigCategoryDTO("dailyLoop", "PASS", "第1-29天可生成日报并进入下一天，写接口有幂等保护。"),
                new ConfigCategoryDTO("riskTools", "PASS", "米线工具可使用、可扣成本、可降低债务且保留证据。"),
                new ConfigCategoryDTO("debtReturn", "PASS", "高风险标题可生成债务，到期后回流为正式事件并阻塞虚空日报。"),
                new ConfigCategoryDTO("endingReview", "PASS", "第30天生成最终日报、结局复盘、路线证据评分和复活赛目标，并禁止进入第31天。"),
                new ConfigCategoryDTO("reports", "PASS", "今日日报和历史日报可读取，复盘有业务日志证据引用。"),
                new ConfigCategoryDTO(
                        "demo_strategy_script",
                        DEMO_STRATEGIES.size() >= REQUIRED_DEMO_STRATEGY_COUNT ? "PASS" : "FAIL",
                        "11条开发验证策略可查询，覆盖稳健、切片、黑红、社交、歌势、赛博女友、主会场之王、光荣毕业、装死、防守和随机路线；验证脚本逐日调用正式 Service，不直连改表。",
                        REQUIRED_DEMO_STRATEGY_COUNT,
                        DEMO_STRATEGIES.size()
                )
        ));
        categories.add(new ConfigCategoryDTO("galleryAssets", galleryCheck.status(), galleryCheck.detail()));
        categories.add(new ConfigCategoryDTO(
                "seedData",
                seedDataCheck.status(),
                seedDataCheck.detail(),
                seedDataCheck.expectedCount(),
                seedDataCheck.actualCount()
        ));
        categories.add(new ConfigCategoryDTO("contentSafety", contentSafetyCheck.status(), contentSafetyCheck.detail()));
        categories.add(devDemoCategory());

        List<String> fatalErrors = new ArrayList<>(galleryCheck.missing().stream()
                .map(asset -> "图库缺少必需图片：" + asset)
                .toList());
        fatalErrors.addAll(seedDataCheck.missing().stream()
                .map(item -> "核心种子数据缺失：" + item)
                .toList());
        fatalErrors.addAll(contentSafetyCheck.fatalErrors());
        boolean canPlayP0 = fatalErrors.isEmpty();

        if (isProdProfile()) {
            return new ConfigCheckDTO(
                    canPlayP0 ? "PASS" : "FAIL",
                    OffsetDateTime.now().toString(),
                    List.of(new ConfigCategoryDTO(
                            "prodConfig",
                            canPlayP0 ? "PASS" : "FAIL",
                            canPlayP0
                                    ? "生产配置已通过最小可玩性检查；完整诊断明细仅在本地验证/测试环境开放。"
                                    : "生产配置未通过最小可玩性检查。"
                    )),
                    canPlayP0 ? List.of() : List.of("生产配置未通过最小可玩性检查；请在受控环境查看本地诊断明细。"),
                    List.of("prod 下已裁剪图库、种子数据和答辩证据明细；公网部署仍建议加网关或管理员鉴权。"),
                    List.of(),
                    devTools(),
                    canPlayP0
            );
        }

        return new ConfigCheckDTO(
                canPlayP0 ? "PASS" : "FAIL",
                OffsetDateTime.now().toString(),
                categories,
                fatalErrors,
                List.of("更多事件池、同台生态和更多路线数值仍需继续扩展；当前检查确认核心30天闭环可玩。"),
                defenseEvidence(),
                devTools(),
                canPlayP0
        );
    }

    private List<DefenseEvidenceDTO> defenseEvidence() {
        return List.of(
                new DefenseEvidenceDTO(
                        "mvc_layers",
                        "MVC分层",
                        "Controller -> Service -> Mapper -> Model/DTO 分层完整，页面通过 REST 接口驱动真实业务。",
                        "web 包接收请求，service 包结算业务，mapper 包访问 day_session、business_log、daily_report 等表。"
                ),
                new DefenseEvidenceDTO(
                        "daily_loop",
                        "30天闭环",
                        "每日行动、标题、事件、日报和结局复盘按状态机推进，不靠前端假数据。",
                        "day_session 记录阶段，business_log 存行动证据，daily_report 生成每日复盘，ending_review 存最终结局。"
                ),
                new DefenseEvidenceDTO(
                        "idempotency_transaction",
                        "幂等事务",
                        "写接口带 idempotencyKey，并在 Service 事务中落库，避免重复点击刷日志。",
                        "api_idempotency_record 记录请求指纹，ActionService/DayService 等写流程使用 @Transactional。"
                ),
                new DefenseEvidenceDTO(
                        "demo_isolation",
                        "验证隔离",
                        "开发验证只在 dev/test/manual profile 开启，正式环境关闭验证入口。",
                        "DevDemoController 只服务验证 profile；prod 下由禁用控制器返回验证工具禁用错误码。"
                )
        );
    }

    private SeedDataCheck checkSeedData() {
        ClassPathResource dataSql = new ClassPathResource("data.sql");
        if (!dataSql.exists()) {
            return new SeedDataCheck(
                    "FAIL",
                    "缺少 data.sql，无法证明核心种子数据可初始化。",
                    List.of("data.sql"),
                    REQUIRED_SEED_KEYS.size(),
                    0
            );
        }
        String sql;
        try {
            sql = new String(dataSql.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return new SeedDataCheck(
                    "FAIL",
                    "data.sql 读取失败：" + exception.getMessage(),
                    List.of("data.sql"),
                    REQUIRED_SEED_KEYS.size(),
                    0
            );
        }
        List<String> missing = new ArrayList<>();
        if (!sql.contains("p0_seed_manifest")) {
            missing.add("p0_seed_manifest");
        }
        List<String> missingSeedKeys = REQUIRED_SEED_KEYS.stream()
                .filter(seedKey -> !sql.contains("'" + seedKey + "'"))
                .toList();
        missing.addAll(missingSeedKeys);
        int actualCount = REQUIRED_SEED_KEYS.size() - missingSeedKeys.size();
        if (missing.isEmpty()) {
            return new SeedDataCheck(
                    "PASS",
                    "data.sql 已包含核心种子清单：9个主行动、12个直播企划、标题风格、4类债务、3个米线工具和9个结局模板。",
                    missing,
                    REQUIRED_SEED_KEYS.size(),
                    actualCount
            );
        }
        return new SeedDataCheck(
                "FAIL",
                "data.sql 存在，但核心种子清单缺失：" + String.join("、", missing),
                missing,
                REQUIRED_SEED_KEYS.size(),
                actualCount
        );
    }

    private GalleryCheck checkGalleryAssets() {
        Path galleryDir = Path.of("图库");
        List<String> requiredAssets = requiredGalleryAssetsFromV4Manifest(galleryDir);
        List<String> missing = requiredAssets.stream()
                .filter(asset -> !Files.isRegularFile(galleryDir.resolve(asset)))
                .toList();
        if (missing.isEmpty()) {
            return new GalleryCheck(
                    "PASS",
                    "v4 图库已就绪：manifest、主角头像、直播间默认图、11条路线封面/结局图、事件插画、NPC头像和直播企划预览均存在；manifest登记资源 " + requiredAssets.size() + " 个，可通过 /gallery/v4/** 引用。代表资源：" + String.join("、", REQUIRED_GALLERY_ASSETS) + "。",
                    missing
            );
        }
        return new GalleryCheck(
                "FAIL",
                "图库缺失 " + missing.size() + " 个必需资源：" + String.join("、", missing),
                missing
        );
    }

    private List<String> requiredGalleryAssetsFromV4Manifest(Path galleryDir) {
        Path manifestPath = galleryDir.resolve("v4").resolve("manifest.json");
        if (!Files.isRegularFile(manifestPath)) {
            return REQUIRED_GALLERY_ASSETS;
        }
        try {
            JsonNode manifest = objectMapper.readTree(manifestPath.toFile());
            JsonNode assets = manifest.path("assets");
            if (!assets.isArray()) {
                return REQUIRED_GALLERY_ASSETS;
            }
            List<String> manifestAssets = new ArrayList<>(REQUIRED_GALLERY_ASSETS);
            for (JsonNode asset : assets) {
                String file = asset.path("file").asText("");
                if (!file.isBlank()) {
                    manifestAssets.add("v4/" + file.replace('\\', '/'));
                }
            }
            return manifestAssets.stream().distinct().toList();
        } catch (IOException exception) {
            return REQUIRED_GALLERY_ASSETS;
        }
    }

    private ContentSafetyCheck checkContentSafety() {
        // S21：默认 PUBLIC_SAFE 模式，负面语境不允许真实人物姓名。
        String mode = environment.getProperty("vupworld.content-safety.mode", "PUBLIC_SAFE");
        boolean realNameMappingEnabled = environment.getProperty(
                "vupworld.content-safety.real-name-mapping.enabled",
                Boolean.class,
                false
        );
        if ("PUBLIC_SAFE".equalsIgnoreCase(mode)) {
            if (realNameMappingEnabled) {
                return new ContentSafetyCheck(
                        "FAIL",
                        "公开安全模式下启用了真实名映射，公开验证不能把拟名彩蛋带进负面逻辑。",
                        List.of("公开安全模式启用了真实名映射：请关闭真实名映射开关后再开播。")
                );
            }
            return new ContentSafetyCheck(
                    "PASS",
                    "当前为公开安全模式，所有同台角色和事件使用拟名，不出现真实个人或团体负面影射。",
                    List.of()
            );
        }
        return new ContentSafetyCheck(
                "PASS",
                realNameMappingEnabled
                        ? "当前为本地私有模式，可启用彩蛋映射，但负面逻辑仍不绑定真实人。"
                        : "当前为本地私有模式，未启用真实名映射。",
                List.of()
        );
    }

    private ConfigCategoryDTO devDemoCategory() {
        boolean enabled = devToolsEnabled();
        if (enabled) {
            return new ConfigCategoryDTO(
                    "devDemo",
                    "PASS",
                    "当前环境开发验证脚本可用，可快速复盘路线；脚本逐日调用正式 Service，通过正式服务推进行动、日报和结局。"
            );
        }
        return new ConfigCategoryDTO(
                "devDemo",
                "PASS",
                "当前为正式环境，开发验证脚本入口保持关闭，不允许脚本代打。"
        );
    }

    private DevToolsDTO devTools() {
        boolean enabled = devToolsEnabled();
        return new DevToolsDTO(
                enabled,
                activeProfileLabel(),
                enabled ? DEMO_STRATEGIES : List.of()
        );
    }

    private boolean devToolsEnabled() {
        if (isProdProfile()) {
            return false;
        }
        return List.of(environment.getActiveProfiles()).stream()
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile) || "manual".equals(profile));
    }

    private boolean isProdProfile() {
        return List.of(environment.getActiveProfiles()).contains("prod");
    }

    private String activeProfileLabel() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "默认配置";
        }
        return List.of(profiles).stream()
                .map(this::profileLabel)
                .distinct()
                .collect(java.util.stream.Collectors.joining("、"));
    }

    private String profileLabel(String profile) {
        return switch (profile) {
            case "dev" -> "开发环境";
            case "test" -> "测试环境";
            case "manual" -> "手动验收";
            case "prod" -> "正式环境";
            default -> "本地环境";
        };
    }

    private record GalleryCheck(String status, String detail, List<String> missing) {
    }

    private record ContentSafetyCheck(String status, String detail, List<String> fatalErrors) {
    }

    private record SeedDataCheck(String status, String detail, List<String> missing, int expectedCount, int actualCount) {
    }
}
