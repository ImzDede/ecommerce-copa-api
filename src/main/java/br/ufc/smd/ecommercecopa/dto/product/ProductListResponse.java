package br.ufc.smd.ecommercecopa.dto.product;

import java.util.List;

public record ProductListResponse(
        List<ProductResponse> items
) {
}
