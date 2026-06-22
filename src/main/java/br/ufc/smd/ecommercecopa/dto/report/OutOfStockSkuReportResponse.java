package br.ufc.smd.ecommercecopa.dto.report;

import java.util.List;

public record OutOfStockSkuReportResponse(
        List<OutOfStockSkuReportItem> items
) {
}
