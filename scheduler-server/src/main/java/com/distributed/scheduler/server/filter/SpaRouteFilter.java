package com.distributed.scheduler.server.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SpaRouteFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 检查是否是API请求
        if (path.startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是静态资源请求
        if (isStaticResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        // 将所有其他请求转发到index.html
        request.getRequestDispatcher("/index.html").forward(request, response);
    }

    /**
     * 检查是否是静态资源请求
     */
    private boolean isStaticResource(String path) {
        // 直接访问根路径
        if (path.equals("/")) {
            return true;
        }
        
        // 直接访问index.html
        if (path.equals("/index.html")) {
            return true;
        }
        
        // 常见静态资源扩展名
        String[] staticExtensions = {".html", ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico", ".woff", ".woff2", ".ttf", ".eot", ".json"};
        
        for (String extension : staticExtensions) {
            if (path.endsWith(extension)) {
                return true;
            }
        }
        
        // 检查是否是子目录下的静态资源
        return path.startsWith("/assets/") || path.startsWith("/js/") || path.startsWith("/css/");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 初始化方法
    }

    @Override
    public void destroy() {
        // 销毁方法
    }
}