package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
    @Mock
    UserRepository userRepository;

    @Spy
    PasswordEncoder passwordEncoder;

    @Spy
    JwtUtil jwtUtil;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(jwtUtil, "secretKey", Base64.getEncoder().encodeToString("this is test secret key this is test secret key this is test secret key this is test secret key".getBytes()));
        jwtUtil.init();
    }

    @Nested
    @DisplayName("AuthService::signup()")
    class Class1 {
        @Test
        @DisplayName("가입하려는 이메일이 이미 존재하면 예외 발생한다.")
        void test1() {
            // given
            SignupRequest signupRequest = new SignupRequest("a@a.com", "password", UserRole.USER.name());
            given(userRepository.existsByEmail(anyString())).willReturn(true);

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> authService.signup(signupRequest));
            assertEquals(invalidRequestException.getMessage(), "이미 존재하는 이메일입니다.");
        }

        @Test
        @DisplayName("가입이 정상적으로 처리된다.")
        void test2() {
            // given
            SignupRequest signupRequest = new SignupRequest("a@a.com", "password", UserRole.USER.name());
            Long userId = 1L;
            given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(false);
            given(userRepository.save(any())).willAnswer(invocationOnMock -> {
                Object user = invocationOnMock.getArgument(0);
                ReflectionTestUtils.setField(user, "id", userId);
                return user;
            });

            // when
            SignupResponse signupResponse = authService.signup(signupRequest);

            // then
            assertNotNull(signupResponse.getBearerToken());
            assertTrue(signupResponse.getBearerToken().startsWith("Bearer"));
        }
    }

    @Nested
    @DisplayName("AuthService::signin()")
    class Class2 {
        @Test
        @DisplayName("해당 이메일의 유저가 없으면 예외가 발생한다.")
        void test1() {
            // given
            SigninRequest signinRequest = new SigninRequest("a@a.com", "password");
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> authService.signin(signinRequest));
            assertEquals(invalidRequestException.getMessage(), "가입되지 않은 유저입니다.");
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
        void test2() {
            // given
            SigninRequest signinRequest = new SigninRequest("a@a.com", "diffpassword");
            Long userId = 1L;
            User user = new User("a@a.com", passwordEncoder.encode("password"), UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.of(user));

            // when & then
            AuthException authException = assertThrows(AuthException.class, () -> authService.signin(signinRequest));
            assertEquals(authException.getMessage(), "잘못된 비밀번호입니다.");
        }

        @Test
        @DisplayName("정상적으로 로그인 된다.")
        void test3() {
            // given
            SigninRequest signinRequest = new SigninRequest("a@a.com", "password");
            Long userId = 1L;
            User user = new User("a@a.com", passwordEncoder.encode("password"), UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);
            given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.of(user));

            // when
            SigninResponse signinResponse = authService.signin(signinRequest);

            // then
            assertNotNull(signinResponse.getBearerToken());
            assertTrue(signinResponse.getBearerToken().startsWith("Bearer"));
        }
    }
}
