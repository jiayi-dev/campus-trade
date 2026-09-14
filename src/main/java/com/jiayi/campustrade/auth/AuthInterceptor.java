package com.jiayi.campustrade.auth;

import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private TokenManager tokenManager;

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        // 1. OPTIONS 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取 Token
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        // 3. 支持 Bearer Token 格式
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 4. 验证 Token
        Integer userId = tokenManager.getUserId(token);

        if (userId == null) {
            writeError(response, 401, "Token无效或已过期");
            return false;
        }

        // 5. 查询当前用户
        User user = userMapper.findById(userId);

        if (user == null) {
            writeError(response, 401, "用户不存在");
            return false;
        }

        // 6. 获取当前请求路径
        String uri = request.getRequestURI();

        // 7. 管理员权限
        if (uri.startsWith("/api/admin/")
                && !"admin".equals(user.getRole())) {

            writeError(response, 403, "没有管理员权限");
            return false;
        }

        // 8. 卖家权限
        if (uri.startsWith("/api/seller/")
                && !"seller".equals(user.getRole())
                && !"admin".equals(user.getRole())) {

            writeError(response, 403, "没有卖家权限");
            return false;
        }

        // 9. 身份验证通过，将用户ID放入当前请求上下文
        AuthContext.setUserId(userId);

        return true;
    }

    /**
     * 统一处理错误响应
     */
    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws Exception {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex) {

        // 请求结束后清理 ThreadLocal，防止用户信息残留
        AuthContext.clear();
    }
}