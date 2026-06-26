package com.example.vupworld.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public class ShopDtos {

    public record PurchaseShopItemRequest(
            @NotBlank(message = "商品ID不能为空") String itemId,
            @NotBlank(message = "幂等键不能为空") String idempotencyKey
    ) {}

    public record PurchaseShopItemResult(
            String itemId,
            String itemName,
            Map<String, Integer> changes,
            int remainingCoin
    ) {}
}
