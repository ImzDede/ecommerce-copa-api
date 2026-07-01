package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados textuais do produto enviados no campo multipart data.")
public record ProductAdminRequest(
        @Schema(example = "Camisa Brasil 2026")
        String name,
        @Schema(example = "00000000-0000-0000-0000-000000000000")
        UUID categoryId,
        @Schema(description = "Opções de variação. Pode ser lista vazia para produto sem variantes visuais.")
        List<OptionRequest> options,
        @Schema(description = "Variantes compráveis. Mesmo produto sem variação deve enviar uma variante única.")
        List<ProductVariantRequest> variants
) {
    public record OptionRequest(
            @Schema(example = "size") String key,
            @Schema(example = "Tamanho") String label
    ) {}
}
