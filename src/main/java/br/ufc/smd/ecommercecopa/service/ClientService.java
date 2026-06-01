package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.client.ClientMeResponse;
import br.ufc.smd.ecommercecopa.dto.client.UpdateClientMeRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.model.UserRole;
import br.ufc.smd.ecommercecopa.repository.ClientRepository;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final AuthService authService;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(AuthService authService,
                         ClientRepository clientRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ClientMeResponse me(HttpSession session) {
        User user = authService.requireSessionUser(session);
        ensureClient(user);

        Client client = clientRepository.findById(user.getId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Cliente não encontrado", HttpStatus.NOT_FOUND));

        return toResponse(client);
    }

    @Transactional
    public ClientMeResponse updateMe(UpdateClientMeRequest request, HttpSession session) {
        User user = authService.requireSessionUser(session);
        ensureClient(user);

        boolean hasName = request.name() != null && !request.name().isBlank();
        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPassword = request.password() != null && !request.password().isBlank();

        if (!hasName && !hasEmail && !hasPassword) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Client client = clientRepository.findById(user.getId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Cliente não encontrado", HttpStatus.NOT_FOUND));

        if (hasName) {
            user.setName(request.name().trim());
        }

        if (hasEmail) {
            String normalizedEmail = normalizeEmail(request.email());
            if (userRepository.existsByEmailAndIdNot(normalizedEmail, user.getId())) {
                throw new AppException("DUPLICATE_RESOURCE", "Email já cadastrado", HttpStatus.CONFLICT);
            }
            user.setEmail(normalizedEmail);
        }

        if (hasPassword) {
            if (!isStrongPassword(request.password())) {
                throw new AppException("BUSINESS_RULE_VIOLATION", "Senha deve ter maiúscula, minúscula e caractere especial", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        userRepository.save(user);
        return toResponse(client);
    }

    @Transactional
    public void deleteMe(HttpSession session) {
        User user = authService.requireSessionUser(session);
        ensureClient(user);

        clientRepository.deleteById(user.getId());
        userRepository.deleteById(user.getId());
        session.invalidate();
    }

    private void ensureClient(User user) {
        if (user.getRole() != UserRole.CLIENT) {
            throw new AppException("FORBIDDEN", "Apenas cliente pode acessar este recurso", HttpStatus.FORBIDDEN);
        }
    }

    private ClientMeResponse toResponse(Client client) {
        User user = client.getUser();
        return new ClientMeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                client.getCpf(),
                client.getDateOfBirth().toString()
        );
    }

    private boolean isStrongPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).+$");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
