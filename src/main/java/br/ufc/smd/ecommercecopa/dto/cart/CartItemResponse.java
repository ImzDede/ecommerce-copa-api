package br.ufc.smd.ecommercecopa.dto.cart;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CartItemResponse(
        UUID skuId,
        UUID productId,
        String title,
        BigDecimal unitPrice,
        String photo,
        Integer stock,
        Integer amount,
        BigDecimal subtotal,
        Map<String, Object> attributes
) {
}
