package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SpringExtension.class)
public class LogAspectTest {
    @InjectMocks
    LogAspect logAspect;

    @Mock
    HttpServletRequest httpServletRequest;

    @Mock
    LocalDateTime localDateTime;

    @Test
    @DisplayName("LogAspect::log()")
    void test1() {
        // given
        Long userId = 1L;
        String servletPath = "path";
        String time = "time";
        given(httpServletRequest.getAttribute("userId")).willReturn(userId);
        given(httpServletRequest.getServletPath()).willReturn(servletPath);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(httpServletRequest));
        given(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)).willReturn(time);
        MockedStatic<LocalDateTime> localDateTimeMockedStatic = mockStatic(LocalDateTime.class);
        given(LocalDateTime.now()).willReturn(localDateTime);

        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        // when
        logAspect.log();

        // then
        String expect = "[" + time + "] " + servletPath + "에 userId " + userId + " 가 접근함.\n";
        assertEquals(expect, outContent.toString());
        localDateTimeMockedStatic.close();
    }
}
