package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.sku.CreateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.sku.CreateSkuRequest;
import br.ufc.smd.ecommercecopa.dto.sku.UpdateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.sku.UpdateSkuRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SkuServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private TagService tagService;

    @Mock
    private HttpSession session;

    private SkuService skuService;

    @BeforeEach
    void setUp() {
        skuService = new SkuService(authService, skuRepository, productRepository, uploadService, new ObjectMapper(), tagService);
        doReturn(new User()).when(authService).requireAdmin(session);
    }

    @Test
    void createRejectsAttributesThatDoNotMatchProductSchema() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

        AppException exception = assertThrows(AppException.class, () -> skuService.create(
                new CreateSkuRequest(
                        productId,
                        "Chuteira Campo 40",
                        "Chuteira para campo",
                        new BigDecimal("399.90"),
                        new BigDecimal("499.90"),
                        "https://example.com/photo.jpg",
                        10,
                        Map.of("edition", "Campo")
                ),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
        verify(skuRepository, never()).save(any(Sku.class));
    }

    @Test
    void createSavesSkuWhenAttributesMatchProductSchema() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
            Sku sku = invocation.getArgument(0);
            ReflectionTestUtils.setField(sku, "id", UUID.randomUUID());
            return sku;
        });

        var response = skuService.create(new CreateSkuRequest(
                productId,
                " Chuteira Campo 40 ",
                " Chuteira para campo ",
                new BigDecimal("399.90"),
                new BigDecimal("499.90"),
                " https://example.com/photo.jpg ",
                10,
                Map.of("edition", " Campo ", "size", "40")
        ), session);

        assertNotNull(response.id());
        assertEquals("Chuteira Campo 40", response.title());
        assertEquals("Chuteira para campo", response.description());
        assertEquals("https://example.com/photo.jpg", response.photo());
        assertEquals("Campo", response.attributes().get("edition"));
    }

    @Test
    void createFormUploadsPhotoAndParsesAttributesJson() {
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));
        when(uploadService.saveImage(any(), eq("products"))).thenReturn("/uploads/products/photo.jpg");
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> {
            Sku sku = invocation.getArgument(0);
            ReflectionTestUtils.setField(sku, "id", UUID.randomUUID());
            return sku;
        });

        CreateSkuFormRequest request = new CreateSkuFormRequest();
        request.setProductId(productId);
        request.setTitle("Chuteira Campo 40");
        request.setDescription("Chuteira para campo");
        request.setPrice(new BigDecimal("399.90"));
        request.setOriginalPrice(new BigDecimal("499.90"));
        request.setStock(10);
        request.setAttributes("{\"edition\":\"Campo\",\"size\":\"40\"}");
        request.setPhoto(new MockMultipartFile("photo", "photo.jpg", "image/jpeg", new byte[]{1}));

        var response = skuService.create(request, session);

        assertEquals("/uploads/products/photo.jpg", response.photo());
        assertEquals("Campo", response.attributes().get("edition"));
        verify(uploadService).saveImage(any(), eq("products"));
    }

    @Test
    void updateRejectsCurrentPriceGreaterThanExistingOriginalPrice() {
        UUID skuId = UUID.randomUUID();
        Sku sku = sku(skuId, product(UUID.randomUUID()));
        sku.setPrice(new BigDecimal("100.00"));
        sku.setOriginalPrice(new BigDecimal("150.00"));
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku));

        AppException exception = assertThrows(AppException.class, () -> skuService.update(
                skuId,
                new UpdateSkuRequest(null, null, null, new BigDecimal("200.00"), null, null, null, null),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(skuRepository, never()).save(any(Sku.class));
    }

    @Test
    void updateFormRejectsUploadingAndRemovingPhotoTogether() {
        UpdateSkuFormRequest request = new UpdateSkuFormRequest();
        request.setRemovePhoto(true);
        request.setPhoto(new MockMultipartFile("photo", "photo.jpg", "image/jpeg", new byte[]{1}));

        AppException exception = assertThrows(AppException.class, () -> skuService.update(UUID.randomUUID(), request, session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(uploadService, never()).saveImage(any(), eq("products"));
        verify(skuRepository, never()).save(any(Sku.class));
    }

    @Test
    void updateFormIgnoresBlankOptionalTextFields() {
        UUID skuId = UUID.randomUUID();
        Sku sku = sku(skuId, product(UUID.randomUUID()));
        sku.setPrice(new BigDecimal("100.00"));
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku));
        when(skuRepository.save(any(Sku.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateSkuFormRequest request = new UpdateSkuFormRequest();
        request.setTitle("");
        request.setDescription(" ");
        request.setStock(5);

        var response = skuService.update(skuId, request, session);

        assertEquals("Chuteira Campo 40", response.title());
        assertEquals("Chuteira para campo", response.description());
        assertEquals(5, response.stock());
    }

    @Test
    void updateRejectsTooShortTitleWhenProvided() {
        UUID skuId = UUID.randomUUID();
        Sku sku = sku(skuId, product(UUID.randomUUID()));
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku));

        AppException exception = assertThrows(AppException.class, () -> skuService.update(
                skuId,
                new UpdateSkuRequest(null, "A", null, null, null, null, null, null),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(skuRepository, never()).save(any(Sku.class));
    }

    @Test
    void deleteSetsDeletedAt() {
        UUID skuId = UUID.randomUUID();
        Sku sku = sku(skuId, product(UUID.randomUUID()));
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku));

        skuService.delete(skuId, session);

        assertNotNull(sku.getDeletedAt());
    }

    private Product product(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", id);
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(
                Map.of("key", "edition", "label", "Edição"),
                Map.of("key", "size", "label", "Tamanho")
        )));
        return product;
    }

    private Sku sku(UUID id, Product product) {
        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle("Chuteira Campo 40");
        sku.setDescription("Chuteira para campo");
        sku.setStock(10);
        sku.setAttributes(Map.of("edition", "Campo", "size", "40"));
        return sku;
    }
}
