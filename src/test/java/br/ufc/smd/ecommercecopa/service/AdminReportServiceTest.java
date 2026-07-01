package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.repository.OrderRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminReportServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private HttpSession session;

    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        adminReportService = new AdminReportService(authService, orderRepository, skuRepository, "uploads");
        when(authService.requireAdmin(session)).thenReturn(new User());
    }

    @Test
    void purchasesByClientUsesInclusiveDateRange() {
        UUID clientId = UUID.randomUUID();
        when(orderRepository.findReportOrders(
                LocalDateTime.parse("2026-01-01T00:00:00"),
                LocalDateTime.parse("2026-02-01T00:00:00"),
                OrderStatus.CANCELED
        )).thenReturn(List.of(
                order(LocalDateTime.parse("2026-01-10T10:00:00"), new BigDecimal("100.00"), clientId, "Maria"),
                order(LocalDateTime.parse("2026-01-11T10:00:00"), new BigDecimal("200.00"), clientId, "Maria")
        ));

        var response = adminReportService.purchasesByClient("2026-01-01", "2026-01-31", session);

        assertEquals(1, response.items().size());
        assertEquals(clientId, response.items().getFirst().clientId());
        assertEquals(new BigDecimal("300.00"), response.items().getFirst().totalValue());
    }

    @Test
    void dailyRevenueAggregatesOrdersByDay() {
        when(orderRepository.findReportOrders(
                LocalDateTime.parse("2026-01-01T00:00:00"),
                LocalDateTime.parse("2026-01-03T00:00:00"),
                OrderStatus.CANCELED
        )).thenReturn(List.of(
                order(LocalDateTime.parse("2026-01-01T10:00:00"), new BigDecimal("150.00")),
                order(LocalDateTime.parse("2026-01-01T12:00:00"), new BigDecimal("50.00"))
        ));

        var response = adminReportService.dailyRevenue("2026-01-01", "2026-01-02", session);

        assertEquals("2026-01-01", response.items().getFirst().day());
        assertEquals(new BigDecimal("200.00"), response.items().getFirst().totalValue());
    }

    @Test
    void outOfStockSkusMapsActiveSkus() {
        Sku sku = sku(UUID.randomUUID());
        when(skuRepository.findOutOfStockSkus()).thenReturn(List.of(sku));

        var response = adminReportService.outOfStockSkus(session);

        assertEquals(sku.getId(), response.items().getFirst().skuId());
        assertEquals("/uploads/products/chuteira-1.webp", response.items().getFirst().photo());
        assertEquals("Chuteira para campo", response.items().getFirst().description());
        assertEquals("Chuteiras", response.items().getFirst().categoryTitle());
    }

    @Test
    void reportsRejectInvalidDateRange() {
        AppException exception = assertThrows(AppException.class,
                () -> adminReportService.dailyRevenue("2026-02-01", "2026-01-01", session));

        assertEquals("VALIDATION_ERROR", exception.getCode());
    }

    private Order order(LocalDateTime createdAt, BigDecimal totalValue) {
        Order order = new Order();
        ReflectionTestUtils.setField(order, "createdAt", createdAt);
        order.setTotalValue(totalValue);
        return order;
    }

    private Order order(LocalDateTime createdAt, BigDecimal totalValue, UUID clientId, String clientName) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", clientId);
        user.setName(clientName);

        Client client = new Client();
        ReflectionTestUtils.setField(client, "userId", clientId);
        client.setUser(user);

        Order order = order(createdAt, totalValue);
        order.setClient(client);
        return order;
    }

    private Sku sku(UUID id) {
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
        sku.setPrice(new BigDecimal("399.90"));
        sku.setStock(0);
        sku.setAttributes(Map.of("size", "40"));
        sku.setPhotos(List.of("/uploads/products/chuteira-1.webp", "/uploads/products/chuteira-2.webp"));
        return sku;
    }
}
