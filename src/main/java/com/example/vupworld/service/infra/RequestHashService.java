package com.example.vupworld.service.infra;


import com.example.vupworld.dto.ActionDtos.CancelActionRequest;
import com.example.vupworld.dto.ActionDtos.RerollTitleRequest;
import com.example.vupworld.dto.ActionDtos.SubmitActionRequest;
import com.example.vupworld.dto.ActionDtos.SubmitScheduleRequest;
import com.example.vupworld.dto.ActionDtos.ChooseTitleRequest;
import com.example.vupworld.dto.DayDtos.NextDayRequest;
import com.example.vupworld.dto.EventDtos.ChooseEventRequest;
import com.example.vupworld.dto.FanTopicDtos.ChooseFanTopicRequest;
import com.example.vupworld.dto.InteractionDtos.ChooseInteractionRequest;
import com.example.vupworld.dto.RebirthDtos.RestartRequest;
import com.example.vupworld.dto.RiskToolDtos.UseRiskToolRequest;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class RequestHashService {
    public String hash(SubmitActionRequest request) {
        return sha256("actionType=" + request.actionType() + ";planType=" + request.planType());
    }

    public String hash(SubmitScheduleRequest request) {
        StringBuilder builder = new StringBuilder("planKey=")
                .append(request.planKey());
        if (request.slots() != null) {
            request.slots().forEach(slot -> builder
                    .append(";slot=").append(slot.slotKey())
                    .append(":").append(slot.actionType())
                    .append(":").append(slot.intensity()));
        }
        return sha256(builder.toString());
    }

    public String hash(ChooseTitleRequest request) {
        return sha256("titleTemplateId=" + request.titleTemplateId());
    }

    public String hash(CancelActionRequest request) {
        return sha256("cancelPendingAction=true");
    }

    public String hash(RerollTitleRequest request) {
        return sha256("planId=" + request.planId());
    }

    public String hash(NextDayRequest request) {
        return sha256("nextDay=true");
    }

    public String hash(UseRiskToolRequest request) {
        return sha256("toolType=" + request.toolType() + ";targetDebtId=" + request.targetDebtId());
    }

    public String hash(ChooseEventRequest request) {
        return sha256("eventId=" + request.eventId()
                + ";choiceId=" + request.choiceId()
                + ";choiceType=" + request.choiceType());
    }

    public String hash(ChooseFanTopicRequest request) {
        return sha256("topicKey=" + request.topicKey() + ";choiceType=" + request.choiceType());
    }

    public String hash(ChooseInteractionRequest request) {
        return sha256("choiceType=" + request.choiceType());
    }

    public String hash(RestartRequest request) {
        return sha256("confirmRestart=" + request.confirmRestart()
                + ";restartBiasType=" + canonicalRestartBiasType(request.restartBiasType()));
    }

    private String canonicalRestartBiasType(String raw) {
        return raw == null || raw.isBlank() ? "ATLAS_NEXT" : raw.trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
