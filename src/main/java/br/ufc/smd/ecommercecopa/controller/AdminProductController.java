package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.product.*;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping("/api/admin/products")
@Tag(name = "Admin - Products", description = "CRUD administrativo de products, que agrupam SKUs e definem schema de variação.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AdminProductController {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public AdminProductController(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    @Operation(summary = "Listar products", description = "Retorna lista resumida de produtos.")
    public ResponseEntity<ApiResponse<ProductListResponse>> list(@Parameter(description = "Slug da categoria, não UUID. Exemplo: chuteiras-de-campo", example = "chuteiras-de-campo")
                                                                  @RequestParam(required = false) String category,
                                                                  HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(new ProductListResponse(productService.listSummary(category, session))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar product por ID")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.findDetail(id, session)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Criar product",
            description = "Cria Product e uma ou mais variantes/SKUs. Descrição, preço e imagem pertencem à variante. No Swagger, use variantImage0 para a imagem de variants[0], variantImage1 para variants[1] etc. Produto sem variação significa options vazio e uma única variante com attributes vazio.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProductMultipartRequest.class),
                            encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE),
                            examples = {
                                    @ExampleObject(name = "Produto com variantes",
                                            summary = "Duas variantes; use variantImage0 e variantImage1 para as imagens",
                                            value = "{\"data\":{\"name\":\"Camisa Brasil 2026\",\"categoryId\":\"00000000-0000-0000-0000-000000000000\",\"options\":[{\"key\":\"size\",\"label\":\"Tamanho\"},{\"key\":\"color\",\"label\":\"Cor\"}],\"variants\":[{\"title\":\"Camisa Brasil P Amarela\",\"description\":\"Modelo P amarelo.\",\"price\":249.90,\"originalPrice\":299.90,\"stock\":10,\"attributes\":{\"size\":\"P\",\"color\":\"Amarela\"}},{\"title\":\"Camisa Brasil M Azul\",\"description\":\"Modelo M azul.\",\"price\":249.90,\"originalPrice\":299.90,\"stock\":5,\"attributes\":{\"size\":\"M\",\"color\":\"Azul\"}}]}}"),
                                    @ExampleObject(name = "Produto sem variacao",
                                            summary = "Uma variante unica com attributes vazio",
                                            value = "{\"data\":{\"name\":\"Álbum Copa 2026\",\"categoryId\":\"00000000-0000-0000-0000-000000000000\",\"options\":[],\"variants\":[{\"title\":\"Álbum Copa 2026\",\"description\":\"Álbum oficial da Copa.\",\"price\":39.90,\"originalPrice\":49.90,\"stock\":100,\"attributes\":{}}]}}")
                            })))
    public ResponseEntity<ApiResponse<ProductAdminResponse>> create(
            @Parameter(hidden = true)
            @RequestPart("data") String dataJson,
            @Parameter(hidden = true)
            MultipartHttpServletRequest multipartRequest,
            HttpSession session) {
        ProductAdminRequest request = parseData(dataJson);
        return ResponseEntity.status(201).body(new ApiResponse<>(productService.createAtomic(request,
                collectFiles(multipartRequest, "variantImages"),
                collectVariantImagesByVariant(multipartRequest),
                session)));
    }

    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar dados básicos do product",
            description = "Atualiza somente metadados do Product. Não cria, remove ou sincroniza variantes/SKUs.")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> update(
            @PathVariable UUID id,
            @RequestBody ProductUpdateRequest request,
            HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.updateMetadata(id, request, session)));
    }

    @PostMapping(value = "/{productId}/variants", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Criar variante do product",
            description = "Cria uma única variante/SKU para um Product existente. Envie imagens opcionais nos campos images, images[], images[0], image0, image1 etc.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProductVariantMultipartRequest.class),
                            encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE))))
    public ResponseEntity<ApiResponse<ProductAdminResponse>> createVariant(
            @PathVariable UUID productId,
            @Parameter(hidden = true)
            @RequestPart("data") String dataJson,
            @Parameter(hidden = true)
            MultipartHttpServletRequest multipartRequest,
            HttpSession session) {
        ProductVariantUpsertRequest request = parseVariantData(dataJson);
        return ResponseEntity.status(201).body(new ApiResponse<>(productService.createVariant(productId, request, collectVariantFiles(multipartRequest), session)));
    }

    @PatchMapping(value = "/{productId}/variants/{skuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atualizar variante do product",
            description = "Atualiza dados de uma única variante/SKU. Fotos são gerenciadas pelas rotas específicas de fotos.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductVariantUpsertRequest.class))))
    public ResponseEntity<ApiResponse<ProductAdminResponse>> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID skuId,
            @RequestBody ProductVariantUpsertRequest request,
            HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.updateVariant(productId, skuId, request, session)));
    }

    @PostMapping(value = "/{productId}/variants/{skuId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Adicionar foto à variante", description = "Adiciona uma foto ao final da galeria da variante/SKU.")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> addVariantPhoto(
            @PathVariable UUID productId,
            @PathVariable UUID skuId,
            @RequestPart("image") MultipartFile image,
            HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(productService.addVariantPhoto(productId, skuId, image, session)));
    }

    @DeleteMapping("/{productId}/variants/{skuId}/photos")
    @Operation(summary = "Excluir foto da variante", description = "Remove uma foto específica da galeria da variante/SKU. Envie o caminho público da foto no query param photo.")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> deleteVariantPhoto(
            @PathVariable UUID productId,
            @PathVariable UUID skuId,
            @RequestParam String photo,
            HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.deleteVariantPhoto(productId, skuId, photo, session)));
    }

    @PatchMapping(value = "/{productId}/variants/{skuId}/photos/reorder", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reordenar fotos da variante", description = "Recebe exatamente as fotos atuais da variante na nova ordem desejada.")
    public ResponseEntity<ApiResponse<ProductAdminResponse>> reorderVariantPhotos(
            @PathVariable UUID productId,
            @PathVariable UUID skuId,
            @RequestBody ProductVariantPhotoOrderRequest request,
            HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(productService.reorderVariantPhotos(productId, skuId, request, session)));
    }

    @DeleteMapping("/{productId}/variants/{skuId}")
    @Operation(summary = "Excluir variante do product", description = "Faz soft delete de uma variante/SKU, mantendo as demais variantes ativas.")
    public ResponseEntity<Void> deleteVariant(@PathVariable UUID productId, @PathVariable UUID skuId, HttpSession session) {
        productService.deleteVariant(productId, skuId, session);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir product", description = "Faz soft delete do product e de todos os SKUs ativos vinculados.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        productService.delete(id, session);
        return ResponseEntity.noContent().build();
    }

    private ProductAdminRequest parseData(String dataJson) {
        try {
            return objectMapper.readValue(dataJson, ProductAdminRequest.class);
        } catch (JsonProcessingException exception) {
            throw new AppException("VALIDATION_ERROR", "Campo data deve ser um JSON válido", HttpStatus.BAD_REQUEST);
        }
    }

    private ProductVariantUpsertRequest parseVariantData(String dataJson) {
        try {
            return objectMapper.readValue(dataJson, ProductVariantUpsertRequest.class);
        } catch (JsonProcessingException exception) {
            throw new AppException("VALIDATION_ERROR", "Campo data deve ser um JSON válido", HttpStatus.BAD_REQUEST);
        }
    }

    private List<MultipartFile> collectVariantFiles(MultipartHttpServletRequest request) {
        List<MultipartFile> files = new ArrayList<>();
        addNonEmpty(files, request.getFiles("images"));
        addNonEmpty(files, request.getFiles("images[]"));
        addNonEmpty(files, request.getFiles("image"));
        addNonEmpty(files, request.getFiles("image0"));
        addNonEmpty(files, request.getFiles("image1"));
        addNonEmpty(files, request.getFiles("image2"));

        collectIndexedFiles(request, "images").values()
                .forEach(indexedFiles -> addNonEmpty(files, indexedFiles));

        return files;
    }

    private List<MultipartFile> collectFiles(MultipartHttpServletRequest request, String baseName) {
        List<MultipartFile> files = new ArrayList<>();
        addNonEmpty(files, request.getFiles(baseName));
        addNonEmpty(files, request.getFiles(baseName + "[]"));

        collectIndexedFiles(request, baseName).values()
                .forEach(indexedFiles -> addNonEmpty(files, indexedFiles));

        return files;
    }

    private Map<Integer, List<MultipartFile>> collectVariantImagesByVariant(MultipartHttpServletRequest request) {
        Map<Integer, List<MultipartFile>> indexed = collectIndexedFiles(request, "variantImages");
        addIndexedAlias(indexed, 0, request.getFiles("variantImage0"));
        addIndexedAlias(indexed, 1, request.getFiles("variantImage1"));
        addIndexedAlias(indexed, 2, request.getFiles("variantImage2"));
        return indexed;
    }

    private Map<Integer, List<MultipartFile>> collectIndexedFiles(MultipartHttpServletRequest request, String baseName) {
        Map<Integer, List<MultipartFile>> indexed = new TreeMap<>();
        request.getMultiFileMap().forEach((name, files) -> {
            Integer index = parseIndex(name, baseName);
            if (index != null) {
                List<MultipartFile> nonEmpty = files.stream()
                        .filter(file -> file != null && !file.isEmpty())
                        .toList();
                if (!nonEmpty.isEmpty()) {
                    indexed.computeIfAbsent(index, ignored -> new ArrayList<>()).addAll(nonEmpty);
                }
            }
        });
        return indexed;
    }

    private void addNonEmpty(List<MultipartFile> target, List<MultipartFile> files) {
        files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .forEach(target::add);
    }

    private void addIndexedAlias(Map<Integer, List<MultipartFile>> target, int index, List<MultipartFile> files) {
        List<MultipartFile> nonEmpty = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
        if (!nonEmpty.isEmpty()) {
            target.computeIfAbsent(index, ignored -> new ArrayList<>()).addAll(nonEmpty);
        }
    }

    private Integer parseIndex(String name, String baseName) {
        String prefix = baseName + "[";
        if (!name.startsWith(prefix) || !name.endsWith("]")) {
            return null;
        }

        String indexText = name.substring(prefix.length(), name.length() - 1);
        try {
            return Integer.parseInt(indexText);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
