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

    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualizar product",
            description = "Atualiza Product e sincroniza variantes/SKUs. Variantes ausentes no payload são removidas por soft delete. No Swagger, use variantImage0 para a imagem de variants[0], variantImage1 para variants[1] etc.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(implementation = ProductMultipartRequest.class),
                            encoding = @Encoding(name = "data", contentType = MediaType.APPLICATION_JSON_VALUE))))
    public ResponseEntity<ApiResponse<ProductAdminResponse>> update(
            @PathVariable UUID id,
            @Parameter(hidden = true)
            @RequestPart("data") String dataJson,
            @Parameter(hidden = true)
            MultipartHttpServletRequest multipartRequest,
            HttpSession session) {
        ProductAdminRequest request = parseData(dataJson);
        return ResponseEntity.ok(new ApiResponse<>(productService.updateAtomic(id, request,
                collectFiles(multipartRequest, "variantImages"),
                collectVariantImagesByVariant(multipartRequest),
                session)));
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
