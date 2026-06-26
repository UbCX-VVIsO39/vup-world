package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.PersonaDtos.PersonaTagBoardDTO;
import com.example.vupworld.service.fan.PersonaTagService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persona")
public class PersonaController {
    private final PersonaTagService personaTagService;

    public PersonaController(PersonaTagService personaTagService) {
        this.personaTagService = personaTagService;
    }

    @GetMapping("/tags")
    public ApiResponse<PersonaTagBoardDTO> tags(HttpSession session) {
        Long userId = SessionSupport.requireUserId(session);
        return ApiResponse.ok("人设标签读取成功，楼友和老粉已经开始命名。", personaTagService.tags(userId));
    }
}
