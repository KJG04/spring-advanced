package org.example.expert.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
public class FilterConfigTest {
    @Mock
    JwtUtil jwtUtil;

    @InjectMocks
    FilterConfig filterConfig;

    @Nested
    @DisplayName("FilterConfig::jwtFilter()")
    class Class1 {
        @Test
        @DisplayName("제대로 작동한다.")
        void test1() {
            // when
            FilterRegistrationBean<JwtFilter> registrationBean = filterConfig.jwtFilter();

            // then
            assertNotNull(registrationBean);
            assertNotNull(registrationBean.getFilter());
        }
    }
}
