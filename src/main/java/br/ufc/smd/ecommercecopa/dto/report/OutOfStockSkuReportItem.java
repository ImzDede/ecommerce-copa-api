package br.ufc.smd.ecommercecopa.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record OutOfStockSkuReportItem(
        UUID skuId,
        UUID productId,
        String title,
        Integer stock,
        BigDecimal price,
        String categorySlug,
        String categoryTitle
) {
}
