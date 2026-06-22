package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.tag.CreateTagRequest;
import br.ufc.smd.ecommercecopa.dto.tag.TagListResponse;
import br.ufc.smd.ecommercecopa.dto.tag.TagResponse;
import br.ufc.smd.ecommercecopa.dto.tag.UpdateTagRequest;
import br.ufc.smd.ecommercecopa.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/tags")
@Tag(name = "Admin - Tags", description = "CRUD administrativo de tags manuais para SKUs.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @GetMapping
    @Operation(summary = "Listar tags")
    public ResponseEntity<ApiResponse<TagListResponse>> list(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(tagService.list(session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar tag")
    public ResponseEntity<ApiResponse<TagResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(tagService.findById(id, session)));
    }

    @PostMapping
    @Operation(summary = "Criar tag")
    public ResponseEntity<ApiResponse<TagResponse>> create(@Valid @RequestBody CreateTagRequest request,
                                                           HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(tagService.create(request, session)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar tag")
    public ResponseEntity<ApiResponse<TagResponse>> update(@PathVariable UUID id,
                                                           @Valid @RequestBody UpdateTagRequest request,
                                                           HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(tagService.update(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir tag", description = "Bloqueia exclusão se a tag estiver vinculada a algum SKU.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        tagService.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
