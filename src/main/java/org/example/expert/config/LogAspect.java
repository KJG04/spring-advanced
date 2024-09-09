package org.example.expert.config;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Aspect
public class LogAspect {
    @Before("@annotation(org.example.expert.domain.common.annotation.Logging)")
    public void log() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        Long userId = (Long) request.getAttribute("userId");
        String path = request.getServletPath();
        String time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        System.out.printf("[%s] %s에 userId %s 가 접근함.%n", time, path, userId.toString());
    }
}
