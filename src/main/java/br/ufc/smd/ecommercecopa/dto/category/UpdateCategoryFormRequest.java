package br.ufc.smd.ecommercecopa.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class UpdateCategoryFormRequest {

    @Schema(description = "Opcional. Deixe em branco para manter o título atual.")
    @Size(max = 80, message = "Título deve ter no máximo 80 caracteres")
    private String title;

    @Schema(type = "string", format = "binary", description = "Nova imagem da categoria. Formatos aceitos: JPEG, PNG e WebP.")
    private MultipartFile image;

    @Schema(description = "Use true para remover a imagem atual sem enviar uma nova", example = "false")
    private Boolean removeImage;

    private Boolean featured;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public MultipartFile getImage() {
        return image;
    }

    public void setImage(MultipartFile image) {
        this.image = image;
    }

    public Boolean getRemoveImage() {
        return removeImage;
    }

    public void setRemoveImage(Boolean removeImage) {
        this.removeImage = removeImage;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }
}
