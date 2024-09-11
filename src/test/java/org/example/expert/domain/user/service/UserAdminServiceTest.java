package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserAdminServiceTest {
    @InjectMocks
    UserAdminService userAdminService;

    @Mock
    UserRepository userRepository;

    @Nested
    @DisplayName("UserAdminService::changeUserRole()")
    class Class1 {
        @Test
        @DisplayName("유저가 없으면 예외가 발생한다.")
        void test1() {
            // given
            Long userId = 1L;
            UserRoleChangeRequest userRoleChangeRequest = new UserRoleChangeRequest(UserRole.USER.name());
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            InvalidRequestException invalidRequestException = assertThrows(InvalidRequestException.class, () -> userAdminService.changeUserRole(userId, userRoleChangeRequest));
            assertEquals(invalidRequestException.getMessage(), "User not found");
        }

        @Test
        @DisplayName("정상적으로 유저의 role이 변경된다.")
        void test2() {
            // given
            Long userId = 1L;
            User user = new User("a@a.com", "password", UserRole.USER);
            ReflectionTestUtils.setField(user, "id", userId);
            UserRoleChangeRequest userRoleChangeRequest = new UserRoleChangeRequest(UserRole.ADMIN.name());
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userAdminService.changeUserRole(userId, userRoleChangeRequest);

            // then
            assertEquals(user.getUserRole(), UserRole.ADMIN);
        }
    }
}
