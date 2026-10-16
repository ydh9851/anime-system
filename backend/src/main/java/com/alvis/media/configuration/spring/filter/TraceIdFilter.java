package com.alvis.media.configuration.spring.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求日志 / 全链路 traceId 过滤器。
 *
 * 职责：
 *1. 生成或透传 traceId（请求头 X-Trace-Id），写入 MDC 供 logback 的 %X{traceId} 输出；
 *2. 回写响应头 X-Trace-Id，前端报错截图即可定位到后端同一次请求的日志；
 *3. 统一记录 method / uri / status / 耗时，慢接口一眼可见。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private static final Logger logger = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.trim().isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long cost = System.currentTimeMillis() - start;
            logger.info("{} {} status={} cost={}ms",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), cost);
            //必须清理，否则线程复用会把上一次请求的 traceId 带过来
            MDC.remove(TRACE_ID);
        }
    }

    /** 静态资源与文档类请求不记录，避免刷屏 */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/admin") || uri.startsWith("/static") || uri.startsWith("/eda");
    }
}
