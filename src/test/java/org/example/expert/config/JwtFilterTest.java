package org.example.expert.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class JwtFilterTest {
    @InjectMocks
    JwtFilter jwtFilter;

    @Mock
    FilterConfig filterConfig;

    @Mock
    HttpServletRequest httpRequest;

    @Mock
    HttpServletResponse httpResponse;

    @Mock
    FilterChain chain;

    @Mock
    Claims claims;

    @Spy
    JwtUtil jwtUtil;

    @BeforeEach
    void prepare() {
        ReflectionTestUtils.setField(jwtUtil, "secretKey", Base64.getEncoder().encodeToString("this is test secret key this is test secret key this is test secret key this is test secret key".getBytes()));
        jwtUtil.init();
    }

    @Test
    @DisplayName("JwtFilter::init()")
    void test1() {
        assertDoesNotThrow(() -> jwtFilter.init(filterConfig));
    }

    @Nested
    @DisplayName("JwtFilter::doFilter()")
    class Class1 {
        @Test
        @DisplayName("/auth로 시작하면 토큰 검사하지 않는다.")
        void test1() {
            // given
            given(httpRequest.getRequestURI()).willReturn("/auth");

            // when & then
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            verify(httpRequest, times(0)).getHeader("Authorization");
        }

        @Test
        @DisplayName("Authorization 헤더가 없으면 예외가 발생한다.")
        void test2() {
            // given
            given(httpRequest.getRequestURI()).willReturn("");
            given(httpRequest.getHeader("Authorization")).willReturn(null);

            // when & then
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST, "JWT 토큰이 필요합니다."));
        }

        @Test
        @DisplayName("Authorization 값이 유효하지 않으면 예외 발생.")
        void test3() {
            // given
            given(httpRequest.getRequestURI()).willReturn("");
            given(httpRequest.getHeader("Authorization")).willReturn("");
            doReturn(null).when(jwtUtil).extractClaims(any());

            // when & then
            ServerException serverException = assertThrows(ServerException.class, () -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertEquals(serverException.getMessage(), "Not Found Token");
        }

        @Test
        @DisplayName("Claims 없으면 예외가 발생한다.")
        void test4() {
            // given
            given(httpRequest.getRequestURI()).willReturn("");
            given(httpRequest.getHeader("Authorization")).willReturn("Bearer 123123");
            doReturn(null).when(jwtUtil).extractClaims(any());

            // when & then
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST, "잘못된 JWT 토큰입니다."));
        }

        @Test
        @DisplayName("/admin에 접근했는데, ADMIN이 아니면 오류 발생.")
        void test5() {
            // given
            given(httpRequest.getRequestURI()).willReturn("/admin");
            given(httpRequest.getHeader("Authorization")).willReturn("Bearer 123123");
            doReturn(claims).when(jwtUtil).extractClaims(any());
            given(claims.getSubject()).willReturn("1");
            given(claims.get("email")).willReturn("a@a.com");
            given(claims.get("userRole")).willReturn(UserRole.USER.name());
            given(claims.get("userRole", String.class)).willReturn(UserRole.USER.name());
            doReturn(claims).when(jwtUtil).extractClaims(any());

            // when & then
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_FORBIDDEN, "관리자 권한이 없습니다."));
        }

        @Test
        @DisplayName("접근 정상 작동")
        void test6() {
            // given
            given(httpRequest.getRequestURI()).willReturn("");
            given(httpRequest.getHeader("Authorization")).willReturn("Bearer 123123");
            doReturn(claims).when(jwtUtil).extractClaims(any());
            given(claims.getSubject()).willReturn("1");
            given(claims.get("email")).willReturn("a@a.com");
            given(claims.get("userRole")).willReturn(UserRole.USER.name());
            given(claims.get("userRole", String.class)).willReturn(UserRole.USER.name());
            doReturn(claims).when(jwtUtil).extractClaims(any());

            // when & then
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
        }

        @Test
        @DisplayName("예외 발생 정상 처리")
        void test7() {
            // given
            given(httpRequest.getRequestURI()).willReturn("");
            given(httpRequest.getHeader("Authorization")).willReturn("Bearer 123123");
            doReturn(claims).when(jwtUtil).extractClaims(any());
            given(claims.getSubject()).willReturn("1");
            given(claims.get("email")).willReturn("a@a.com");
            given(claims.get("userRole")).willReturn(UserRole.USER.name());
            given(claims.get("userRole", String.class)).willReturn(UserRole.USER.name());

            // when & then
            doThrow(new SecurityException()).when(jwtUtil).extractClaims(any());
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            doThrow(new MalformedJwtException("message")).when(jwtUtil).extractClaims(any());
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(2)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않는 JWT 서명입니다."));

            doThrow(new ExpiredJwtException(null, null, "message")).when(jwtUtil).extractClaims(any());
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_UNAUTHORIZED, "만료된 JWT 토큰입니다."));

            doThrow(new UnsupportedJwtException("")).when(jwtUtil).extractClaims(any());
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST, "지원되지 않는 JWT 토큰입니다."));

            doThrow(new RuntimeException()).when(jwtUtil).extractClaims(any());
            assertDoesNotThrow(() -> jwtFilter.doFilter(httpRequest, httpResponse, chain));
            assertDoesNotThrow(() -> verify(httpResponse, times(1)).sendError(HttpServletResponse.SC_BAD_REQUEST, "유효하지 않는 JWT 토큰입니다."));
        }
    }

    @Test
    @DisplayName("JwtFilter::destroy() 정상 작동")
    void test2() {
        assertDoesNotThrow(() -> jwtFilter.destroy());
    }
}
