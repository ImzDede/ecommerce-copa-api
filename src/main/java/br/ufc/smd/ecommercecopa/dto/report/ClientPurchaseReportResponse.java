package br.ufc.smd.ecommercecopa.dto.report;

import java.util.List;

public record ClientPurchaseReportResponse(
        List<ClientPurchaseReportItem> items
) {
}
