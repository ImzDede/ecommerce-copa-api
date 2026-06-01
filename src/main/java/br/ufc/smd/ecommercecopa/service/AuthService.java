package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.auth.AuthUserResponse;
import br.ufc.smd.ecommercecopa.dto.auth.LoginRequest;
import br.ufc.smd.ecommercecopa.dto.auth.RegisterClientRequest;
import br.ufc.smd.ecommercecopa.dto.auth.RegisterClientResponse;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Client;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.model.UserRole;
import br.ufc.smd.ecommercecopa.repository.ClientRepository;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, ClientRepository clientRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public RegisterClientResponse registerClient(RegisterClientRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AppException("DUPLICATE_RESOURCE", "Email já cadastrado", HttpStatus.CONFLICT);
        }
        if (clientRepository.existsByCpf(request.cpf())) {
            throw new AppException("DUPLICATE_RESOURCE", "CPF já cadastrado", HttpStatus.CONFLICT);
        }

        LocalDate dateOfBirth;
        try {
            dateOfBirth = LocalDate.parse(request.dateOfBirth());
        } catch (Exception ex) {
            throw new AppException("VALIDATION_ERROR", "DateOfBirth deve estar no formato yyyy-MM-dd", HttpStatus.BAD_REQUEST);
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(normalizedEmail);
        user.setRole(UserRole.CLIENT);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User savedUser = userRepository.save(user);

        Client client = new Client();
        client.setUser(savedUser);
        client.setCpf(request.cpf());
        client.setDateOfBirth(dateOfBirth);
        clientRepository.save(client);

        return new RegisterClientResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    public AuthUserResponse login(LoginRequest request, HttpSession session) {
        String normalizedEmail = normalizeEmail(request.email());

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new AppException("AUTH_INVALID_CREDENTIALS", "Email ou senha inválidos", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException("AUTH_INVALID_CREDENTIALS", "Email ou senha inválidos", HttpStatus.UNAUTHORIZED);
        }

        session.setAttribute(SessionKeys.AUTH_USER_ID, user.getId());

        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), true);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public AuthUserResponse me(HttpSession session) {
        User user = requireSessionUser(session);
        return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), true);
    }

    public User requireSessionUser(HttpSession session) {
        Object userIdObj = session.getAttribute(SessionKeys.AUTH_USER_ID);
        if (!(userIdObj instanceof UUID userId)) {
            throw new AppException("AUTH_REQUIRED", "Sessão inválida ou expirada", HttpStatus.UNAUTHORIZED);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("AUTH_REQUIRED", "Sessão inválida ou expirada", HttpStatus.UNAUTHORIZED));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
