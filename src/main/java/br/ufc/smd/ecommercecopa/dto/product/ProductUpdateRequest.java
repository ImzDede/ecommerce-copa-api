package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Dados editáveis do Product, sem sincronizar variantes/SKUs.")
public record ProductUpdateRequest(
        @Schema(example = "Caixas Figurinha")
        String name,
        @Schema(example = "00000000-0000-0000-0000-000000000000")
        UUID categoryId
) {}
