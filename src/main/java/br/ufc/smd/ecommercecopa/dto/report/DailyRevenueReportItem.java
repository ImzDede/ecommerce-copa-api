package br.ufc.smd.ecommercecopa.dto.report;

import java.math.BigDecimal;

public record DailyRevenueReportItem(
        String day,
        BigDecimal totalValue
) {
}
