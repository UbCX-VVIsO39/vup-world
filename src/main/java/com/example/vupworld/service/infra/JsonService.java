package com.example.vupworld.service.infra;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.vupworld.dto.ActionDtos.TitleOptionDTO;
import com.example.vupworld.dto.EndingDtos.EndingTagDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class JsonService {
    private final ObjectMapper objectMapper;

    public JsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON serialization failed", e);
        }
    }

    public List<String> readStringList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON list parsing failed", e);
        }
    }

    public List<Long> readLongList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON long list parsing failed", e);
        }
    }

    public List<Map<String, Object>> readEvidenceRefs(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON evidence parsing failed", e);
        }
    }

    public List<EndingTagDTO> readEndingTags(String json) {
        try {
            List<Object> rawTags = objectMapper.readValue(json, new TypeReference<>() {
            });
            return rawTags.stream()
                    .map(this::toEndingTag)
                    .toList();
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("JSON ending tag parsing failed", e);
        }
    }

    private EndingTagDTO toEndingTag(Object rawTag) {
        if (rawTag instanceof String label) {
            return new EndingTagDTO("LEGACY_TAG", label, "旧版结局标签");
        }
        EndingTagDTO tag = objectMapper.convertValue(rawTag, EndingTagDTO.class);
        return new EndingTagDTO(
                tag.tagKey() == null ? "LEGACY_TAG" : tag.tagKey(),
                tag.label() == null ? "" : tag.label(),
                tag.evidence() == null ? "旧版结局标签" : tag.evidence()
        );
    }

    public Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON map parsing failed", e);
        }
    }

    public <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON parsing failed", e);
        }
    }

    public List<TitleOptionDTO> readTitleOptions(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON title parsing failed", e);
        }
    }
}
