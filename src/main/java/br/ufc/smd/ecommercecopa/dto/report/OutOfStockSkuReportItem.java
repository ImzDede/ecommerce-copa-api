package br.ufc.smd.ecommercecopa.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record OutOfStockSkuReportItem(
        UUID skuId,
        UUID productId,
        String photo,
        String name,
        String description,
        Integer stock,
        BigDecimal price,
        String categorySlug,
        String categoryTitle
) {
}
