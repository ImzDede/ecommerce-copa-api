package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.cart.CartItemRequest;
import br.ufc.smd.ecommercecopa.dto.cart.CartItemResponse;
import br.ufc.smd.ecommercecopa.dto.cart.CartResponse;
import br.ufc.smd.ecommercecopa.dto.cart.UpdateCartItemRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Cart;
import br.ufc.smd.ecommercecopa.model.CartId;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.CartRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final AuthService authService;
    private final CartRepository cartRepository;
    private final SkuRepository skuRepository;

    public CartService(AuthService authService, CartRepository cartRepository, SkuRepository skuRepository) {
        this.authService = authService;
        this.cartRepository = cartRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getMyCart(HttpSession session) {
        Client client = authService.requireClient(session);
        return toResponse(cartRepository.findByClient_UserIdOrderByCreatedAtAsc(client.getUserId()));
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        Sku sku = requirePurchasableSku(request.skuId());
        CartId id = new CartId(client.getUserId(), sku.getId());
        Cart cart = cartRepository.findById(id).orElseGet(() -> newCart(id, client, sku));
        int nextAmount = cart.getAmount() == null ? request.amount() : cart.getAmount() + request.amount();

        validateAmountAgainstStock(nextAmount, sku);
        cart.setAmount(nextAmount);
        cartRepository.save(cart);

        return getMyCart(session);
    }

    @Transactional
    public CartResponse updateItem(UUID skuId, UpdateCartItemRequest request, HttpSession session) {
        Client client = authService.requireClient(session);
        Cart cart = requireOwnCartItem(client.getUserId(), skuId);
        Sku sku = requirePurchasableSku(skuId);

        validateAmountAgainstStock(request.amount(), sku);
        cart.setAmount(request.amount());
        cartRepository.save(cart);

        return getMyCart(session);
    }

    @Transactional
    public CartResponse deleteItem(UUID skuId, HttpSession session) {
        Client client = authService.requireClient(session);
        Cart cart = requireOwnCartItem(client.getUserId(), skuId);
        cartRepository.delete(cart);
        return getMyCart(session);
    }

    @Transactional
    public void clear(HttpSession session) {
        Client client = authService.requireClient(session);
        cartRepository.deleteByClient_UserId(client.getUserId());
    }

    private Cart newCart(CartId id, Client client, Sku sku) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setClient(client);
        cart.setSku(sku);
        cart.setAmount(0);
        return cart;
    }

    private Cart requireOwnCartItem(UUID clientId, UUID skuId) {
        return cartRepository.findById(new CartId(clientId, skuId))
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Item do carrinho não encontrado", HttpStatus.NOT_FOUND));
    }

    private Sku requirePurchasableSku(UUID skuId) {
        Sku sku = skuRepository.findByIdAndDeletedAtIsNull(skuId)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
        if (sku.getProduct().getDeletedAt() != null) {
            throw new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND);
        }
        if (sku.getStock() <= 0) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "SKU sem estoque disponível", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return sku;
    }

    private void validateAmountAgainstStock(Integer amount, Sku sku) {
        if (amount == null || amount < 1) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Quantidade deve ser maior que zero", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (amount > sku.getStock()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Quantidade solicitada excede o estoque disponível", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private CartResponse toResponse(List<Cart> items) {
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toItemResponse)
                .toList();
        BigDecimal totalValue = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(itemResponses, totalValue);
    }

    private CartItemResponse toItemResponse(Cart cart) {
        Sku sku = cart.getSku();
        BigDecimal subtotal = sku.getPrice().multiply(BigDecimal.valueOf(cart.getAmount()));
        return new CartItemResponse(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getTitle(),
                sku.getPrice(),
                sku.getPhoto(),
                sku.getStock(),
                cart.getAmount(),
                subtotal,
                sku.getAttributes()
        );
    }
}
