package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.address.AddressListResponse;
import br.ufc.smd.ecommercecopa.dto.address.AddressResponse;
import br.ufc.smd.ecommercecopa.dto.address.CreateAddressRequest;
import br.ufc.smd.ecommercecopa.dto.address.UpdateAddressRequest;
import br.ufc.smd.ecommercecopa.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@Tag(name = "Endereços", description = "Rotas privadas de endereços do cliente autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping
    @Operation(summary = "Listar endereços")
    public ResponseEntity<ApiResponse<AddressListResponse>> list(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(addressService.list(session)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar endereço")
    public ResponseEntity<ApiResponse<AddressResponse>> findById(@PathVariable UUID id, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(addressService.findById(id, session)));
    }

    @PostMapping
    @Operation(summary = "Criar endereço")
    public ResponseEntity<ApiResponse<AddressResponse>> create(@Valid @RequestBody CreateAddressRequest request,
                                                               HttpSession session) {
        return ResponseEntity.status(201).body(new ApiResponse<>(addressService.create(request, session)));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<ApiResponse<AddressResponse>> update(@PathVariable UUID id,
                                                               @Valid @RequestBody UpdateAddressRequest request,
                                                               HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(addressService.update(id, request, session)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir endereço", description = "Faz soft delete preenchendo deletedAt.")
    public ResponseEntity<Void> delete(@PathVariable UUID id, HttpSession session) {
        addressService.delete(id, session);
        return ResponseEntity.noContent().build();
    }
}
