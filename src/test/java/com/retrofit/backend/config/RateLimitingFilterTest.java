package com.retrofit.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimiterService rateLimiterService;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
        // Configure capacities for testing
        ReflectionTestUtils.setField(rateLimiterService, "authCapacity", 3L);
        ReflectionTestUtils.setField(rateLimiterService, "authRefillDurationSeconds", 60L);
        ReflectionTestUtils.setField(rateLimiterService, "apiCapacity", 5L);
        ReflectionTestUtils.setField(rateLimiterService, "apiRefillDurationSeconds", 60L);

        rateLimitingFilter = new RateLimitingFilter(rateLimiterService);
    }

    @Test
    @DisplayName("Preflight CORS (OPTIONS) should always be allowed")
    void testOptionsBypassesRateLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/auth/login");
        request.setServletPath("/auth/login");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("X-Rate-Limit-Remaining"));
    }

    @Test
    @DisplayName("Auth endpoint allows requests up to capacity and blocks with 429 when exceeded")
    void testAuthRateLimitingExceeded() throws ServletException, IOException {
        String testIp = "10.0.0.1";

        // First 3 requests should succeed (capacity is 3)
        for (int i = 0; i < 3; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
            request.setServletPath("/auth/login");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            rateLimitingFilter.doFilter(request, response, filterChain);

            assertEquals(200, response.getStatus());
            assertNotNull(response.getHeader("X-Rate-Limit-Remaining"));
        }

        // 4th request should be throttled with HTTP 429
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/auth/login");
        blockedRequest.setServletPath("/auth/login");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertEquals(429, blockedResponse.getStatus());
        assertNotNull(blockedResponse.getHeader("Retry-After"));
        assertEquals("0", blockedResponse.getHeader("X-Rate-Limit-Remaining"));
        assertTrue(blockedResponse.getContentAsString().contains("Demasiados intentos"));
    }

    @Test
    @DisplayName("General API endpoint allows requests up to capacity and throttles with 429")
    void testApiRateLimiting() throws ServletException, IOException {
        String testIp = "10.0.0.2";

        // First 5 requests should pass (capacity is 5)
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/projects");
            request.setServletPath("/projects");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain filterChain = new MockFilterChain();

            rateLimitingFilter.doFilter(request, response, filterChain);

            assertEquals(200, response.getStatus());
        }

        // 6th request is throttled
        MockHttpServletRequest blockedRequest = new MockHttpServletRequest("GET", "/projects");
        blockedRequest.setServletPath("/projects");
        blockedRequest.setRemoteAddr(testIp);
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();

        rateLimitingFilter.doFilter(blockedRequest, blockedResponse, filterChain);

        assertEquals(429, blockedResponse.getStatus());
        assertTrue(blockedResponse.getContentAsString().contains("Límite de solicitudes de la API excedido"));
    }
}
