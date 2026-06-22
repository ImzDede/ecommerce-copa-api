package br.ufc.smd.ecommercecopa.dto.sku;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record CreateSkuRequest(
        @NotNull(message = "Produto é obrigatório")
        UUID productId,

        @NotBlank(message = "Título é obrigatório")
        @Size(min = 2, max = 160, message = "Título deve ter entre 2 e 160 caracteres")
        String title,

        @NotBlank(message = "Descrição é obrigatória")
        @Size(min = 2, max = 2000, message = "Descrição deve ter entre 2 e 2000 caracteres")
        String description,

        @NotNull(message = "Preço é obrigatório")
        @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
        BigDecimal price,

        @DecimalMin(value = "0.01", message = "Preço original deve ser maior que zero")
        BigDecimal originalPrice,

        String photo,

        @NotNull(message = "Estoque é obrigatório")
        @Min(value = 0, message = "Estoque não pode ser negativo")
        Integer stock,

        @NotNull(message = "Atributos são obrigatórios")
        @Schema(description = "Valores dos atributos do SKU. As chaves devem corresponder aos keys definidos no schema do produto.",
                example = "{\"cape\": \"Dura\", \"version\": \"Dourado\"}")
        Map<String, Object> attributes
) {
}
