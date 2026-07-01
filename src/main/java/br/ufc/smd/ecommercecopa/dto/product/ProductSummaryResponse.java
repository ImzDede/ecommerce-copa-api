package br.ufc.smd.ecommercecopa.dto.product;

import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import java.util.UUID;

public record ProductSummaryResponse(
        UUID id,
        String name,
        String coverImage,
        int variantCount,
        CategoryResponse category
) {}
