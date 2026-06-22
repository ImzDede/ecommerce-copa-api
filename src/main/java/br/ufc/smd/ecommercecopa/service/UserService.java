package br.ufc.smd.ecommercecopa.service;

import br.ufc.smd.ecommercecopa.dto.user.UserPhotoResponse;
import br.ufc.smd.ecommercecopa.model.User;
import br.ufc.smd.ecommercecopa.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UploadService uploadService;

    public UserService(AuthService authService,
                       UserRepository userRepository,
                       UploadService uploadService) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.uploadService = uploadService;
    }

    @Transactional
    public UserPhotoResponse updateMyPhoto(MultipartFile photo, HttpSession session) {
        User user = authService.requireSessionUser(session);
        String previousPhoto = user.getProfilePhoto();
        user.setProfilePhoto(uploadService.saveImage(photo, "profiles"));
        userRepository.save(user);
        uploadService.deleteByPublicPath(previousPhoto);
        return new UserPhotoResponse(user.getProfilePhoto());
    }

    @Transactional
    public UserPhotoResponse deleteMyPhoto(HttpSession session) {
        User user = authService.requireSessionUser(session);
        String previousPhoto = user.getProfilePhoto();
        user.setProfilePhoto(null);
        userRepository.save(user);
        uploadService.deleteByPublicPath(previousPhoto);
        return new UserPhotoResponse(null);
    }

    @Transactional
    public void deleteMe(HttpSession session) {
        User user = authService.requireSessionUser(session);
        String previousPhoto = user.getProfilePhoto();
        user.setDeletedAt(LocalDateTime.now());
        user.setProfilePhoto(null);
        userRepository.save(user);
        session.invalidate();
        uploadService.deleteByPublicPath(previousPhoto);
    }
}
