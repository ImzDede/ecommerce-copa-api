package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.product.CreateProductRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private HttpSession session;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(authService, productRepository, categoryRepository, skuRepository);
        doReturn(new User()).when(authService).requireAdmin(session);
    }

    @Test
    void createRejectsSchemaWithoutSelectors() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(category(categoryId)));

        AppException exception = assertThrows(AppException.class, () -> productService.create(
                new CreateProductRequest(categoryId, Map.of("fields", List.of())),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createRejectsRepeatedSelectorKeys() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(category(categoryId)));

        AppException exception = assertThrows(AppException.class, () -> productService.create(
                new CreateProductRequest(categoryId, Map.of("selectors", List.of(
                        Map.of("key", "size", "label", "Tamanho"),
                        Map.of("key", "size", "label", "Numeração")
                ))),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
    }

    @Test
    void deleteSoftDeletesProductAndLinkedActiveSkus() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku firstSku = sku(UUID.randomUUID(), product);
        Sku secondSku = sku(UUID.randomUUID(), product);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(productId)).thenReturn(List.of(firstSku, secondSku));

        productService.delete(productId, session);

        assertNotNull(product.getDeletedAt());
        assertEquals(product.getDeletedAt(), firstSku.getDeletedAt());
        assertEquals(product.getDeletedAt(), secondSku.getDeletedAt());
    }

    private Category category(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", id);
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");
        return category;
    }

    private Product product(UUID id, Category category) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));
        return product;
    }

    private Sku sku(UUID id, Product product) {
        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        return sku;
    }
}
