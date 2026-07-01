package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import br.ufc.smd.ecommercecopa.dto.product.*;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductService {

    private final AuthService authService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SkuRepository skuRepository;
    private final UploadService uploadService;

    public ProductService(AuthService authService,
                          ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SkuRepository skuRepository,
                          UploadService uploadService) {
        this.authService = authService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.skuRepository = skuRepository;
        this.uploadService = uploadService;
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> listSummary(String categorySlug, HttpSession session) {
        authService.requireAdmin(session);

        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        List<Product> products = categorySlug == null || categorySlug.isBlank()
                ? productRepository.findByDeletedAtIsNull(sort)
                : productRepository.findByCategory_SlugAndDeletedAtIsNull(categorySlug.trim(), sort);

        return products.stream().map(p -> {
            Category category = p.getCategory();
            return new ProductSummaryResponse(
                    p.getId(),
                    p.getName(),
                    coverImage(p),
                    (int) skuRepository.countByProduct_IdAndDeletedAtIsNull(p.getId()),
                    new CategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured())
                );
        }).toList();
    }

    @Transactional(readOnly = true)
    public ProductAdminResponse findDetail(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Product product = requireActiveProduct(id);
        Category category = product.getCategory();

        List<Sku> skus = skuRepository.findByProduct_IdAndDeletedAtIsNull(id, Sort.by(Sort.Direction.ASC, "title"));

        List<ProductVariantResponse> variants = skus.stream().map(s -> new ProductVariantResponse(
                s.getId(),
                s.getTitle(),
                s.getDescription(),
                s.getPrice(),
                s.getOriginalPrice(),
                s.getStock(),
                safePhotos(s),
                s.getAttributes()
        )).collect(Collectors.toList());

        return new ProductAdminResponse(
                product.getId(),
                product.getName(),
                new CategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured()),
                optionsFromSchema(product.getSchema()),
                variants
        );
    }

    @Transactional
    public ProductAdminResponse createAtomic(ProductAdminRequest request, List<MultipartFile> variantImages, HttpSession session) {
        return createAtomic(request, variantImages, Map.of(), session);
    }

    @Transactional
    public ProductAdminResponse createAtomic(ProductAdminRequest request,
                                             List<MultipartFile> variantImages,
                                             Map<Integer, List<MultipartFile>> variantImagesByVariant,
                                             HttpSession session) {
        authService.requireAdmin(session);
        validateRequest(request);
        List<ProductVariantRequest> variants = requireVariants(request.variants());

        Category category = requireCategory(request.categoryId());
        Product product = new Product();
        product.setName(normalizeRequiredText(request.name(), 2, 160, "Nome deve ter entre 2 e 160 caracteres"));
        product.setCategory(category);
        product.setSchema(validateSchema(request.options()));

        Product savedProduct = productRepository.save(product);

        for (int i = 0; i < variants.size(); i++) {
            ProductVariantRequest vReq = variants.get(i);
            Sku sku = new Sku();
            sku.setProduct(savedProduct);
            sku.setTitle(normalizeVariantTitle(vReq.title(), savedProduct.getName()));
            sku.setDescription(normalizeVariantDescription(vReq.description()));
            sku.setPrice(validatePrice(vReq.price()));
            sku.setOriginalPrice(validateOriginalPrice(vReq.price(), vReq.originalPrice()));
            sku.setStock(validateStock(vReq.stock()));
            sku.setAttributes(validateAttributes(savedProduct, vReq.attributes()));
            sku.setPhotos(saveVariantPhotos(vReq, i, variantImages, variantImagesByVariant));
            skuRepository.save(sku);
        }

        return findDetail(savedProduct.getId(), session);
    }

    @Transactional
    public ProductAdminResponse updateAtomic(UUID id, ProductAdminRequest request, List<MultipartFile> variantImages, HttpSession session) {
        return updateAtomic(id, request, variantImages, Map.of(), session);
    }

    @Transactional
    public ProductAdminResponse updateAtomic(UUID id,
                                             ProductAdminRequest request,
                                             List<MultipartFile> variantImages,
                                             Map<Integer, List<MultipartFile>> variantImagesByVariant,
                                             HttpSession session) {
        authService.requireAdmin(session);
        validateRequest(request);
        Product product = requireActiveProduct(id);

        product.setName(normalizeRequiredText(request.name(), 2, 160, "Nome deve ter entre 2 e 160 caracteres"));
        if (request.categoryId() != null) {
            product.setCategory(requireCategory(request.categoryId()));
        }
        product.setSchema(validateSchema(request.options()));

        productRepository.save(product);

        // SKU Synchronization
        List<Sku> existingSkus = skuRepository.findByProduct_IdAndDeletedAtIsNull(id, Sort.by(Sort.Direction.ASC, "title"));
        Set<UUID> processedIds = new HashSet<>();
        List<ProductVariantRequest> variants = requireVariants(request.variants());

        for (int i = 0; i < variants.size(); i++) {
            ProductVariantRequest vReq = variants.get(i);
            Sku sku;
            if (vReq.id() != null) {
                sku = skuRepository.findById(vReq.id())
                        .filter(s -> s.getProduct().getId().equals(id) && s.getDeletedAt() == null)
                        .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
            } else {
                sku = new Sku();
                sku.setProduct(product);
            }

            sku.setTitle(normalizeVariantTitle(vReq.title(), product.getName()));
            sku.setDescription(normalizeVariantDescription(vReq.description()));
            sku.setPrice(validatePrice(vReq.price()));
            sku.setOriginalPrice(validateOriginalPrice(vReq.price(), vReq.originalPrice()));
            sku.setStock(validateStock(vReq.stock()));
            sku.setAttributes(validateAttributes(product, vReq.attributes()));

            // SKU Photos Synchronization
            List<String> newPhotos = saveVariantPhotos(vReq, i, variantImages, variantImagesByVariant);
            
            if (!newPhotos.isEmpty()) {
                if (sku.getPhotos() != null) {
                    sku.getPhotos().forEach(uploadService::deleteByPublicPath);
                }
                sku.setPhotos(newPhotos);
            }

            skuRepository.save(sku);
            processedIds.add(sku.getId());
        }

        // Soft delete removed SKUs
        for (Sku existing : existingSkus) {
            if (!processedIds.contains(existing.getId())) {
                existing.setDeletedAt(LocalDateTime.now());
                if (existing.getPhotos() != null) {
                    existing.getPhotos().forEach(uploadService::deleteByPublicPath);
                }
                skuRepository.save(existing);
            }
        }

        return findDetail(product.getId(), session);
    }

    @Transactional
    public ProductAdminResponse updateMetadata(UUID id, ProductUpdateRequest request, HttpSession session) {
        authService.requireAdmin(session);
        validateProductUpdateRequest(request);

        Product product = requireActiveProduct(id);
        boolean nameChanged = false;
        String updatedName = null;

        if (request.name() != null) {
            updatedName = normalizeRequiredText(request.name(), 2, 160, "Nome deve ter entre 2 e 160 caracteres");
            nameChanged = !Objects.equals(product.getName(), updatedName);
            product.setName(updatedName);
        }
        if (request.categoryId() != null) {
            product.setCategory(requireCategory(request.categoryId()));
        }

        productRepository.save(product);

        if (nameChanged) {
            syncSingleVariantTitleIfOptionless(product, updatedName);
        }

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse createVariant(UUID productId,
                                              ProductVariantUpsertRequest request,
                                              List<MultipartFile> images,
                                              HttpSession session) {
        authService.requireAdmin(session);
        validateVariantRequest(request);
        Product product = requireActiveProduct(productId);

        if (extractSelectorKeys(product).isEmpty()
                && !skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId()).isEmpty()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Produto sem variações deve possuir apenas uma variante", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Sku sku = new Sku();
        sku.setProduct(product);
        applyVariantData(product, sku, request);
        sku.setPhotos(savePhotos(images));
        skuRepository.save(sku);

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse updateVariant(UUID productId,
                                              UUID skuId,
                                              ProductVariantUpsertRequest request,
                                              HttpSession session) {
        authService.requireAdmin(session);
        validateVariantRequest(request);
        Product product = requireActiveProduct(productId);
        Sku sku = requireActiveSku(productId, skuId);

        applyVariantData(product, sku, request);
        skuRepository.save(sku);

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse addVariantPhoto(UUID productId, UUID skuId, MultipartFile image, HttpSession session) {
        authService.requireAdmin(session);
        Product product = requireActiveProduct(productId);
        Sku sku = requireActiveSku(productId, skuId);

        List<String> photos = new ArrayList<>(safePhotos(sku));
        photos.add(uploadService.saveImage(image, "products"));
        sku.setPhotos(photos);
        skuRepository.save(sku);

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse deleteVariantPhoto(UUID productId, UUID skuId, String photo, HttpSession session) {
        authService.requireAdmin(session);
        Product product = requireActiveProduct(productId);
        Sku sku = requireActiveSku(productId, skuId);

        String normalizedPhoto = normalizePhotoPath(photo);
        List<String> currentPhotos = safePhotos(sku);
        if (!currentPhotos.contains(normalizedPhoto)) {
            throw new AppException("RESOURCE_NOT_FOUND", "Foto não encontrada para esta variante", HttpStatus.NOT_FOUND);
        }

        List<String> updatedPhotos = new ArrayList<>(currentPhotos);
        updatedPhotos.remove(normalizedPhoto);
        sku.setPhotos(updatedPhotos);
        skuRepository.save(sku);
        uploadService.deleteByPublicPath(normalizedPhoto);

        return toAdminResponse(product);
    }

    @Transactional
    public ProductAdminResponse reorderVariantPhotos(UUID productId,
                                                     UUID skuId,
                                                     ProductVariantPhotoOrderRequest request,
                                                     HttpSession session) {
        authService.requireAdmin(session);
        Product product = requireActiveProduct(productId);
        Sku sku = requireActiveSku(productId, skuId);
        validatePhotoOrder(request, safePhotos(sku));

        sku.setPhotos(new ArrayList<>(request.photos()));
        skuRepository.save(sku);

        return toAdminResponse(product);
    }

    @Transactional
    public void deleteVariant(UUID productId, UUID skuId, HttpSession session) {
        authService.requireAdmin(session);
        requireActiveProduct(productId);
        List<Sku> activeSkus = skuRepository.findByProduct_IdAndDeletedAtIsNull(productId);
        Sku target = activeSkus.stream()
                .filter(sku -> sku.getId().equals(skuId))
                .findFirst()
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));

        if (activeSkus.size() <= 1) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Produto deve possuir ao menos uma variante", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        target.setDeletedAt(LocalDateTime.now());
        safePhotos(target).forEach(uploadService::deleteByPublicPath);
        skuRepository.save(target);
    }

    @Transactional
    public void delete(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        Product product = requireActiveProduct(id);
        LocalDateTime deletedAt = LocalDateTime.now();
        product.setDeletedAt(deletedAt);
        
        List<Sku> activeSkus = skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId());
        for (Sku sku : activeSkus) {
            sku.setDeletedAt(deletedAt);
            if (sku.getPhotos() != null) {
                sku.getPhotos().forEach(uploadService::deleteByPublicPath);
            }
        }
        productRepository.save(product);
    }

    private Product requireActiveProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Produto não encontrado", HttpStatus.NOT_FOUND));
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Categoria não encontrada", HttpStatus.NOT_FOUND));
    }

    private Sku requireActiveSku(UUID productId, UUID skuId) {
        return skuRepository.findByIdAndDeletedAtIsNull(skuId)
                .filter(sku -> sku.getProduct().getId().equals(productId))
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "SKU não encontrado", HttpStatus.NOT_FOUND));
    }

    private Map<String, Object> validateSchema(List<ProductAdminRequest.OptionRequest> options) {
        Map<String, Object> schema = new LinkedHashMap<>();
        List<Map<String, String>> selectors = new java.util.ArrayList<>();
        Set<String> keys = new HashSet<>();

        if (options == null || options.isEmpty()) {
            schema.put("selectors", selectors);
            return schema;
        }

        for (ProductAdminRequest.OptionRequest opt : options) {
            if (opt.key() == null || opt.key().isBlank()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Cada selector deve possuir key", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (opt.label() == null || opt.label().isBlank()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Cada selector deve possuir label", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!opt.key().matches("^[a-z][a-zA-Z0-9]*$")) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Key do selector deve usar camelCase", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!keys.add(opt.key())) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Schema possui selector repetido", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            selectors.add(Map.of("key", opt.key(), "label", opt.label()));
        }
        schema.put("selectors", selectors);
        return schema;
    }

    private Map<String, Object> validateAttributes(Product product, Map<String, Object> attributes) {
        Set<String> selectorKeys = extractSelectorKeys(product);
        if (selectorKeys.isEmpty()) {
            if (attributes != null && !attributes.isEmpty()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Atributos do SKU não são compatíveis com o schema do produto", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return new LinkedHashMap<>();
        }

        if (attributes == null) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Atributos do SKU são obrigatórios", HttpStatus.UNPROCESSABLE_ENTITY);
        }

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

    private List<ProductAdminRequest.OptionRequest> optionsFromSchema(Map<String, Object> schema) {
        Object selectorsObj = schema == null ? null : schema.get("selectors");
        if (!(selectorsObj instanceof List<?> selectors)) {
            return List.of();
        }

        List<ProductAdminRequest.OptionRequest> options = new java.util.ArrayList<>();
        for (Object selectorObj : selectors) {
            if (selectorObj instanceof Map<?, ?> selector
                    && selector.get("key") instanceof String key
                    && selector.get("label") instanceof String label) {
                options.add(new ProductAdminRequest.OptionRequest(key, label));
            }
        }
        return options;
    }

    private void validateRequest(ProductAdminRequest request) {
        if (request == null) {
            throw new AppException("VALIDATION_ERROR", "Dados do produto são obrigatórios", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateProductUpdateRequest(ProductUpdateRequest request) {
        if (request == null || request.name() == null && request.categoryId() == null) {
            throw new AppException("VALIDATION_ERROR", "Informe ao menos um campo para atualizar", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateVariantRequest(ProductVariantUpsertRequest request) {
        if (request == null) {
            throw new AppException("VALIDATION_ERROR", "Dados da variante são obrigatórios", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizePhotoPath(String photo) {
        if (photo == null || photo.isBlank()) {
            throw new AppException("VALIDATION_ERROR", "Foto é obrigatória", HttpStatus.BAD_REQUEST);
        }
        return photo.trim();
    }

    private void validatePhotoOrder(ProductVariantPhotoOrderRequest request, List<String> currentPhotos) {
        if (request == null || request.photos() == null) {
            throw new AppException("VALIDATION_ERROR", "Lista de fotos é obrigatória", HttpStatus.BAD_REQUEST);
        }

        List<String> requestedPhotos = request.photos().stream()
                .map(this::normalizePhotoPath)
                .toList();
        Set<String> requestedSet = new LinkedHashSet<>(requestedPhotos);
        Set<String> currentSet = new LinkedHashSet<>(currentPhotos);

        if (requestedSet.size() != requestedPhotos.size() || requestedPhotos.size() != currentPhotos.size() || !requestedSet.equals(currentSet)) {
            throw new AppException("VALIDATION_ERROR", "Lista de fotos deve conter exatamente as fotos atuais da variante", HttpStatus.BAD_REQUEST);
        }
    }

    private List<ProductVariantRequest> requireVariants(List<ProductVariantRequest> variants) {
        if (variants == null || variants.isEmpty()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "O produto deve possuir ao menos uma variante", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (variants.stream().anyMatch(Objects::isNull)) {
            throw new AppException("VALIDATION_ERROR", "Variante inválida", HttpStatus.BAD_REQUEST);
        }
        return variants;
    }

    private String normalizeRequiredText(String value, int minLength, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        String normalized = value.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int maxLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new AppException("BUSINESS_RULE_VIOLATION", message, HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return normalized;
    }

    private String normalizeVariantTitle(String title, String productName) {
        if (title == null || title.isBlank()) {
            return productName;
        }
        return normalizeRequiredText(title, 2, 160, "Título da variante deve ter entre 2 e 160 caracteres");
    }

    private String normalizeVariantDescription(String variantDescription) {
        return normalizeRequiredText(variantDescription, 2, 2000, "Descrição da variante deve ter entre 2 e 2000 caracteres");
    }

    private BigDecimal validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Preço deve ser maior que zero", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return price;
    }

    private BigDecimal validateOriginalPrice(BigDecimal price, BigDecimal originalPrice) {
        if (originalPrice != null && originalPrice.compareTo(price) <= 0) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Preço original deve ser maior que o preço atual", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return originalPrice;
    }

    private Integer validateStock(Integer stock) {
        if (stock == null || stock < 0) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Estoque deve ser maior ou igual a zero", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return stock;
    }

    private void applyVariantData(Product product, Sku sku, ProductVariantUpsertRequest request) {
        sku.setTitle(normalizeVariantTitle(request.title(), product.getName()));
        sku.setDescription(normalizeVariantDescription(request.description()));
        sku.setPrice(validatePrice(request.price()));
        sku.setOriginalPrice(validateOriginalPrice(request.price(), request.originalPrice()));
        sku.setStock(validateStock(request.stock()));
        sku.setAttributes(validateAttributes(product, request.attributes()));
    }

    private List<String> savePhotos(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<String> photos = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                photos.add(uploadService.saveImage(file, "products"));
            }
        }
        return photos;
    }

    private List<String> saveVariantPhotos(ProductVariantRequest variant,
                                           int variantIndex,
                                           List<MultipartFile> variantImages,
                                           Map<Integer, List<MultipartFile>> variantImagesByVariant) {
        List<MultipartFile> files = new java.util.ArrayList<>();
        List<MultipartFile> indexedFiles = variantImagesByVariant == null ? null : variantImagesByVariant.get(variantIndex);
        if (indexedFiles != null && !indexedFiles.isEmpty()) {
            files.addAll(indexedFiles);
        } else if (variant.imageIndices() != null && !variant.imageIndices().isEmpty()) {
            if (variantImages == null) {
                throw new AppException("VALIDATION_ERROR", "Índice de imagem da variante inválido", HttpStatus.BAD_REQUEST);
            }
            for (Integer index : variant.imageIndices()) {
                if (index == null || index < 0 || index >= variantImages.size()) {
                    throw new AppException("VALIDATION_ERROR", "Índice de imagem da variante inválido", HttpStatus.BAD_REQUEST);
                }
                files.add(variantImages.get(index));
            }
        } else if (variantImages != null && variantIndex < variantImages.size()) {
            files.add(variantImages.get(variantIndex));
        }

        return savePhotos(files);
    }

    private String coverImage(Product product) {
        return skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId(), Sort.by(Sort.Direction.ASC, "title"))
                .stream()
                .map(Sku::getPhotos)
                .filter(photos -> photos != null && !photos.isEmpty())
                .map(List::getFirst)
                .findFirst()
                .orElse(null);
    }

    private List<String> safePhotos(Sku sku) {
        List<String> photos = sku.getPhotos();
        return photos == null ? List.of() : photos;
    }

    private void syncSingleVariantTitleIfOptionless(Product product, String title) {
        if (!extractSelectorKeys(product).isEmpty()) {
            return;
        }

        List<Sku> activeSkus = skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId());
        if (activeSkus.size() == 1) {
            Sku sku = activeSkus.getFirst();
            sku.setTitle(title);
            skuRepository.save(sku);
        }
    }

    private ProductAdminResponse toAdminResponse(Product product) {
        Category category = product.getCategory();
        List<Sku> skus = skuRepository.findByProduct_IdAndDeletedAtIsNull(product.getId(), Sort.by(Sort.Direction.ASC, "title"));
        
        List<ProductVariantResponse> variants = skus.stream().map(s -> new ProductVariantResponse(
                s.getId(),
                s.getTitle(),
                s.getDescription(),
                s.getPrice(),
                s.getOriginalPrice(),
                s.getStock(),
                safePhotos(s),
                s.getAttributes()
        )).collect(Collectors.toList());

        return new ProductAdminResponse(
                product.getId(),
                product.getName(),
                new CategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured()),
                optionsFromSchema(product.getSchema()),
                variants
        );
    }
}
