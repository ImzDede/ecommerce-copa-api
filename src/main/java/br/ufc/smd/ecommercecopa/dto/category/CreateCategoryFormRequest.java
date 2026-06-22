package br.ufc.smd.ecommercecopa.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public class CreateCategoryFormRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(min = 2, max = 80, message = "Título deve ter entre 2 e 80 caracteres")
    private String title;

    @Schema(type = "string", format = "binary", description = "Imagem da categoria. Formatos aceitos: JPEG, PNG e WebP.")
    private MultipartFile image;

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

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }
}
