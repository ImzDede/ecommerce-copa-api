package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.dto.admin.UpdateAdminMeRequest;
import br.ufc.smd.ecommercecopa.model.Admin;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.model.UserRole;
import br.ufc.smd.ecommercecopa.repository.AdminRepository;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpSession session;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(authService, adminRepository, userRepository, passwordEncoder);
    }

    @Test
    void meReturnsCurrentAdminProfile() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        Admin admin = new Admin();
        admin.setUser(user);
        when(authService.requireAdmin(session)).thenReturn(user);
        when(adminRepository.findById(userId)).thenReturn(Optional.of(admin));

        var response = adminService.me(session);

        assertEquals(userId, response.userId());
        assertEquals("Admin", response.name());
        assertEquals("old@example.com", response.email());
    }

    @Test
    void updateMeUpdatesCommonUserFields() {
        UUID userId = UUID.randomUUID();
        User user = user(userId);
        Admin admin = new Admin();
        admin.setUser(user);
        when(authService.requireAdmin(session)).thenReturn(user);
        when(adminRepository.findById(userId)).thenReturn(Optional.of(admin));
        when(userRepository.existsByEmailAndIdNot("admin@example.com", userId)).thenReturn(false);
        when(passwordEncoder.encode("Aa!123")).thenReturn("encoded");

        var response = adminService.updateMe(new UpdateAdminMeRequest(" Novo Admin ", " ADMIN@example.com ", "Aa!123"), session);

        assertEquals("Novo Admin", response.name());
        assertEquals("admin@example.com", response.email());
        assertEquals("encoded", user.getPasswordHash());
        verify(userRepository).save(user);
    }

    private User user(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Admin");
        user.setEmail("old@example.com");
        user.setRole(UserRole.ADMIN);
        user.setPasswordHash("hash");
        return user;
    }
}
