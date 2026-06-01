package br.ufc.smd.ecommercecopa.controller;

import br.ufc.smd.ecommercecopa.dto.ApiResponse;
import br.ufc.smd.ecommercecopa.dto.client.ClientMeResponse;
import br.ufc.smd.ecommercecopa.dto.client.UpdateClientMeRequest;
import br.ufc.smd.ecommercecopa.service.ClientService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ClientMeResponse>> me(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.me(session)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ClientMeResponse>> updateMe(@Valid @RequestBody UpdateClientMeRequest request,
                                                                  HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.updateMe(request, session)));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(HttpSession session) {
        clientService.deleteMe(session);
        return ResponseEntity.noContent().build();
    }
}
