package br.ufc.smd.ecommercecopa.dto.sku;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public class UpdateSkuFormRequest {

    private UUID productId;

    @Schema(description = "Opcional. Deixe em branco para manter o título atual.")
    @Size(max = 160, message = "Título deve ter no máximo 160 caracteres")
    private String title;

    @Schema(description = "Opcional. Deixe em branco para manter a descrição atual.")
    @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
    private String description;

    @DecimalMin(value = "0.01", message = "Preço deve ser maior que zero")
    private BigDecimal price;

    @DecimalMin(value = "0.01", message = "Preço original deve ser maior que zero")
    private BigDecimal originalPrice;

    @Min(value = 0, message = "Estoque não pode ser negativo")
    private Integer stock;

    @Schema(description = "JSON com os atributos do SKU. Envie vazio para manter os atuais.", example = "{\"cape\":\"Dura\",\"version\":\"Dourado\"}")
    private String attributes;

    @Schema(type = "string", format = "binary")
    private MultipartFile photo;

    @Schema(description = "Use true para remover a foto atual sem enviar uma nova", example = "false")
    private Boolean removePhoto;

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public MultipartFile getPhoto() {
        return photo;
    }

    public void setPhoto(MultipartFile photo) {
        this.photo = photo;
    }

    public Boolean getRemovePhoto() {
        return removePhoto;
    }

    public void setRemovePhoto(Boolean removePhoto) {
        this.removePhoto = removePhoto;
    }
}
