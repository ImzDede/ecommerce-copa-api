package br.ufc.smd.ecommercecopa.dto.catalog;

import br.ufc.smd.ecommercecopa.dto.tag.TagResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CatalogSkuOptionResponse(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        BigDecimal originalPrice,
        String photo,
        Integer stock,
        Double rating,
        Long reviewCount,

        @Schema(description = "Valores dos atributos do SKU.",
                example = "{\"cape\": \"Dura\", \"version\": \"Dourado\"}")
        Map<String, Object> attributes,

        List<TagResponse> tags
) {
}
