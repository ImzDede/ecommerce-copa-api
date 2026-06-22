package br.ufc.smd.ecommercecopa.dto.category;

import java.util.List;

public record CategoryListResponse(
        List<CategoryResponse> items
) {
}
