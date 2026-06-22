package br.ufc.smd.ecommercecopa.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.model.UserRole;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UploadService uploadService;

    @Mock
    private HttpSession session;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(authService, userRepository, uploadService);
    }

    @Test
    void deleteMeSoftDeletesUserAndInvalidatesSession() {
        User user = user(UUID.randomUUID());
        user.setProfilePhoto("/uploads/profiles/photo.jpg");
        when(authService.requireSessionUser(session)).thenReturn(user);

        userService.deleteMe(session);

        assertNotNull(user.getDeletedAt());
        assertNull(user.getProfilePhoto());
        verify(userRepository).save(user);
        verify(session).invalidate();
        verify(uploadService).deleteByPublicPath("/uploads/profiles/photo.jpg");
    }

    private User user(UUID id) {
        User user = new User();
        ReflectionTestUtils.setField(user, "id", id);
        user.setName("Maria Silva");
        user.setEmail("maria@example.com");
        user.setRole(UserRole.CLIENT);
        user.setPasswordHash("hash");
        return user;
    }
}
