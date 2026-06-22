package br.ufc.smd.ecommercecopa.dto.review;

import java.util.UUID;

public record ReviewResponse(
        UUID clientId,
        String clientName,
        UUID skuId,
        String skuTitle,
        Integer stars,
        String comment,
        String createdAt
) {
}
