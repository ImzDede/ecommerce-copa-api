package br.ufc.smd.ecommercecopa.dto.catalog;

import java.util.List;

public record CatalogSkuListResponse(
        List<CatalogSkuResponse> items,
        Integer page,
        Integer size,
        Long totalItems,
        Integer totalPages
) {
}
