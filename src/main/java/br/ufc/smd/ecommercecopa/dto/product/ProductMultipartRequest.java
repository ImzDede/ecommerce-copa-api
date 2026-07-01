package br.ufc.smd.ecommercecopa.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "ProductMultipartRequest", description = "Formulário multipart usado para criar ou atualizar produto, opções e variantes/SKUs.")
public class ProductMultipartRequest {

    @Schema(description = "JSON estruturado do produto. Descrição, preço e imagens pertencem às variantes/SKUs, não ao produto. No Swagger, use variantImage0 para variants[0], variantImage1 para variants[1] etc.",
            example = "{\"name\":\"Camisa Brasil 2026\",\"categoryId\":\"00000000-0000-0000-0000-000000000000\",\"options\":[{\"key\":\"size\",\"label\":\"Tamanho\"},{\"key\":\"color\",\"label\":\"Cor\"}],\"variants\":[{\"title\":\"Camisa Brasil P Amarela\",\"description\":\"Modelo P amarelo.\",\"price\":249.90,\"originalPrice\":299.90,\"stock\":10,\"attributes\":{\"size\":\"P\",\"color\":\"Amarela\"}},{\"title\":\"Camisa Brasil M Azul\",\"description\":\"Modelo M azul.\",\"price\":249.90,\"originalPrice\":299.90,\"stock\":5,\"attributes\":{\"size\":\"M\",\"color\":\"Azul\"}}]}")
    private ProductAdminRequest data;

    @Schema(description = "Imagem da variante de indice 0, ou seja, variants[0]. Opcional.", type = "string", format = "binary")
    private MultipartFile variantImage0;

    @Schema(description = "Imagem da variante de indice 1, ou seja, variants[1]. Opcional.", type = "string", format = "binary")
    private MultipartFile variantImage1;

    @Schema(description = "Imagem da variante de indice 2, ou seja, variants[2]. Opcional.", type = "string", format = "binary")
    private MultipartFile variantImage2;

    public ProductAdminRequest getData() {
        return data;
    }

    public void setData(ProductAdminRequest data) {
        this.data = data;
    }

    public MultipartFile getVariantImage0() {
        return variantImage0;
    }

    public void setVariantImage0(MultipartFile variantImage0) {
        this.variantImage0 = variantImage0;
    }

    public MultipartFile getVariantImage1() {
        return variantImage1;
    }

    public void setVariantImage1(MultipartFile variantImage1) {
        this.variantImage1 = variantImage1;
    }

    public MultipartFile getVariantImage2() {
        return variantImage2;
    }

    public void setVariantImage2(MultipartFile variantImage2) {
        this.variantImage2 = variantImage2;
    }
}
