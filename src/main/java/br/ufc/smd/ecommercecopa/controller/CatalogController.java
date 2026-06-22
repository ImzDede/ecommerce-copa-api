package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogProductDetailResponse;
import br.ufc.smd.ecommercecopa.dto.catalog.CatalogSkuListResponse;
import br.ufc.smd.ecommercecopa.dto.category.CategoryListResponse;
import br.ufc.smd.ecommercecopa.dto.review.ReviewListResponse;
import br.ufc.smd.ecommercecopa.service.CategoryService;
import br.ufc.smd.ecommercecopa.service.CatalogService;
import br.ufc.smd.ecommercecopa.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Catálogo", description = "Rotas públicas de catálogo e visualização de produto.")
public class CatalogController {

    private final CatalogService catalogService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    public CatalogController(CatalogService catalogService, CategoryService categoryService, ReviewService reviewService) {
        this.catalogService = catalogService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorias públicas")
    public ResponseEntity<ApiResponse<CategoryListResponse>> listCategories() {
        return ResponseEntity.ok(new ApiResponse<>(categoryService.listPublic()));
    }

    @GetMapping("/skus")
    @Operation(summary = "Listar catálogo", description = "Lista SKUs compráveis em estoque com filtros de categoria, busca, paginação e ordenação.")
    public ResponseEntity<ApiResponse<CatalogSkuListResponse>> listSkus(@RequestParam(required = false) List<String> category,
                                                                         @RequestParam(required = false) String q,
                                                                         @RequestParam(defaultValue = "1") int page,
                                                                         @RequestParam(defaultValue = "12") int size,
                                                                          @RequestParam(defaultValue = "relevance")
                                                                          @Parameter(schema = @Schema(type = "string", allowableValues = {"relevance", "price-asc", "price-desc", "newest", "rating"}))
                                                                          String sort) {
        return ResponseEntity.ok(new ApiResponse<>(catalogService.listSkus(category, q, page, size, sort)));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Visualizar produto", description = "Retorna o product base, schema de variações, SKU selecionado e SKUs ativos relacionados.")
    public ResponseEntity<ApiResponse<CatalogProductDetailResponse>> findProduct(@PathVariable UUID productId,
                                                                                  @RequestParam(required = false) UUID skuId) {
        return ResponseEntity.ok(new ApiResponse<>(catalogService.findProduct(productId, skuId)));
    }

    @GetMapping("/skus/{skuId}/reviews")
    @Operation(summary = "Listar avaliações do SKU", description = "Lista avaliações públicas vinculadas ao SKU informado.")
    public ResponseEntity<ApiResponse<ReviewListResponse>> listSkuReviews(@PathVariable UUID skuId) {
        return ResponseEntity.ok(new ApiResponse<>(reviewService.listBySku(skuId)));
    }
}
