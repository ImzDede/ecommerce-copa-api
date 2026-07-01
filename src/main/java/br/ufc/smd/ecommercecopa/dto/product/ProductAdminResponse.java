package br.ufc.smd.ecommercecopa.dto.product;

import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import java.util.List;
import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        String name,
        CategoryResponse category,
        List<ProductAdminRequest.OptionRequest> options,
        List<ProductVariantResponse> variants
) {}
