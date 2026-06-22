package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.cart.CartItemRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Cart;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.CartRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private HttpSession session;

    private CartService cartService;
    private Client client;

    @BeforeEach
    void setUp() {
        cartService = new CartService(authService, cartRepository, skuRepository);
        client = client(UUID.randomUUID());
    }

    @Test
    void addItemCreatesCartItemAndCalculatesTotal() {
        UUID skuId = UUID.randomUUID();
        Sku sku = sku(skuId, 10);
        List<Cart> savedItems = new ArrayList<>();
        when(authService.requireClient(session)).thenReturn(client);
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku));
        when(cartRepository.findById(any())).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> {
            Cart cart = invocation.getArgument(0);
            savedItems.clear();
            savedItems.add(cart);
            return cart;
        });
        when(cartRepository.findByClient_UserIdOrderByCreatedAtAsc(client.getUserId())).thenAnswer(invocation -> savedItems);

        var response = cartService.addItem(new CartItemRequest(skuId, 2), session);

        assertEquals(1, response.items().size());
        assertEquals(2, response.items().getFirst().amount());
        assertEquals(new BigDecimal("799.80"), response.totalValue());
    }

    @Test
    void addItemRejectsAmountGreaterThanStock() {
        UUID skuId = UUID.randomUUID();
        when(authService.requireClient(session)).thenReturn(client);
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku(skuId, 3)));
        when(cartRepository.findById(any())).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> cartService.addItem(new CartItemRequest(skuId, 4), session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void addItemRejectsOutOfStockSku() {
        UUID skuId = UUID.randomUUID();
        when(authService.requireClient(session)).thenReturn(client);
        when(skuRepository.findByIdAndDeletedAtIsNull(skuId)).thenReturn(Optional.of(sku(skuId, 0)));

        AppException exception = assertThrows(AppException.class, () -> cartService.addItem(new CartItemRequest(skuId, 1), session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        assertTrue(exception.getMessage().contains("sem estoque"));
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

    private Sku sku(UUID id, int stock) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setTitle("Chuteiras");
        category.setSlug("chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));

        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle("Chuteira Campo 40");
        sku.setDescription("Chuteira para campo");
        sku.setPrice(new BigDecimal("399.90"));
        sku.setStock(stock);
        sku.setAttributes(Map.of("size", "40"));
        return sku;
    }
}
