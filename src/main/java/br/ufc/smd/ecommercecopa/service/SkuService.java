package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.sku.CreateSkuRequest;
import br.ufc.smd.ecommercecopa.dto.sku.CreateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.sku.SkuListResponse;
import br.ufc.smd.ecommercecopa.dto.sku.SkuResponse;
import br.ufc.smd.ecommercecopa.dto.sku.UpdateSkuFormRequest;
import br.ufc.smd.ecommercecopa.dto.sku.UpdateSkuRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SkuService {

    private static final TypeReference<Map<String, Object>> ATTRIBUTES_TYPE = new TypeReference<>() {
    };

    private final AuthService authService;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final UploadService uploadService;
    private final ObjectMapper objectMapper;
    private final TagService tagService;

    public SkuService(AuthService authService,
                      SkuRepository skuRepository,
                      ProductRepository productRepository,
                      UploadService uploadService,
                      ObjectMapper objectMapper,
                      TagService tagService) {
        this.authService = authService;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.uploadService = uploadService;
        this.objectMapper = objectMapper;
        this.tagService = tagService;
    }

    @Transactional(readOnly = true)
    public SkuListResponse list(UUID productId, String categorySlug, Boolean inStock, HttpSession session) {
        authService.requireAdmin(session);

        Sort sort = Sort.by(Sort.Direction.ASC, "title");
        List<Sku> skus;
        if (productId != null) {
            requireActiveProduct(productId);
            skus = skuRepository.findByProduct_IdAndDeletedAtIsNull(productId, sort);
        } else if (categorySlug != null && !categorySlug.isBlank()) {
            skus = skuRepository.findByProduct_Category_SlugAndDeletedAtIsNull(categorySlug.trim(), sort);
        } else {
            skus = skuRepository.findByDeletedAtIsNull(sort);
        }

        return new SkuListResponse(skus.stream()
                .filter(sku -> sku.getProduct().getDeletedAt() == null)
                .filter(sku -> matchesStockFilter(sku, inStock))
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public SkuResponse findById(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Sku sku = requireActiveSku(id);
        if (sku.getProduct().getDeletedAt() != null) {
            throw new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND);
        }
        return toResponse(sku);
    }

    @Transactional
    public SkuResponse create(CreateSkuFormRequest request, HttpSession session) {
        authService.requireAdmin(session);

        return createSku(
                request.getProductId(),
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getOriginalPrice(),
                saveOptionalPhoto(request.getPhoto()),
                request.getStock(),
                parseAttributes(request.getAttributes(), true)
        );
    }

    @Transactional
    public SkuResponse create(CreateSkuRequest request, HttpSession session) {
        authService.requireAdmin(session);

        return createSku(
                request.productId(),
                request.title(),
                request.description(),
                request.price(),
                request.originalPrice(),
                request.photo(),
                request.stock(),
                request.attributes()
        );
    }

    @Transactional
    public SkuResponse update(UUID id, UpdateSkuFormRequest request, HttpSession session) {
        authService.requireAdmin(session);

        boolean photoUploaded = hasUploadedPhoto(request.getPhoto());
        boolean removePhoto = Boolean.TRUE.equals(request.getRemovePhoto());
        if (photoUploaded && removePhoto) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Não é possível enviar e remover a foto no mesmo request", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, Object> parsedAttributes = request.getAttributes() != null ? parseAttributes(request.getAttributes(), false) : null;

        return updateSku(
                id,
                request.getProductId(),
                request.getTitle(),
                request.getDescription(),
                request.getPrice(),
                request.getOriginalPrice(),
                request.getOriginalPrice() != null,
                photoUploaded ? uploadService.saveImage(request.getPhoto(), "products") : null,
                photoUploaded,
                removePhoto,
                request.getStock(),
                parsedAttributes,
                parsedAttributes != null
        );
    }

    @Transactional
    public SkuResponse update(UUID id, UpdateSkuRequest request, HttpSession session) {
        authService.requireAdmin(session);

        return updateSku(
                id,
                request.productId(),
                request.title(),
                request.description(),
                request.price(),
                request.originalPrice(),
                request.originalPrice() != null,
                request.photo(),
                request.photo() != null,
                false,
                request.stock(),
                request.attributes(),
                request.attributes() != null
        );
    }

    private SkuResponse createSku(UUID productId,
                                  String title,
                                  String description,
                                  BigDecimal price,
                                  BigDecimal originalPrice,
                                  String photo,
                                  Integer stock,
                                  Map<String, Object> rawAttributes) {
        Product product = requireActiveProduct(productId);
        Map<String, Object> attributes = validateAttributes(product, rawAttributes);

        Sku sku = new Sku();
        sku.setProduct(product);
        sku.setTitle(title.trim());
        sku.setDescription(description.trim());
        sku.setPrice(price);
        sku.setOriginalPrice(validateOriginalPrice(price, originalPrice));
        sku.setPhoto(normalizeOptionalText(photo));
        sku.setStock(stock);
        sku.setAttributes(attributes);

        return toResponse(skuRepository.save(sku));
    }

    private SkuResponse updateSku(UUID id,
                                  UUID productId,
                                  String title,
                                  String description,
                                  BigDecimal price,
                                  BigDecimal originalPrice,
                                  boolean hasOriginalPrice,
                                  String photo,
                                  boolean hasPhoto,
                                  boolean removePhoto,
                                  Integer stock,
                                  Map<String, Object> rawAttributes,
                                  boolean hasAttributes) {
        boolean hasProduct = productId != null;
        boolean hasTitle = title != null && !title.isBlank();
        boolean hasDescription = description != null && !description.isBlank();
        boolean hasPrice = price != null;
        boolean hasStock = stock != null;

        if (!hasProduct && !hasTitle && !hasDescription && !hasPrice && !hasOriginalPrice && !hasPhoto && !removePhoto && !hasStock && !hasAttributes) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (hasPhoto && removePhoto) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Não é possível enviar e remover a foto no mesmo request", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Sku sku = requireActiveSku(id);
        String previousPhoto = sku.getPhoto();
        Product product = hasProduct ? requireActiveProduct(productId) : sku.getProduct();
        Map<String, Object> attributes = hasAttributes ? rawAttributes : sku.getAttributes();

        if (hasProduct) {
            sku.setProduct(product);
        }
        if (hasTitle) {
            sku.setTitle(normalizeTextRange(title, 2, 160, "Título deve ter entre 2 e 160 caracteres"));
        }
        if (hasDescription) {
            sku.setDescription(normalizeTextRange(description, 2, 2000, "Descrição deve ter entre 2 e 2000 caracteres"));
        }
        if (hasPrice) {
            sku.setPrice(price);
        }
        if (hasOriginalPrice) {
            sku.setOriginalPrice(validateOriginalPrice(sku.getPrice(), originalPrice));
        }
        if (hasPrice && !hasOriginalPrice) {
            validateOriginalPrice(sku.getPrice(), sku.getOriginalPrice());
        }
        if (removePhoto) {
            sku.setPhoto(null);
        } else if (hasPhoto) {
            sku.setPhoto(normalizeOptionalText(photo));
        }
        if (hasStock) {
            sku.setStock(stock);
        }
        if (hasAttributes || hasProduct) {
            sku.setAttributes(validateAttributes(product, attributes));
        }

        SkuResponse response = toResponse(skuRepository.save(sku));
        if ((removePhoto || hasPhoto) && previousPhoto != null && !previousPhoto.equals(sku.getPhoto())) {
            uploadService.deleteByPublicPath(previousPhoto);
        }
        return response;
    }

    @Transactional
    public void delete(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Sku sku = requireActiveSku(id);
        sku.setDeletedAt(LocalDateTime.now());
    }

    private Product requireActiveProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Produto não encontrado", HttpStatus.NOT_FOUND));
    }

    private Sku requireActiveSku(UUID id) {
        return skuRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
    }

    private SkuResponse toResponse(Sku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getProduct().getId(),
                sku.getTitle(),
                sku.getDescription(),
                sku.getPrice(),
                sku.getOriginalPrice(),
                sku.getPhoto(),
                sku.getStock(),
                sku.getAttributes(),
                tagService.listSkuTags(sku.getId())
        );
    }

    private boolean matchesStockFilter(Sku sku, Boolean inStock) {
        if (inStock == null) {
            return true;
        }
        return inStock ? sku.getStock() > 0 : sku.getStock() <= 0;
    }

    private BigDecimal validateOriginalPrice(BigDecimal price, BigDecimal originalPrice) {
        if (originalPrice != null && originalPrice.compareTo(price) <= 0) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Preço original deve ser maior que o preço atual", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return originalPrice;
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeTextRange(String value, int minLength, int maxLength, String message) {
        String normalized = value.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return normalized;
    }

    private String saveOptionalPhoto(MultipartFile photo) {
        if (!hasUploadedPhoto(photo)) {
            return null;
        }
        return uploadService.saveImage(photo, "products");
    }

    private boolean hasUploadedPhoto(MultipartFile photo) {
        return photo != null && !photo.isEmpty();
    }

    private Map<String, Object> parseAttributes(String value, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) {
                throw new AppException("VALIDATION_ERROR", "Atributos são obrigatórios", HttpStatus.BAD_REQUEST);
            }
            return null;
        }

        try {
            return objectMapper.readValue(value, ATTRIBUTES_TYPE);
        } catch (JsonProcessingException ex) {
            throw new AppException("VALIDATION_ERROR", "Atributos devem ser um JSON válido", HttpStatus.BAD_REQUEST);
        }
    }

    private Map<String, Object> validateAttributes(Product product, Map<String, Object> attributes) {
        if (attributes == null) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Atributos do SKU são obrigatórios", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> selectorKeys = extractSelectorKeys(product);
        Set<String> attributeKeys = attributes.keySet();

        if (!attributeKeys.equals(selectorKeys)) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Atributos do SKU não são compatíveis com o schema do produto", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        for (String key : selectorKeys) {
            Object value = attributes.get(key);
            if (value == null || value instanceof String text && text.isBlank()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Atributos do SKU não podem ser vazios", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            normalized.put(key, value instanceof String text ? text.trim() : value);
        }

        return normalized;
    }

    private Set<String> extractSelectorKeys(Product product) {
        Object selectorsObj = product.getSchema().get("selectors");
        if (!(selectorsObj instanceof List<?> selectors)) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Schema do produto está inválido", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> keys = new HashSet<>();
        for (Object selectorObj : selectors) {
            if (!(selectorObj instanceof Map<?, ?> selector) || !(selector.get("key") instanceof String key)) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Schema do produto está inválido", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            keys.add(key);
        }

        return keys;
    }
}
