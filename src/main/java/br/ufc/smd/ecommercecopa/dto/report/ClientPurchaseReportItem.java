package br.ufc.smd.ecommercecopa.dto.report;

import java.math.BigDecimal;
import java.util.UUID;

public record ClientPurchaseReportItem(
        UUID clientId,
        String clientName,
        Long totalOrders,
        BigDecimal totalValue
) {
}
