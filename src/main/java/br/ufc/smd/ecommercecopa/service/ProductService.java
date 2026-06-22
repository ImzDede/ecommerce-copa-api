package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.category.CategoryResponse;
import br.ufc.smd.ecommercecopa.dto.product.CreateProductRequest;
import br.ufc.smd.ecommercecopa.dto.product.ProductListResponse;
import br.ufc.smd.ecommercecopa.dto.product.ProductResponse;
import br.ufc.smd.ecommercecopa.dto.product.UpdateProductRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Category;
import br.ufc.smd.ecommercecopa.model.Product;
import br.ufc.smd.ecommercecopa.model.Sku;
import br.ufc.smd.ecommercecopa.repository.CategoryRepository;
import br.ufc.smd.ecommercecopa.repository.ProductRepository;
import br.ufc.smd.ecommercecopa.repository.SkuRepository;
import jakarta.servlet.http.HttpSession;
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

@Service
public class ProductService {

    private final AuthService authService;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SkuRepository skuRepository;

    public ProductService(AuthService authService,
                          ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          SkuRepository skuRepository) {
        this.authService = authService;
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public ProductListResponse list(String categorySlug, HttpSession session) {
        authService.requireAdmin(session);

        Sort sort = Sort.by(Sort.Direction.ASC, "createdAt");
        List<Product> products = categorySlug == null || categorySlug.isBlank()
                ? productRepository.findByDeletedAtIsNull(sort)
                : productRepository.findByCategory_SlugAndDeletedAtIsNull(categorySlug.trim(), sort);

        return new ProductListResponse(products.stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(UUID id, HttpSession session) {
        authService.requireAdmin(session);
        return toResponse(requireActiveProduct(id));
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request, HttpSession session) {
        authService.requireAdmin(session);

        Category category = requireCategory(request.categoryId());
        Product product = new Product();
        product.setCategory(category);
        product.setSchema(validateSchema(request.schema()));

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request, HttpSession session) {
        authService.requireAdmin(session);

        boolean hasCategory = request.categoryId() != null;
        boolean hasSchema = request.schema() != null;
        if (!hasCategory && !hasSchema) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Product product = requireActiveProduct(id);
        if (hasCategory) {
            product.setCategory(requireCategory(request.categoryId()));
        }
        if (hasSchema) {
            product.setSchema(validateSchema(request.schema()));
        }

        return toResponse(productRepository.save(product));
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
        }
    }

    private Product requireActiveProduct(UUID id) {
        return productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Produto não encontrado", HttpStatus.NOT_FOUND));
    }

    private Category requireCategory(UUID id) {
        return categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Categoria não encontrada", HttpStatus.NOT_FOUND));
    }

    private ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                new CategoryResponse(category.getId(), category.getSlug(), category.getTitle(), category.getImage(), category.isFeatured()),
                product.getSchema(),
                skuRepository.countByProduct_IdAndDeletedAtIsNull(product.getId())
        );
    }

    private Map<String, Object> validateSchema(Map<String, Object> schema) {
        Object selectorsObj = schema.get("selectors");
        if (!(selectorsObj instanceof List<?> selectors)) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Schema deve possuir selectors como lista", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> keys = new HashSet<>();
        for (Object selectorObj : selectors) {
            if (!(selectorObj instanceof Map<?, ?> selector)) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Cada selector do schema deve ser um objeto", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            Object keyObj = selector.get("key");
            Object labelObj = selector.get("label");
            if (!(keyObj instanceof String key) || key.isBlank()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Cada selector deve possuir key", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!(labelObj instanceof String label) || label.isBlank()) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Cada selector deve possuir label", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!key.matches("^[a-z][a-zA-Z0-9]*$")) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Key do selector deve usar camelCase", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (!keys.add(key)) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Schema possui selector repetido", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        return new LinkedHashMap<>(schema);
    }
}
