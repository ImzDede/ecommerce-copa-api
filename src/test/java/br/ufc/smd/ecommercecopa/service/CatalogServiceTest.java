package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.ReviewRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.repository.projection.SkuReviewStatsProjection;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private TagService tagService;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(skuRepository, productRepository, reviewRepository, tagService);
        lenient().when(reviewRepository.findStatsBySkuIds(any())).thenReturn(List.of());
    }

    @Test
    void listSkusReturnsPaginatedPublicCatalog() {
        Product product = product(UUID.randomUUID());
        Sku sku = sku(UUID.randomUUID(), product, "Chuteira Campo 40", 10);
        when(skuRepository.searchPublicCatalog(eq(List.of("chuteiras")), eq(1), eq("predator"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sku), PageRequest.of(0, 12), 1));

        var response = catalogService.listSkus(List.of(" chuteiras "), " predator ", 1, 12, "price-asc");

        assertEquals(1, response.items().size());
        assertEquals(1, response.page());
        assertEquals(12, response.size());
        assertEquals(1, response.totalItems());
        assertEquals("chuteiras", response.items().getFirst().category().slug());
        assertEquals(0.0, response.items().getFirst().rating());
        assertEquals(0L, response.items().getFirst().reviewCount());
    }

    @Test
    void listSkusSortsByRatingWithMultipleCategories() {
        Product product = product(UUID.randomUUID());
        Sku lowRating = sku(UUID.randomUUID(), product, "A", 10);
        Sku highRating = sku(UUID.randomUUID(), product, "B", 10);
        when(skuRepository.searchPublicCatalogForRating(eq(List.of("chuteiras", "camisas")), eq(2), isNull()))
                .thenReturn(List.of(lowRating, highRating));
        when(reviewRepository.findStatsBySkuIds(any())).thenReturn(List.of(
                stats(lowRating.getId(), 3.0, 2L),
                stats(highRating.getId(), 4.75, 4L)
        ));

        var response = catalogService.listSkus(List.of(" chuteiras, camisas "), null, 1, 12, "rating");

        assertEquals(highRating.getId(), response.items().getFirst().id());
        assertEquals(4.8, response.items().getFirst().rating());
        assertEquals(4L, response.items().getFirst().reviewCount());
    }

    @Test
    void findProductSelectsRequestedSku() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        Sku first = sku(UUID.randomUUID(), product, "Chuteira Campo 40", 10);
        Sku second = sku(UUID.randomUUID(), product, "Chuteira Campo 41", 3);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(eq(productId), any(Sort.class))).thenReturn(List.of(first, second));

        var response = catalogService.findProduct(productId, second.getId());

        assertEquals(second.getId(), response.selectedSkuId());
        assertEquals(2, response.skus().size());
    }

    @Test
    void findProductChoosesFirstInStockSkuWhenSkuIdIsMissing() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        Sku outOfStock = sku(UUID.randomUUID(), product, "A sem estoque", 0);
        Sku inStock = sku(UUID.randomUUID(), product, "B em estoque", 2);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(eq(productId), any(Sort.class))).thenReturn(List.of(outOfStock, inStock));

        var response = catalogService.findProduct(productId, null);

        assertEquals(inStock.getId(), response.selectedSkuId());
    }

    @Test
    void findProductRejectsSkuThatDoesNotBelongToProduct() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        Sku sku = sku(UUID.randomUUID(), product, "Chuteira Campo 40", 10);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(eq(productId), any(Sort.class))).thenReturn(List.of(sku));

        AppException exception = assertThrows(AppException.class, () -> catalogService.findProduct(productId, UUID.randomUUID()));

        assertEquals("RESOURCE_NOT_FOUND", exception.getCode());
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    private Product product(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));
        return product;
    }

    private Sku sku(UUID id, Product product, String title, int stock) {
        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle(title);
        sku.setDescription("Chuteira para campo");
        sku.setPrice(new BigDecimal("399.90"));
        sku.setOriginalPrice(new BigDecimal("499.90"));
        sku.setPhoto("https://example.com/photo.jpg");
        sku.setStock(stock);
        sku.setAttributes(Map.of("size", "40"));
        return sku;
    }

    private SkuReviewStatsProjection stats(UUID skuId, double rating, long reviewCount) {
        return new SkuReviewStatsProjection() {
            @Override
            public UUID getSkuId() {
                return skuId;
            }

            @Override
            public Double getRating() {
                return rating;
            }

            @Override
            public Long getReviewCount() {
                return reviewCount;
            }
        };
    }
}
