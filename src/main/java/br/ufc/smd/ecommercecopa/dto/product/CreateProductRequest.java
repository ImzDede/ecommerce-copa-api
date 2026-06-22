package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record CreateProductRequest(
        @NotNull(message = "Categoria é obrigatória")
        UUID categoryId,

        @NotNull(message = "Schema é obrigatório")
        @Schema(description = "Define os selectors de variação do produto. Cada selector tem key (identificador) e label (rótulo).",
                example = "{\"selectors\": [{\"key\": \"version\", \"label\": \"Versão\"}, {\"key\": \"cape\", \"label\": \"Capa\"}]}")
        Map<String, Object> schema
) {
}
