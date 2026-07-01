package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.address.AddressResponse;
import br.ufc.smd.ecommercecopa.dto.order.CreateOrderRequest;
import br.ufc.smd.ecommercecopa.dto.order.OrderItemResponse;
import br.ufc.smd.ecommercecopa.dto.order.OrderListResponse;
import br.ufc.smd.ecommercecopa.dto.order.OrderResponse;
import br.ufc.smd.ecommercecopa.dto.order.UpdateOrderStatusRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Address;
import br.ufc.smd.ecommercecopa.model.Cart;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Order;
import br.ufc.smd.ecommercecopa.model.OrderItem;
import br.ufc.smd.ecommercecopa.model.OrderItemId;
import br.ufc.smd.ecommercecopa.model.OrderStatus;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.AddressRepository;
import br.ufc.smd.ecommercecopa.repository.CartRepository;
import br.ufc.smd.ecommercecopa.repository.OrderItemRepository;
import br.ufc.smd.ecommercecopa.repository.OrderRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final AuthService authService;
    private final AddressRepository addressRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(AuthService authService,
                        AddressRepository addressRepository,
                        CartRepository cartRepository,
                        OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository) {
        this.authService = authService;
        this.addressRepository = addressRepository;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        Address address = addressRepository.findByIdAndClient_UserIdAndDeletedAtIsNull(request.addressId(), client.getUserId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Endereço não encontrado", HttpStatus.NOT_FOUND));
        List<Cart> cartItems = cartRepository.findByClient_UserIdOrderByCreatedAtAsc(client.getUserId());

        if (cartItems.isEmpty()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Carrinho está vazio", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Cart cart : cartItems) {
            Sku sku = cart.getSku();
            validatePurchasableCartItem(cart, sku);
            totalValue = totalValue.add(sku.getPrice().multiply(BigDecimal.valueOf(cart.getAmount())));
        }

        Order order = new Order();
        order.setClient(client);
        order.setAddress(address);
        order.setTotalValue(totalValue);
        order.setStatus(OrderStatus.PROCESSING);
        Order savedOrder = orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (Cart cart : cartItems) {
            Sku sku = cart.getSku();
            sku.setStock(sku.getStock() - cart.getAmount());

            OrderItem item = new OrderItem();
            item.setId(new OrderItemId(savedOrder.getId(), sku.getId()));
            item.setOrder(savedOrder);
            item.setSku(sku);
            item.setPrice(sku.getPrice());
            item.setAmount(cart.getAmount());
            orderItems.add(item);
        }

        orderItemRepository.saveAll(orderItems);
        cartRepository.deleteByClient_UserId(client.getUserId());

        return toResponse(savedOrder, orderItems);
    }

    @Transactional(readOnly = true)
    public OrderListResponse listMine(HttpSession session) {
        Client client = authService.requireClient(session);
        return new OrderListResponse(orderRepository.findByClient_UserIdOrderByCreatedAtDesc(client.getUserId()).stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse findMine(UUID id, HttpSession session) {
        Client client = authService.requireClient(session);
        Order order = orderRepository.findByIdAndClient_UserId(id, client.getUserId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Pedido não encontrado", HttpStatus.NOT_FOUND));
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderListResponse listAdmin(HttpSession session) {
        authService.requireAdmin(session);
        return new OrderListResponse(orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse findAdmin(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Order order = requireOrder(id);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatusAdmin(UUID id, UpdateOrderStatusRequest request, HttpSession session) {
        authService.requireAdmin(session);
        OrderStatus status = parseStatus(request.status());
        if (status == OrderStatus.CANCELED) {
            return cancelAdmin(id, session);
        }

        Order order = requireOrder(id);
        if (effectiveStatus(order) == OrderStatus.CANCELED) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Pedido cancelado não pode voltar para outro status", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        order.setStatus(status);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse cancelAdmin(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Order order = requireOrder(id);
        if (effectiveStatus(order) == OrderStatus.CANCELED) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Pedido já está cancelado", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        List<OrderItem> items = orderItemRepository.findByOrder_IdOrderBySku_TitleAsc(order.getId());
        for (OrderItem item : items) {
            Sku sku = item.getSku();
            sku.setStock(sku.getStock() + item.getAmount());
        }
        order.setStatus(OrderStatus.CANCELED);
        order.setDeletedAt(LocalDateTime.now());

        return toResponse(order, items);
    }

    private Order requireOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Pedido não encontrado", HttpStatus.NOT_FOUND));
    }

    private void validatePurchasableCartItem(Cart cart, Sku sku) {
        if (sku.getDeletedAt() != null || sku.getProduct().getDeletedAt() != null) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Carrinho possui SKU indisponível", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (cart.getAmount() == null || cart.getAmount() < 1) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Carrinho possui quantidade inválida", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (sku.getStock() < cart.getAmount()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Estoque insuficiente para finalizar o pedido", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private OrderResponse toResponse(Order order) {
        return toResponse(order, orderItemRepository.findByOrder_IdOrderBySku_TitleAsc(order.getId()));
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        return new OrderResponse(
                order.getId(),
                order.getClient().getUserId(),
                order.getClient().getUser().getName(),
                toAddressResponse(order.getAddress()),
                order.getTotalValue(),
                effectiveStatus(order).name(),
                order.getCreatedAt().toString(),
                order.getDeletedAt() == null ? null : order.getDeletedAt().toString(),
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        Sku sku = item.getSku();
        return new OrderItemResponse(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getTitle(),
                item.getPrice(),
                item.getAmount(),
                item.getPrice().multiply(BigDecimal.valueOf(item.getAmount())),
                firstPhoto(sku),
                sku.getAttributes()
        );
    }

    private String firstPhoto(Sku sku) {
        List<String> photos = sku.getPhotos();
        return photos == null || photos.isEmpty() ? null : photos.getFirst();
    }

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getName(),
                address.getStreet(),
                address.getNumber(),
                address.getState(),
                address.getCity(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getPostalCode(),
                address.isDefaultAddress()
        );
    }

    private OrderStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException("VALIDATION_ERROR", "Status é obrigatório", HttpStatus.BAD_REQUEST);
        }

        try {
            return OrderStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AppException("VALIDATION_ERROR", "Status inválido", HttpStatus.BAD_REQUEST);
        }
    }

    private OrderStatus effectiveStatus(Order order) {
        if (order.getDeletedAt() != null) {
            return OrderStatus.CANCELED;
        }
        return order.getStatus() == null ? OrderStatus.PROCESSING : order.getStatus();
    }
}
