package br.ufc.smd.ecommercecopa.dto.catalog;

import java.util.UUID;

public record CatalogCategoryResponse(
        UUID id,
        String slug,
        String title,
        String image,
        boolean featured
) {
}
