package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.common.GameException;
import com.example.vupworld.dto.AuthDtos.UserDTO;
import com.example.vupworld.dto.GameDtos.GameBootstrapDTO;
import com.example.vupworld.dto.GameDtos.GameStartDTO;
import com.example.vupworld.dto.GameDtos.RenameSaveSlotRequest;
import com.example.vupworld.dto.GameDtos.SaveArchiveDTO;
import com.example.vupworld.dto.GameDtos.SaveSlotDTO;
import com.example.vupworld.service.AuthService;
import com.example.vupworld.service.core.GameService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.example.vupworld.web.SessionSupport.USER_ID;

@RestController
@RequestMapping("/api")
public class GameController {
    private final AuthService authService;
    private final GameService gameService;
    private final Environment environment;

    public GameController(AuthService authService, GameService gameService, Environment environment) {
        this.authService = authService;
        this.gameService = gameService;
        this.environment = environment;
    }

    @GetMapping("/game/bootstrap")
    public ApiResponse<GameBootstrapDTO> bootstrap(HttpSession session) {
        GameBootstrapDTO bootstrap = gameService.bootstrap(optionalUserId(session));
        authService.bindSession(session, bootstrap.user());
        return ApiResponse.ok("本地游戏入口读取成功。", bootstrap);
    }

    @PostMapping("/game/quick-start")
    public ApiResponse<GameStartDTO> quickStart(HttpSession session) {
        UserDTO user = localOrExistingPlayer(session);
        return ApiResponse.ok("本地第一局已就绪。", gameService.quickStart(user));
    }

    @PostMapping("/save-slots/{slotNumber}/start")
    public ApiResponse<GameStartDTO> startSlot(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = localOrExistingPlayer(session);
        return ApiResponse.ok("本地存档槽已就绪。", gameService.startSlot(user, slotNumber));
    }

    @GetMapping("/game/continue")
    public ApiResponse<GameStartDTO> continueRunGet(HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok("本地存档读取成功。", gameService.continueRun(user));
    }

    @PostMapping("/game/continue")
    public ApiResponse<GameStartDTO> continueRun(HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok("本地存档读取成功。", gameService.continueRun(user));
    }

    @GetMapping("/save-slots/{slotNumber}/continue")
    public ApiResponse<GameStartDTO> continueSlotGet(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok("本地存档槽读取成功。", gameService.continueSlot(user, slotNumber));
    }

    @PostMapping("/save-slots/{slotNumber}/continue")
    public ApiResponse<GameStartDTO> continueSlot(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok("本地存档槽读取成功。", gameService.continueSlot(user, slotNumber));
    }

    @GetMapping("/save-slots")
    public ApiResponse<List<SaveSlotDTO>> saveSlots(HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok("本地存档槽读取成功。", gameService.saveSlots(user == null ? null : user.id()));
    }

    @PostMapping("/save-slots/{slotNumber}/save")
    public ApiResponse<SaveSlotDTO> saveSlot(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok(
                "本地存档已保存。",
                gameService.saveSlot(user == null ? null : user.id(), slotNumber)
        );
    }

    @GetMapping("/save/export")
    public ApiResponse<SaveArchiveDTO> exportDefaultSlot(HttpSession session) {
        return exportSlot(1, session);
    }

    @GetMapping("/save-slots/{slotNumber}/export")
    public ApiResponse<SaveArchiveDTO> exportSlot(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok(
                "本地存档导出成功。",
                gameService.exportSlot(user, slotNumber)
        );
    }

    @PostMapping("/save/import")
    public ApiResponse<GameStartDTO> importDefaultSlot(@RequestBody SaveArchiveDTO archive, HttpSession session) {
        return importSlot(1, archive, session);
    }

    @PostMapping("/save-slots/{slotNumber}/import")
    public ApiResponse<GameStartDTO> importSlot(
            @PathVariable int slotNumber,
            @RequestBody SaveArchiveDTO archive,
            HttpSession session
    ) {
        UserDTO user = localOrExistingPlayer(session);
        return ApiResponse.ok(
                "本地存档导入成功。",
                gameService.importSlot(user, slotNumber, archive)
        );
    }

    @PostMapping("/save-slots/{slotNumber}/restart")
    public ApiResponse<GameStartDTO> restartSlot(@PathVariable int slotNumber, HttpSession session) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok(
                "本槽已重新开始。",
                gameService.restartSlot(user, slotNumber)
        );
    }

    @PostMapping("/save-slots/{slotNumber}/rename")
    public ApiResponse<SaveSlotDTO> renameSlot(
            @PathVariable int slotNumber,
            @RequestBody RenameSaveSlotRequest request,
            HttpSession session
    ) {
        UserDTO user = existingPlayer(session);
        authService.bindSession(session, user);
        return ApiResponse.ok(
                "本地存档已重命名。",
                gameService.renameSlot(user, slotNumber, request == null ? null : request.name())
        );
    }

    private Long optionalUserId(HttpSession session) {
        Object value = session.getAttribute(USER_ID);
        return value instanceof Long userId ? userId : null;
    }

    private UserDTO existingPlayer(HttpSession session) {
        Long sessionUserId = optionalUserId(session);
        if (sessionUserId != null) {
            return authService.getUser(sessionUserId);
        }
        // 单机 profile（singleplayer/manual）保留 local_player 自动登录的设计意图
        if (isSingleplayerProfile()) {
            return authService.localPlayerIfExists();
        }
        // prod/mysql/default profile 必须显式登录，未登录直接拒绝
        throw new GameException("UNAUTHORIZED", "请先登录。");
    }

    private UserDTO localOrExistingPlayer(HttpSession session) {
        UserDTO user = existingPlayer(session);
        if (user == null) {
            // 仅在单机 profile 且 local_player 尚未创建时可达，自动建号并登录
            return authService.localPlayer(session);
        }
        authService.bindSession(session, user);
        return user;
    }

    // 单机 profile 下允许自动 local_player 登录；prod/mysql/default 要求显式登录
    private boolean isSingleplayerProfile() {
        for (String profile : environment.getActiveProfiles()) {
            if ("singleplayer".equalsIgnoreCase(profile) || "manual".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }
}
