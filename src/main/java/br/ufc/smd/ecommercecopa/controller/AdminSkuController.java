package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.sku.CreateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.sku.SkuListResponse;
import br.ufc.smd.ecommercecopa.dto.sku.SkuResponse;
import br.ufc.smd.ecommercecopa.dto.sku.UpdateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.tag.TagListResponse;
import br.ufc.smd.ecommercecopa.dto.tag.UpdateSkuTagsRequest;
import br.ufc.smd.ecommercecopa.service.SkuService;
import br.ufc.smd.ecommercecopa.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.beans.PropertyEditorSupport;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/skus")
@Tag(name = "Admin - SKUs", description = "CRUD administrativo de SKUs, a unidade comprável do catálogo.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminSkuController {

    private final SkuService skuService;
    private final TagService tagService;

    public AdminSkuController(SkuService skuService, TagService tagService) {
        this.skuService = skuService;
        this.tagService = tagService;
    }

    @InitBinder
    void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(MultipartFile.class, new PropertyEditorSupport() {
            @Override
            public void setAsText(String text) {
                if (text == null || text.isBlank()) {
                    setValue(null);
                    return;
                }
                throw new IllegalArgumentException("Campo de arquivo deve ser enviado como arquivo");
            }
        });
    }

    @GetMapping
    @Operation(summary = "Listar SKUs")
    public ResponseEntity<ApiResponse<SkuListResponse>> list(@RequestParam(required = false) UUID productId,
                                                             @RequestParam(required = false) String category,
                                                             @RequestParam(required = false) Boolean inStock,
                                                             HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(skuService.list(productId, category, inStock, session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar SKU por ID")
    public ResponseEntity<ApiResponse<SkuResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(skuService.findById(id, session)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Criar SKU", description = "Cria SKU via multipart/form-data. Envie attributes como JSON string e photo como arquivo opcional.")
    public ResponseEntity<ApiResponse<SkuResponse>> create(@Valid @ModelAttribute CreateSkuFormRequest request,
                                                            HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(skuService.create(request, session)));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualizar SKU", description = "Atualiza SKU via multipart/form-data. Use removePhoto=true para remover a foto atual.")
    public ResponseEntity<ApiResponse<SkuResponse>> update(@PathVariable UUID id,
                                                            @Valid @ModelAttribute UpdateSkuFormRequest request,
                                                            HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(skuService.update(id, request, session)));
    }

    @PutMapping("/{id}/tags")
    @Operation(summary = "Substituir tags do SKU", description = "Substitui todas as tags manuais vinculadas ao SKU.")
    public ResponseEntity<ApiResponse<TagListResponse>> replaceTags(@PathVariable UUID id,
                                                                    @Valid @RequestBody UpdateSkuTagsRequest request,
                                                                    HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(tagService.replaceSkuTags(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir SKU", description = "Faz soft delete do SKU preenchendo deletedAt.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        skuService.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
