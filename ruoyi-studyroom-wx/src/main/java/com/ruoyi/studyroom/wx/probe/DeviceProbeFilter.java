package com.ruoyi.studyroom.wx.probe;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * 门禁设备协议抓包过滤器
 * <p>
 * 设备服务器地址改为本系统后, 设备会按私有协议向本系统发起请求。
 * 本过滤器记录每一个请求的原始报文(method + uri + query + headers + body)
 * 到日志与 sr_access_packet 表, 用于反推设备协议。不做任何业务处理, 只记录。
 */
@Slf4j
@Component
@Order(Integer.MIN_VALUE)
public class DeviceProbeFilter extends OncePerRequestFilter {

    private static final String[] PROBE_PATHS = {"/server", "/access", "/api/faceDeivce", "/api/faceDevice"};

    private final JdbcTemplate jdbcTemplate;

    @Value("${studyroom.access.probe.enabled:true}")
    private boolean probeEnabled;

    public DeviceProbeFilter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!probeEnabled || !isProbePath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }
        // 包装请求以缓存 body, 后续 Controller 仍可正常读取
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request);
        try {
            filterChain.doFilter(wrapper, response);
        } finally {
            record(wrapper, response.getStatus());
        }
    }

    private boolean isProbePath(String uri) {
        if (uri == null) {
            return false;
        }
        for (String p : PROBE_PATHS) {
            if (uri.startsWith(p)) {
                return true;
            }
        }
        return "/".equals(uri) || "/server".equals(uri);
    }

    private void record(ContentCachingRequestWrapper wrapper, int status) {
        try {
            String method = wrapper.getMethod();
            String uri = wrapper.getRequestURI();
            String query = wrapper.getQueryString();
            StringBuilder headers = new StringBuilder();
            Enumeration<String> names = wrapper.getHeaderNames();
            if (names != null) {
                while (names.hasMoreElements()) {
                    String n = names.nextElement();
                    String v = wrapper.getHeader(n);
                    if (v != null && v.length() > 500) {
                        v = v.substring(0, 500) + "...";
                    }
                    headers.append(n).append(": ").append(v).append("\n");
                }
            }
            byte[] body = wrapper.getContentAsByteArray();
            String bodyStr = body == null || body.length == 0 ? ""
                : new String(body, StandardCharsets.UTF_8);
            if (bodyStr.length() > 4000) {
                bodyStr = bodyStr.substring(0, 4000) + "...(truncated)";
            }
            String remoteIp = wrapper.getRemoteAddr();
            String line = String.format(
                "[PROBE] %s %s%s\nheaders:\n%sbody: %s\nremoteIp: %s\nstatus: %d",
                method, uri, query == null ? "" : "?" + query, headers, bodyStr, remoteIp, status);
            log.info(line);
            // 落库
            jdbcTemplate.update(
                "INSERT INTO sr_access_packet (method, uri, query_string, headers, body, remote_ip, status, create_time) VALUES (?,?,?,?,?,?,?,NOW())",
                method, uri, query, headers.length() == 0 ? null : headers.toString(),
                bodyStr.isEmpty() ? null : bodyStr, remoteIp, status);
        } catch (Exception e) {
            log.warn("[PROBE] record failed: {}", e.getMessage());
        }
    }
}
