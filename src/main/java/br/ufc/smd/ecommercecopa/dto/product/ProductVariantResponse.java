package br.ufc.smd.ecommercecopa.dto.product;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        Integer stock,
        String photo,
        Map<String, Object> attributes
) {}
