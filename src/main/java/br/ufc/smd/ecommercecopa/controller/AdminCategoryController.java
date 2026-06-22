package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.category.CategoryListResponse;
import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import br.ufc.smd.ecommercecopa.dto.category.CreateCategoryRequest;
import br.ufc.smd.ecommercecopa.dto.category.UpdateCategoryRequest;
import br.ufc.smd.ecommercecopa.service.CategoryService;
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
@RequestMapping("/api/admin/categories")
@Tag(name = "Admin - Categorias", description = "CRUD administrativo de categorias.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "Listar categorias")
    public ResponseEntity<ApiResponse<CategoryListResponse>> list(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(categoryService.list(session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(categoryService.findById(id, session)));
    }

    @PostMapping
    @Operation(summary = "Criar categoria", description = "Cria categoria com slug gerado automaticamente a partir do título.")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request,
                                                                HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(categoryService.create(request, session)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar categoria", description = "Atualiza o título e recalcula um slug único.")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable UUID id,
                                                                @Valid @RequestBody UpdateCategoryRequest request,
                                                                HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(categoryService.update(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir categoria", description = "Remove categoria somente quando não há products vinculados.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        categoryService.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
