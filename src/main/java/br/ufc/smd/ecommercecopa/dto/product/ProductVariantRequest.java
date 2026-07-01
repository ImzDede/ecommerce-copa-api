package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Variante comprável do produto. Cada variante é persistida como um SKU vinculado ao produto.")
public record ProductVariantRequest(
        @Schema(description = "Preencher apenas em update. Null cria uma nova variante.")
        UUID id, // null for new variants
        @Schema(example = "Camisa Brasil P Amarela")
        String title,
        @Schema(example = "Modelo P amarelo.")
        String description,
        @Schema(example = "249.90")
        BigDecimal price,
        @Schema(example = "299.90")
        BigDecimal originalPrice,
        @Schema(example = "10")
        Integer stock,
        @Schema(description = "Valores das opções definidas em options.", example = "{\"size\": \"P\", \"color\": \"Amarela\"}")
        Map<String, Object> attributes,
        @Schema(hidden = true)
        List<Integer> imageIndices
) {}
