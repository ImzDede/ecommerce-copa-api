package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.order.CreateOrderRequest;
import br.ufc.smd.ecommercecopa.dto.order.UpdateOrderStatusRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Address;
import br.ufc.smd.ecommercecopa.model.Cart;
import br.ufc.smd.ecommercecopa.model.CartId;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderItem;
import br.ufc.smd.ecommercecopa.model.OrderItemId;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.AddressRepository;
import br.ufc.smd.ecommercecopa.repository.CartRepository;
import br.ufc.smd.ecommercecopa.repository.OrderItemRepository;
import br.ufc.smd.ecommercecopa.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
class OrderServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private HttpSession session;

    private OrderService orderService;
    private Client client;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(authService, addressRepository, cartRepository, orderRepository, orderItemRepository);
        client = client(UUID.randomUUID());
    }

    @Test
    void createBuildsOrderFromCartAndRestoresCart() {
        UUID addressId = UUID.randomUUID();
        Sku sku = sku(UUID.randomUUID(), 5, new BigDecimal("100.00"));
        Address address = address(addressId, client);
        Cart cart = cart(client, sku, 2);
        when(authService.requireClient(session)).thenReturn(client);
        when(addressRepository.findByIdAndClient_UserIdAndDeletedAtIsNull(addressId, client.getUserId())).thenReturn(Optional.of(address));
        when(cartRepository.findByClient_UserIdOrderByCreatedAtAsc(client.getUserId())).thenReturn(List.of(cart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.parse("2026-01-01T10:00:00"));
            return order;
        });

        var response = orderService.create(new CreateOrderRequest(addressId), session);

        assertEquals("PROCESSING", response.status());
        assertEquals(new BigDecimal("200.00"), response.totalValue());
        assertEquals(3, sku.getStock());
        assertEquals(1, response.items().size());
        verify(cartRepository).deleteByClient_UserId(client.getUserId());
        verify(orderItemRepository).saveAll(any());
    }

    @Test
    void cancelAdminSetsCanceledAndRestoresStock() {
        UUID orderId = UUID.randomUUID();
        Sku sku = sku(UUID.randomUUID(), 3, new BigDecimal("100.00"));
        Order order = order(orderId, client, address(UUID.randomUUID(), client), null);
        OrderItem item = orderItem(order, sku, 2);
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_IdOrderBySku_TitleAsc(orderId)).thenReturn(List.of(item));

        var response = orderService.cancelAdmin(orderId, session);

        assertEquals("CANCELED", response.status());
        assertEquals(5, sku.getStock());
    }

    @Test
    void updateStatusAdminPersistsNewStatus() {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId, client, address(UUID.randomUUID(), client), null);
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrder_IdOrderBySku_TitleAsc(orderId)).thenReturn(List.of());

        var response = orderService.updateStatusAdmin(orderId, new UpdateOrderStatusRequest("shipped"), session);

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
        assertEquals("SHIPPED", response.status());
    }

    @Test
    void cancelAdminRejectsAlreadyCanceledOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = order(orderId, client, address(UUID.randomUUID(), client), LocalDateTime.parse("2026-01-02T10:00:00"));
        when(authService.requireAdmin(session)).thenReturn(new User());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        AppException exception = assertThrows(AppException.class, () -> orderService.cancelAdmin(orderId, session));

        assertEquals("BUSINESS_RULE_VIOLATION", exception.getCode());
        verify(orderItemRepository, never()).findByOrder_IdOrderBySku_TitleAsc(orderId);
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

    private Address address(UUID id, Client client) {
        Address address = new Address();
        ReflectionTestUtils.setField(address, "id", id);
        address.setClient(client);
        address.setName("Casa");
        address.setStreet("Rua A");
        address.setNumber("123");
        address.setState("CE");
        address.setCity("Fortaleza");
        address.setNeighborhood("Centro");
        address.setPostalCode("60000-000");
        return address;
    }

    private Sku sku(UUID id, int stock, BigDecimal price) {
        Category category = new Category();
        ReflectionTestUtils.setField(category, "id", UUID.randomUUID());
        category.setSlug("chuteiras");
        category.setTitle("Chuteiras");

        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", UUID.randomUUID());
        product.setCategory(category);
        product.setSchema(Map.of("selectors", List.of(Map.of("key", "size", "label", "Tamanho"))));

        Sku sku = new Sku();
        ReflectionTestUtils.setField(sku, "id", id);
        sku.setProduct(product);
        sku.setTitle("Chuteira Campo 40");
        sku.setDescription("Chuteira para campo");
        sku.setPrice(price);
        sku.setStock(stock);
        sku.setAttributes(Map.of("size", "40"));
        return sku;
    }

    private Cart cart(Client client, Sku sku, int amount) {
        Cart cart = new Cart();
        cart.setId(new CartId(client.getUserId(), sku.getId()));
        cart.setClient(client);
        cart.setSku(sku);
        cart.setAmount(amount);
        return cart;
    }

    private Order order(UUID id, Client client, Address address, LocalDateTime deletedAt) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "id", id);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.parse("2026-01-01T10:00:00"));
        order.setClient(client);
        order.setAddress(address);
        order.setTotalValue(new BigDecimal("200.00"));
        order.setStatus(deletedAt == null ? OrderStatus.PROCESSING : OrderStatus.CANCELED);
        order.setDeletedAt(deletedAt);
        return order;
    }

    private OrderItem orderItem(Order order, Sku sku, int amount) {
        OrderItem item = new OrderItem();
        item.setId(new OrderItemId(order.getId(), sku.getId()));
        item.setOrder(order);
        item.setSku(sku);
        item.setPrice(sku.getPrice());
        item.setAmount(amount);
        return item;
    }
}
