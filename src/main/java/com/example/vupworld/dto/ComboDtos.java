package com.example.vupworld.dto;

import java.util.List;

public final class ComboDtos {
    private ComboDtos() {
    }

    public record ComboDiscoveryDTO(
            String headline,
            int discoveredCount,
            int totalCount,
            int lockedCount,
            int progressPercent,
            int currentRunDiscoveredCount,
            int newThisRunCount,
            String collectionLabel,
            String nextComboKey,
            String nextComboLabel,
            List<ComboItemDTO> discovered,
            List<ComboItemDTO> locked,
            String nextHint
    ) {
    }

    public record ComboItemDTO(
            String comboKey,
            String label,
            String hint,
            boolean discovered,
            String evidence,
            String tone
    ) {
    }
}
