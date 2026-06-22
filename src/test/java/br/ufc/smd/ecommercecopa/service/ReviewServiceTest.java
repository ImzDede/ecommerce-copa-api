package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.review.CreateReviewRequest;
import br.ufc.smd.ecommercecopa.dto.review.UpdateReviewRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Review;
import br.ufc.smd.ecommercecopa.model.ReviewId;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.ReviewRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private HttpSession session;

    private ReviewService reviewService;
    private Client client;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(authService, reviewRepository, skuRepository);
        client = client(UUID.randomUUID());
    }

    @Test
    void createRejectsDuplicateReviewForSku() {
        UUID skuId = UUID.randomUUID();
        when(authService.requireClient(session)).thenReturn(client);
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku(skuId)));
        when(reviewRepository.existsById(new ReviewId(client.getUserId(), skuId))).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> reviewService.create(
                new CreateReviewRequest(skuId, 5, "Muito bom"),
                session
        ));

        assertEquals("DUPLICATE_RESOURCE", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createSavesReviewLinkedToSku() {
        UUID skuId = UUID.randomUUID();
        when(authService.requireClient(session)).thenReturn(client);
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku(skuId)));
        when(reviewRepository.existsById(new ReviewId(client.getUserId(), skuId))).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "createdAt", LocalDateTime.parse("2026-01-01T10:00:00"));
            return review;
        });

        var response = reviewService.create(new CreateReviewRequest(skuId, 4, " Ótima qualidade "), session);

        assertEquals(client.getUserId(), response.clientId());
        assertEquals(skuId, response.skuId());
        assertEquals(4, response.stars());
        assertEquals("Ótima qualidade", response.comment());
    }

    @Test
    void updateRejectsEmptyBody() {
        when(authService.requireClient(session)).thenReturn(client);

        AppException exception = assertThrows(AppException.class, () -> reviewService.update(
                UUID.randomUUID(),
                new UpdateReviewRequest(null, null),
                session
        ));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    private Client client(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Maria Silva");

        Client client = new Client();
        ReflectionTestUtils.setField(client, "userId", id);
        client.setUser(user);
        client.setCpf("12345678901");
        client.setDateOfBirth(LocalDate.parse("2000-01-01"));
        return client;
    }

    private Sku sku(UUID id) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setTitle("Chuteiras");
        category.setSlug("chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        product.setCategory(category);
        product.setSchema(Map.of("selectors", java.util.List.of(Map.of("key", "size", "label", "Tamanho"))));

        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle("Chuteira Campo 40");
        sku.setDescription("Chuteira para campo");
        sku.setPrice(new BigDecimal("399.90"));
        sku.setStock(10);
        sku.setAttributes(Map.of("size", "40"));
        return sku;
    }
}
