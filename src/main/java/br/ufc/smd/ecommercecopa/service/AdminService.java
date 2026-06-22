package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.admin.AdminMeResponse;
import br.ufc.smd.ecommercecopa.dto.admin.UpdateAdminMeRequest;
import br.ufc.smd.ecommercecopa.exception.AppException;
import br.ufc.smd.ecommercecopa.model.Admin;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.AdminRepository;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final AuthService authService;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminService(AuthService authService,
                        AdminRepository adminRepository,
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.adminRepository = adminRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AdminMeResponse me(HttpSession session) {
        User user = authService.requireAdmin(session);
        Admin admin = adminRepository.findById(user.getId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Administrador não encontrado", HttpStatus.NOT_FOUND));
        return toResponse(admin);
    }

    @Transactional
    public AdminMeResponse updateMe(UpdateAdminMeRequest request, HttpSession session) {
        User user = authService.requireAdmin(session);
        Admin admin = adminRepository.findById(user.getId())
                .orElseThrow(() -> new AppException("RESOURCE_NOT_FOUND", "Administrador não encontrado", HttpStatus.NOT_FOUND));

        boolean hasName = request.name() != null && !request.name().isBlank();
        boolean hasEmail = request.email() != null && !request.email().isBlank();
        boolean hasPassword = request.password() != null && !request.password().isBlank();

        if (!hasName && !hasEmail && !hasPassword) {
            throw new AppException("BUSINESS_RULE_VIOLATION", "Informe ao menos um campo para atualizar", HttpStatus.UNPROCESSABLE_ENTITY);
        }

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
        return toResponse(admin);
    }

    private AdminMeResponse toResponse(Admin admin) {
        User user = admin.getUser();
        return new AdminMeResponse(user.getId(), user.getName(), user.getEmail(), user.getProfilePhoto());
    }

    private boolean isStrongPassword(String password) {
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z0-9]).+$");
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
