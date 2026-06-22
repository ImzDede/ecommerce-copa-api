package br.ufc.smd.ecommercecopa.dto.product;

import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        CategoryResponse category,

        @Schema(description = "Schema de seletores do produto.",
                example = "{\"selectors\": [{\"key\": \"version\", \"label\": \"Versão\"}, {\"key\": \"cape\", \"label\": \"Capa\"}]}")
        Map<String, Object> schema,

        Long skuCount
) {
}
