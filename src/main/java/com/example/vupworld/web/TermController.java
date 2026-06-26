package com.example.vupworld.web;

import com.example.vupworld.common.ApiResponse;
import com.example.vupworld.dto.MoodDtos.TermDTO;
import com.example.vupworld.service.content.TermGlossaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 术语词典Controller。提供VTuber圈术语查询接口。
 */
@RestController
@RequestMapping("/api/terms")
public class TermController {
    private final TermGlossaryService termGlossaryService;

    public TermController(TermGlossaryService termGlossaryService) {
        this.termGlossaryService = termGlossaryService;
    }

    /**
     * 获取所有术语列表。
     */
    @GetMapping
    public ApiResponse<List<TermDTO>> getAllTerms() {
        return ApiResponse.ok("术语词典已获取。", termGlossaryService.getAllTerms());
    }

    /**
     * 获取单个术语。
     */
    @GetMapping("/{key}")
    public ApiResponse<TermDTO> getTerm(@PathVariable String key) {
        return ApiResponse.ok("术语已获取。", termGlossaryService.getTerm(key));
    }
}
