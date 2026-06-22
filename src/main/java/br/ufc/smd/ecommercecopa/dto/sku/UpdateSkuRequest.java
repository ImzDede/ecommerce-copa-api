package br.ufc.smd.ecommercecopa.dto.sku;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateSkuRequest(
        UUID productId,

        @Size(max = 160, message = "Título deve ter no máximo 160 caracteres")
        String title,

        @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
        String description,

        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal price,

        @DecimalMin(value = "0.01", message = "Preço original deve ser maior que zero")
        BigDecimal originalPrice,

        String photo,

        @Min(value = 0, message = "Estoque não pode ser negativo")
        Integer stock,

        @Schema(description = "Valores dos atributos do SKU. As chaves devem corresponder aos keys definidos no schema do produto.",
                example = "{\"cape\": \"Dura\", \"version\": \"Dourado\"}")
        Map<String, Object> attributes
) {
}
