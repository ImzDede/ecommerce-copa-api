package br.ufc.smd.ecommercecopa.dto.catalog;

import br.ufc.smd.ecommercecopa.dto.tag.TagResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CatalogSkuResponse(
        UUID id,
        UUID productId,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        String photo,
        Integer stock,
        Double rating,
        Long reviewCount,
        Map<String, Object> attributes,
        CatalogCategoryResponse category,
        List<TagResponse> tags
) {
}
