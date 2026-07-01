package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "ProductVariantMultipartRequest", description = "Formulário multipart para criar ou atualizar uma única variante/SKU.")
public class ProductVariantMultipartRequest {
    @Schema(description = "JSON da variante/SKU.")
    private ProductVariantUpsertRequest data;

    @Schema(description = "Primeira imagem da variante. Opcional.", type = "string", format = "binary")
    private MultipartFile image0;

    @Schema(description = "Segunda imagem da variante. Opcional.", type = "string", format = "binary")
    private MultipartFile image1;

    @Schema(description = "Terceira imagem da variante. Opcional.", type = "string", format = "binary")
    private MultipartFile image2;

    public ProductVariantUpsertRequest getData() {
        return data;
    }

    public void setData(ProductVariantUpsertRequest data) {
        this.data = data;
    }

    public MultipartFile getImage0() {
        return image0;
    }

    public void setImage0(MultipartFile image0) {
        this.image0 = image0;
    }

    public MultipartFile getImage1() {
        return image1;
    }

    public void setImage1(MultipartFile image1) {
        this.image1 = image1;
    }

    public MultipartFile getImage2() {
        return image2;
    }

    public void setImage2(MultipartFile image2) {
        this.image2 = image2;
    }
}
