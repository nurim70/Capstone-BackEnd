package com.clothz.aistyling.api.service.user;

import com.clothz.aistyling.api.controller.user.request.UserCreateRequest;
import com.clothz.aistyling.api.controller.user.request.UserUpdateRequest;
import com.clothz.aistyling.api.service.user.response.UserImagesResponse;
import com.clothz.aistyling.api.service.user.response.UserInfoResponse;
import com.clothz.aistyling.api.service.user.response.UserSingUpResponse;
import com.clothz.aistyling.api.service.user.response.UserUpdateResponse;
import com.clothz.aistyling.domain.user.User;
import com.clothz.aistyling.domain.user.UserRepository;
import com.clothz.aistyling.domain.user.constant.UserRole;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

@RequiredArgsConstructor
@Transactional
@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserSingUpResponse signUp(final UserCreateRequest request, final List<String> imgUrls) throws JsonProcessingException {
        sameCheckEmail(request.email());
        final String encodePassword = passwordEncoder.encode(request.password());
        final String serializedImages = serializeImages(imgUrls);
        final User user = userRepository.save(createUserEntity(request, encodePassword, serializedImages));
        final var deserializedImages = deserializeImageUrls(user.getUserImages());
        return UserSingUpResponse.of(user, deserializedImages);
    }

    public void sameCheckEmail(final String email) {
        userRepository.findByEmail(email).ifPresent(n -> {
            throw new DuplicateKeyException("Email already exists");
        });
    }

    private User createUserEntity(final UserCreateRequest request, final String encodePassword, final String imgUrls) throws JsonProcessingException {
        return User.builder()
                .email(request.email())
                .nickname(request.nickname())
                .password(encodePassword)
                .userRole(UserRole.USER)
                .userImages(imgUrls)
                .build();
    }

    public UserImagesResponse uploadUserImg(final List<String> imgUrls, final Long id) throws IOException {
        if(imgUrls.isEmpty())
            throw new IllegalArgumentException("Image is empty");
        final User user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        final String serializedImages = serializeImages(imgUrls);
        user.saveImages(serializedImages);
        userRepository.save(user);
        return UserImagesResponse.from(imgUrls);
    }

    private String serializeImages(final List<String> imgUrls) throws JsonProcessingException {
        return objectMapper.writeValueAsString(imgUrls);
    }

    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(final Long id) throws JsonProcessingException {
        final User user = userRepository.findById(id).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );
        final var deserializedImages = deserializeImageUrls(user.getUserImages());
        return UserInfoResponse.of(user, deserializedImages);
    }

    private List<String> deserializeImageUrls(final String imgUrls) throws JsonProcessingException {
        if(null == imgUrls)
            return List.of();
        return objectMapper.readValue(imgUrls, new TypeReference<List<String>>() {});
    }

    public UserUpdateResponse updateUser(UserUpdateRequest request) throws NoSuchElementException {
        User user = checkEmailExist(request.email());
        user.updateNickname(request.nickname());
        user.updatePassword(request.password());
        User updateUser = userRepository.save(user);
        return UserUpdateResponse.of(user);
    }


    public User checkEmailExist(final String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> {
            throw new NoSuchElementException("Email does not exists");
        });
    }

}
