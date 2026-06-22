package br.ufc.smd.ecommercecopa.dto.catalog;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CatalogProductDetailResponse(
        UUID id,
        CatalogCategoryResponse category,

        @Schema(description = "Schema de seletores do produto.",
                example = "{\"selectors\": [{\"key\": \"version\", \"label\": \"Versão\"}, {\"key\": \"cape\", \"label\": \"Capa\"}]}")
        Map<String, Object> schema,

        UUID selectedSkuId,
        CatalogSkuOptionResponse selectedSku,
        List<CatalogSkuOptionResponse> skus
) {
}
