package br.ufc.smd.ecommercecopa.dto.sku;

import java.util.List;

public record SkuListResponse(
        List<SkuResponse> items
) {
}
