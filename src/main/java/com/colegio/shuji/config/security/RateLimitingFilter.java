package com.colegio.shuji.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de Rate Limiting para mitigar ataques de fuerza bruta, abuso y denegación de servicio (DoS)
 * en endpoints críticos de autenticación, pagos y webhooks.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

  private static final long WINDOW_MS = 60_000L; // 1 minuto
  private static final int MAX_ENTRIES = 10_000;

  private static final Pattern IPV4_PATTERN =
      Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
  private static final Pattern IPV6_PATTERN =
      Pattern.compile("^[0-9a-fA-F:]+$");

  private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

  @Value("${app.security.behind-trusted-proxy:false}")
  private boolean behindTrustedProxy;

  // Límites por endpoint
  private static final String LOGIN_PATH = "/api/v1/auth/login";
  private static final String REGISTER_PATH = "/api/v1/auth/register";
  private static final String REFRESH_PATH = "/api/v1/auth/refresh";

  private static final int LOGIN_LIMIT = 15;
  private static final int REGISTER_LIMIT = 10;
  private static final int REFRESH_LIMIT = 20;
  private static final int WEBHOOK_PAGOS_LIMIT = 120;
  private static final int PAGOS_TESORERIA_LIMIT = 30;

  public RateLimitingFilter() {
    this.behindTrustedProxy = false;
  }

  public RateLimitingFilter(boolean behindTrustedProxy) {
    this.behindTrustedProxy = behindTrustedProxy;
  }

  public void setBehindTrustedProxy(boolean behindTrustedProxy) {
    this.behindTrustedProxy = behindTrustedProxy;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    String uri = request.getRequestURI();
    Integer limit = resolveLimit(uri);

    if (limit == null || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
      filterChain.doFilter(request, response);
      return;
    }

    String ip = extractClientIp(request);
    String key = ip + ":" + uri;

    cleanOldEntriesIfNeeded();

    RequestCounter counter = requestCounts.computeIfAbsent(key, k -> new RequestCounter());

    if (!counter.allow(limit, WINDOW_MS)) {
      log.warn("Rate limit excedido para la IP {} en el endpoint {} (límite: {}/min)", ip, uri, limit);
      response.setStatus(429);
      response.setHeader("Retry-After", "60");
      response.setContentType("application/json");
      response.getWriter().write(
          "{\"success\":false,\"status\":429,\"errorCode\":\"TOO_MANY_REQUESTS\",\"message\":\"Demasiadas solicitudes. Espere antes de reintentar.\"}");
      return;
    }

    filterChain.doFilter(request, response);
  }

  private Integer resolveLimit(String uri) {
    if (uri == null) return null;
    if (uri.endsWith(LOGIN_PATH) || uri.equals(LOGIN_PATH)) {
      return LOGIN_LIMIT;
    } else if (uri.endsWith(REGISTER_PATH) || uri.equals(REGISTER_PATH)) {
      return REGISTER_LIMIT;
    } else if (uri.endsWith(REFRESH_PATH) || uri.equals(REFRESH_PATH)) {
      return REFRESH_LIMIT;
    } else if (uri.startsWith("/api/v1/pagos/webhook")) {
      return WEBHOOK_PAGOS_LIMIT;
    } else if (uri.startsWith("/api/v1/pagos") || uri.startsWith("/api/v1/tesoreria")) {
      return PAGOS_TESORERIA_LIMIT;
    }
    return null;
  }

  public String extractClientIp(HttpServletRequest request) {
    if (behindTrustedProxy) {
      String xfHeader = request.getHeader("X-Forwarded-For");
      if (xfHeader != null && !xfHeader.isBlank() && !"unknown".equalsIgnoreCase(xfHeader)) {
        String firstIp = xfHeader.split(",")[0].trim();
        if (isValidIp(firstIp)) {
          return firstIp;
        }
      }
    }
    return request.getRemoteAddr();
  }

  public static boolean isValidIp(String ip) {
    if (ip == null || ip.isBlank()) return false;
    return IPV4_PATTERN.matcher(ip).matches() || (ip.contains(":") && IPV6_PATTERN.matcher(ip).matches());
  }

  private void cleanOldEntriesIfNeeded() {
    if (requestCounts.size() > MAX_ENTRIES) {
      long now = System.currentTimeMillis();
      requestCounts.entrySet().removeIf(e -> now - e.getValue().getWindowStart() > WINDOW_MS * 2);
    }
  }

  public void reset() {
    requestCounts.clear();
  }

  public static class RequestCounter {
    private long windowStart = System.currentTimeMillis();
    private int count = 0;

    public synchronized boolean allow(int maxRequests, long windowDurationMs) {
      long now = System.currentTimeMillis();
      if (now - windowStart >= windowDurationMs) {
        windowStart = now;
        count = 0;
      }
      if (count < maxRequests) {
        count++;
        return true;
      }
      return false;
    }

    public synchronized long getWindowStart() {
      return windowStart;
    }

    public synchronized int getCount() {
      return count;
    }
  }
}
