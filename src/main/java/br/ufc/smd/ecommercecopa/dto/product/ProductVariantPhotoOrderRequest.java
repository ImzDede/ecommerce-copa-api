package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Nova ordem das fotos de uma variante/SKU.")
public record ProductVariantPhotoOrderRequest(
        @Schema(description = "Lista com exatamente as fotos atuais, na nova ordem desejada.")
        List<String> photos
) {}
