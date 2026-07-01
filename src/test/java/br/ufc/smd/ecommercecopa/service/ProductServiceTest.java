package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.product.ProductAdminRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductUpdateRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductVariantPhotoOrderRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductVariantRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductVariantUpsertRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import br.ufc.smd.ecommercecopa.service.UploadService;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
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
    private UploadService uploadService;

    @Mock
    private HttpSession session;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(authService, productRepository, categoryRepository, skuRepository, uploadService);
        doReturn(new User()).when(authService).requireAdmin(session);
    }

    @Test
    void createRejectsProductWithoutVariants() {
        UUID categoryId = UUID.randomUUID();

        ProductAdminRequest request = new ProductAdminRequest(
                "Produto Teste", categoryId, List.of(), List.of()
        );

        AppException exception = assertThrows(AppException.class, () -> productService.createAtomic(
                request, null, session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createRejectsRepeatedSelectorKeys() {
        UUID categoryId = UUID.randomUUID();
        when(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).thenReturn(Optional.of(category(categoryId)));

        ProductAdminRequest request = new ProductAdminRequest(
                "Produto Teste", categoryId, List.of(
                        new ProductAdminRequest.OptionRequest("size", "Tamanho"),
                        new ProductAdminRequest.OptionRequest("size", "Numeração")
                ), List.of(validVariant(Map.of("size", "40")))
        );

        AppException exception = assertThrows(AppException.class, () -> productService.createAtomic(
                request, null, session
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

    @Test
    void updateMetadataDoesNotDeleteVariantsAndSyncsSingleOptionlessSkuTitle() {
        UUID productId = UUID.randomUUID();
        Product product = optionlessProduct(productId, category(UUID.randomUUID()));
        product.setName("Nome Antigo");
        Sku sku = sku(UUID.randomUUID(), product);
        sku.setTitle("Nome Antigo");
        sku.setPhotos(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"));
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(productId)).thenReturn(List.of(sku));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(any(UUID.class), any(Sort.class))).thenReturn(List.of(sku));

        var response = productService.updateMetadata(productId, new ProductUpdateRequest("Nome Novo", null), session);

        assertEquals("Nome Novo", product.getName());
        assertEquals("Nome Novo", sku.getTitle());
        assertNull(sku.getDeletedAt());
        assertEquals(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"), response.variants().getFirst().photos());
        verify(productRepository).save(product);
        verify(skuRepository).save(sku);
    }

    @Test
    void updateVariantDoesNotTouchOtherVariants() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        target.setTitle("Variante antiga");
        target.setPhotos(List.of("/uploads/products/foto-1.webp"));
        Sku untouched = sku(UUID.randomUUID(), product);
        untouched.setTitle("Outra variante");
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByIdAndDeletedAtIsNull(target.getId())).thenReturn(Optional.of(target));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(any(UUID.class), any(Sort.class))).thenReturn(List.of(target, untouched));

        productService.updateVariant(productId, target.getId(), new ProductVariantUpsertRequest(
                "Variante nova",
                "Descrição atualizada",
                new BigDecimal("20.00"),
                new BigDecimal("30.00"),
                8,
                Map.of("size", "M")
        ), session);

        assertEquals("Variante nova", target.getTitle());
        assertEquals(new BigDecimal("20.00"), target.getPrice());
        assertEquals(8, target.getStock());
        assertEquals(List.of("/uploads/products/foto-1.webp"), target.getPhotos());
        assertEquals("Outra variante", untouched.getTitle());
        assertNull(untouched.getDeletedAt());
        verify(skuRepository).save(target);
        verify(skuRepository, never()).save(untouched);
    }

    @Test
    void addVariantPhotoAppendsPhoto() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        target.setPhotos(List.of("/uploads/products/foto-1.webp"));
        MockMultipartFile image = new MockMultipartFile("image", "foto.webp", "image/webp", new byte[]{1});
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByIdAndDeletedAtIsNull(target.getId())).thenReturn(Optional.of(target));
        when(uploadService.saveImage(any(), eq("products"))).thenReturn("/uploads/products/foto-2.webp");
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(any(UUID.class), any(Sort.class))).thenReturn(List.of(target));

        var response = productService.addVariantPhoto(productId, target.getId(), image, session);

        assertEquals(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"), target.getPhotos());
        assertEquals(target.getPhotos(), response.variants().getFirst().photos());
        verify(skuRepository).save(target);
    }

    @Test
    void deleteVariantPhotoRemovesOnlySelectedPhoto() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        target.setPhotos(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"));
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByIdAndDeletedAtIsNull(target.getId())).thenReturn(Optional.of(target));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(any(UUID.class), any(Sort.class))).thenReturn(List.of(target));

        productService.deleteVariantPhoto(productId, target.getId(), "/uploads/products/foto-1.webp", session);

        assertEquals(List.of("/uploads/products/foto-2.webp"), target.getPhotos());
        verify(uploadService).deleteByPublicPath("/uploads/products/foto-1.webp");
        verify(skuRepository).save(target);
    }

    @Test
    void reorderVariantPhotosAcceptsSamePhotosInNewOrder() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        target.setPhotos(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"));
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByIdAndDeletedAtIsNull(target.getId())).thenReturn(Optional.of(target));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(any(UUID.class), any(Sort.class))).thenReturn(List.of(target));

        productService.reorderVariantPhotos(productId, target.getId(), new ProductVariantPhotoOrderRequest(
                List.of("/uploads/products/foto-2.webp", "/uploads/products/foto-1.webp")
        ), session);

        assertEquals(List.of("/uploads/products/foto-2.webp", "/uploads/products/foto-1.webp"), target.getPhotos());
        verify(skuRepository).save(target);
    }

    @Test
    void reorderVariantPhotosRejectsMissingOrExtraPhotos() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        target.setPhotos(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"));
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByIdAndDeletedAtIsNull(target.getId())).thenReturn(Optional.of(target));

        AppException exception = assertThrows(AppException.class, () -> productService.reorderVariantPhotos(
                productId,
                target.getId(),
                new ProductVariantPhotoOrderRequest(List.of("/uploads/products/foto-1.webp")),
                session
        ));

        assertEquals("VALIDATION_ERROR", exception.getCode());
        assertEquals(List.of("/uploads/products/foto-1.webp", "/uploads/products/foto-2.webp"), target.getPhotos());
        verify(skuRepository, never()).save(target);
    }

    @Test
    void deleteVariantRejectsRemovingLastActiveVariant() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku onlySku = sku(UUID.randomUUID(), product);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(productId)).thenReturn(List.of(onlySku));

        AppException exception = assertThrows(AppException.class,
                () -> productService.deleteVariant(productId, onlySku.getId(), session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertNull(onlySku.getDeletedAt());
        verify(skuRepository, never()).save(onlySku);
    }

    @Test
    void deleteVariantSoftDeletesOnlySelectedVariant() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId, category(UUID.randomUUID()));
        Sku target = sku(UUID.randomUUID(), product);
        Sku untouched = sku(UUID.randomUUID(), product);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.findByProduct_IdAndDeletedAtIsNull(productId)).thenReturn(List.of(target, untouched));

        productService.deleteVariant(productId, target.getId(), session);

        assertNotNull(target.getDeletedAt());
        assertNull(untouched.getDeletedAt());
        verify(skuRepository).save(target);
        verify(skuRepository, never()).save(untouched);
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
        product.setName("Produto Teste");
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));
        return product;
    }

    private Product optionlessProduct(UUID id, Category category) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setName("Produto Teste");
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of()));
        return product;
    }

    private Sku sku(UUID id, Product product) {
        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        return sku;
    }

    private ProductVariantRequest validVariant(Map<String, Object> attributes) {
        return new ProductVariantRequest(null, "Variante", "Descrição da variante", new BigDecimal("10.00"), null, 5, attributes, List.of());
    }
}
