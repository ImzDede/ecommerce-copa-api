package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.config.OpenApiConfig;
import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.user.UserPhotoResponse;
import br.ufc.smd.ecommercecopa.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Usuários", description = "Rotas privadas do usuário autenticado.")
@SecurityRequirement(name = OpenApiConfig.SESSION_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Atualizar foto de perfil", description = "Recebe a imagem no campo multipart photo.")
    public ResponseEntity<ApiResponse<UserPhotoResponse>> updateMyPhoto(@RequestPart("photo") MultipartFile photo,
                                                                        HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(userService.updateMyPhoto(photo, session)));
    }

    @DeleteMapping("/me/photo")
    @Operation(summary = "Remover foto de perfil", description = "Remove a foto de perfil do usuário autenticado.")
    public ResponseEntity<ApiResponse<UserPhotoResponse>> deleteMyPhoto(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(userService.deleteMyPhoto(session)));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Excluir minha conta", description = "Remove o usuário autenticado e sua linha de cliente ou admin, invalidando a sessão.")
    public ResponseEntity<Void> deleteMe(HttpSession session) {
        userService.deleteMe(session);
        return ResponseEntity.noContent().build();
    }
}
