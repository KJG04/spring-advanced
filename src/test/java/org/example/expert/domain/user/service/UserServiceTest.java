package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Spy
    PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("UserService::getUser()")
    class Class1 {
        @Test
        @DisplayName("user가 없으면 예외가 발생한다.")
        void test1() {
            //given
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            //when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> userService.getUser(1));
            assertEquals("User not found", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("user가 정상적으로 응답된다.")
        void test2() {
            //given
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            // when
            UserResponse userResponse = userService.getUser(user.getId());

            assertEquals(user.getId(), userResponse.getId());
            assertEquals(user.getEmail(), userResponse.getEmail());
        }
    }

    @Nested
    @DisplayName("UserService::changePassword()")
    class Class2 {
        @Test
        @DisplayName("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.")
        void test1() {
            //given
            UserChangePasswordRequest userChangePasswordRequest1 = new UserChangePasswordRequest("Password1234!", "p");
            UserChangePasswordRequest userChangePasswordRequest2 = new UserChangePasswordRequest("Password1234!", "passwordpassword");
            UserChangePasswordRequest userChangePasswordRequest3 = new UserChangePasswordRequest("Password1234!", "password1234");

            // when & then
            InvalidRequestException invalidRequestException1 = assertThrows(InvalidRequestException.class, () -> userService.changePassword(1L, userChangePasswordRequest1));
            InvalidRequestException invalidRequestException2 = assertThrows(InvalidRequestException.class, () -> userService.changePassword(1L, userChangePasswordRequest2));
            InvalidRequestException invalidRequestException3 = assertThrows(InvalidRequestException.class, () -> userService.changePassword(1L, userChangePasswordRequest3));

            assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", invalidRequestException1.getMessage());
            assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", invalidRequestException2.getMessage());
            assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", invalidRequestException3.getMessage());
        }

        @Test
        @DisplayName("유저를 찾을 수 없으면 예외가 발생한다.")
        void test2() {
            //given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Password1234!", "Password1234");
            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            //when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> userService.changePassword(1L, userChangePasswordRequest));
            assertEquals("User not found", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("이전 비밀번호와 새 비밀번호가 같으면 예외가 발생한다.")
        void test3() {
            //given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Password1234!", "Password1234!");
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            ReflectionTestUtils.setField(user, "password", passwordEncoder.encode("Password1234!"));

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            //when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> userService.changePassword(user.getId(), userChangePasswordRequest));
            assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("비밀번호와 이전 비밀번호가 같지 않으면 예외가 발생한다.")
        void test4() {
            //given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Password1234!", "Password1234!");
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            ReflectionTestUtils.setField(user, "password", passwordEncoder.encode("Qwerasdf1234!"));

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            //when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> userService.changePassword(user.getId(), userChangePasswordRequest));
            assertEquals("잘못된 비밀번호입니다.", invalidRequestException.getMessage());
        }

        @Test
        @DisplayName("비밀번호가 정상적으로 변경된다.")
        void test5() {
            //given
            UserChangePasswordRequest userChangePasswordRequest = new UserChangePasswordRequest("Password1234!", "Newpassword1234!");
            AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
            User user = User.fromAuthUser(authUser);
            ReflectionTestUtils.setField(user, "password", passwordEncoder.encode("Password1234!"));

            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            //when
            userService.changePassword(user.getId(), userChangePasswordRequest);

            // then
            assertTrue(passwordEncoder.matches("Newpassword1234!", user.getPassword()));
        }
    }
}
