package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.annotation.Auth;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.MethodParameter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.NativeWebRequest;

import java.lang.annotation.Annotation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;

@ExtendWith(SpringExtension.class)
public class AuthUserArgumentResolverTest {

    @InjectMocks
    AuthUserArgumentResolver authUserArgumentResolver;

    @Mock
    MethodParameter methodParameter;

    @Mock
    NativeWebRequest webRequest;

    @Mock
    HttpServletRequest request;

    @Nested
    @DisplayName("AuthUserArgumentResolver::supportsParameter()")
    class Class1 {
        @Test
        @DisplayName("@Auth 어노테이션과 AuthUser 타입이 함께 사용되지 않은 경우 예외 발생.")
        void test1() {
            //given
            given(methodParameter.getParameterAnnotation(Auth.class)).willReturn(null);
            doReturn(AuthUser.class).when(methodParameter).getParameterType();

            // when & then
            AuthException authException = assertThrows(AuthException.class, () -> authUserArgumentResolver.supportsParameter(methodParameter));
            assertEquals(authException.getMessage(), "@Auth와 AuthUser 타입은 함께 사용되어야 합니다.");
        }

        @Test
        @DisplayName("@Auth 어노테이션과 AuthUser 타입이 함께 사용되지 않은 경우 예외 발생.")
        void test1_2() {
            //given
            Auth auth = new Auth() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Auth.class;
                }
            };

            given(methodParameter.getParameterAnnotation(Auth.class)).willReturn(auth);
            doReturn(Object.class).when(methodParameter).getParameterType();

            // when & then
            AuthException authException = assertThrows(AuthException.class, () -> authUserArgumentResolver.supportsParameter(methodParameter));
            assertEquals(authException.getMessage(), "@Auth와 AuthUser 타입은 함께 사용되어야 합니다.");
        }

        @Test
        @DisplayName("제대로 작동한다.")
        void test2() {
            //given
            given(methodParameter.getParameterAnnotation(Auth.class)).willReturn(null);
            doReturn(Object.class).when(methodParameter).getParameterType();

            // when & then
            assertFalse(authUserArgumentResolver.supportsParameter(methodParameter));
        }

        @Test
        @DisplayName("제대로 작동한다.")
        void test2_2() {
            //given
            Auth auth = new Auth() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return Auth.class;
                }
            };

            given(methodParameter.getParameterAnnotation(Auth.class)).willReturn(auth);
            doReturn(AuthUser.class).when(methodParameter).getParameterType();

            // when & then
            assertTrue(authUserArgumentResolver.supportsParameter(methodParameter));
        }
    }

    @Nested
    @DisplayName("AuthUserArgumentResolver::resolveArgument()")
    class Class2 {
        @Test
        @DisplayName("제대로 작동한다.")
        void test1() {
            // given
            Long userId = 1L;
            String email = "a@a.com";
            String userRole = UserRole.USER.name();

            given(webRequest.getNativeRequest()).willReturn(request);
            given(request.getAttribute("userId")).willReturn(userId);
            given(request.getAttribute("email")).willReturn(email);
            given(request.getAttribute("userRole")).willReturn(userRole);

            // when
            AuthUser authUser = (AuthUser) authUserArgumentResolver.resolveArgument(null, null, webRequest, null);

            assertNotNull(authUser);
            assertEquals(authUser.getId(), userId);
            assertEquals(authUser.getEmail(), email);
            assertEquals(authUser.getUserRole().name(), userRole);
        }
    }
}
