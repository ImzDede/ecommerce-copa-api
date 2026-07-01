package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Dados de uma variante/SKU específica.")
public record ProductVariantUpsertRequest(
        @Schema(example = "Caixa de Figurinhas - Dourada")
        String title,
        @Schema(example = "A maleta é projetada para proteger suas figurinhas.")
        String description,
        @Schema(example = "29.99")
        BigDecimal price,
        @Schema(example = "50.00")
        BigDecimal originalPrice,
        @Schema(example = "9")
        Integer stock,
        @Schema(description = "Valores das opções definidas no Product.", example = "{\"color\": \"Dourada\"}")
        Map<String, Object> attributes
) {}
