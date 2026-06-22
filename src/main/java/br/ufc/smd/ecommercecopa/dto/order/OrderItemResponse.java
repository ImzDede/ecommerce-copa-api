package br.ufc.smd.ecommercecopa.dto.order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record OrderItemResponse(
        UUID skuId,
        UUID productId,
        String title,
        BigDecimal price,
        Integer amount,
        BigDecimal subtotal,
        String photo,
        Map<String, Object> attributes
) {
}
