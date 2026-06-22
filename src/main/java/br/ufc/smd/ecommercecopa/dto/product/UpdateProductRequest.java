package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.UUID;

public record UpdateProductRequest(
        UUID categoryId,

        @Schema(description = "Define os selectors de variação do produto. Cada selector tem key (identificador) e label (rótulo).",
                example = "{\"selectors\": [{\"key\": \"version\", \"label\": \"Versão\"}, {\"key\": \"cape\", \"label\": \"Capa\"}]}")
        Map<String, Object> schema
) {
}
