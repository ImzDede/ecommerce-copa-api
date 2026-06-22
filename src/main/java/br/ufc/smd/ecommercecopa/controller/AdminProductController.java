package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.product.CreateProductRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductListResponse;
import br.ufc.smd.ecommercecopa.dto.product.ProductResponse;
import br.ufc.smd.ecommercecopa.dto.product.UpdateProductRequest;
import br.ufc.smd.ecommercecopa.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin - Products", description = "CRUD administrativo de products, que agrupam SKUs e definem schema de variação.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar products", description = "Filtra por slug da categoria quando o parâmetro category for informado.")
    public ResponseEntity<ApiResponse<ProductListResponse>> list(@Parameter(description = "Slug da categoria, não UUID. Exemplo: chuteiras-de-campo", example = "chuteiras-de-campo")
                                                                  @RequestParam(required = false) String category,
                                                                  HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.list(category, session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar product por ID")
    public ResponseEntity<ApiResponse<ProductResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.findById(id, session)));
    }

    @PostMapping
    @Operation(summary = "Criar product", description = "Cria agrupador de SKUs vinculado a uma categoria e com schema de seletores.")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody CreateProductRequest request,
                                                               HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(productService.create(request, session)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar product")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateProductRequest request,
                                                               HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.update(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir product", description = "Faz soft delete do product e de todos os SKUs ativos vinculados.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        productService.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
