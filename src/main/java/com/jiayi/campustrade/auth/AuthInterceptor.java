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

        // 1.5 游客可访问的公开只读 GET 接口
        // 商品列表/详情/图片/分类属于公共展示资源，游客无需 Token 即可浏览
        // 写操作（POST/PUT/DELETE）仍需登录，由下方 Token 校验拦截
        if ("GET".equalsIgnoreCase(request.getMethod())
                && isPublicReadPath(request.getRequestURI())) {
            return true;
        }

        // 1.6 AI 助手基础问答：页面与 Mock 问答对游客公开
        // 该接口不读写任何用户私有数据（conversationId 当前不落库），
        // 未来若加入“保存个人会话/查询我的订单”等能力，必须重新收紧为登录态
        if ("POST".equalsIgnoreCase(request.getMethod())
                && "/api/ai/chat".equals(request.getRequestURI())) {
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
        //    例外：学生「申请成为卖家」的两个接口对 student 开放，
        //    真正的角色与申请状态校验在 SellerApplicationService 中完成。
        boolean isSellerApplicationEntry =
                "/api/seller/apply".equals(uri)
                        || "/api/seller/application/my".equals(uri);

        if (!isSellerApplicationEntry
                && uri.startsWith("/api/seller/")
                && !"seller".equals(user.getRole())
                && !"admin".equals(user.getRole())) {

            writeError(response, 403, "没有卖家权限");
            return false;
        }

        // 9. 全量订单查询（GET /api/orders）仅管理员可访问
        //    精确匹配路径，不影响 /orders/buyer、/orders/seller 等子路径
        //    以及 POST /orders（下单）、PUT /orders/{id}/status（状态流转）
        if ("GET".equalsIgnoreCase(request.getMethod())
                && "/api/orders".equals(uri)
                && !"admin".equals(user.getRole())) {

            writeError(response, 403, "没有管理员权限");
            return false;
        }

        // 10. 身份验证通过，将用户ID放入当前请求上下文
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

    /**
     * 游客可访问的公开只读 GET 路径判断
     *
     * 放行范围（仅限 GET 方法）：
     * - /api/goods/page         商品列表（含筛选/搜索/分页查询参数）
     * - /api/goods/{id}         商品详情（id 为纯数字）
     * - /api/goods/{id}/images  商品全部图片（id 为纯数字）
     * - /api/categories         分类列表（详情页显示分类名用）
     *
     * 不放行的写操作（POST/PUT/DELETE）继续走 Token 校验：
     * - POST   /api/goods                发布商品
     * - PUT    /api/goods                修改商品
     * - DELETE /api/goods/{id}           删除商品
     * - POST   /api/goods/{id}/images    关联图片
     * - POST   /api/goods/{id}/view      浏览量上报
     * - PUT    /api/goods/{id}/status    上下架
     */
    private boolean isPublicReadPath(String uri) {
        if (uri == null) return false;

        // GET /api/goods/page — 商品列表（含筛选/搜索/分页查询参数）
        if ("/api/goods/page".equals(uri)) return true;

        // GET /api/categories — 分类列表
        if ("/api/categories".equals(uri)) return true;

        // GET /api/goods/{id} 和 /api/goods/{id}/images
        if (uri.startsWith("/api/goods/")) {
            String remainder = uri.substring("/api/goods/".length());
            // /api/goods/26 → 商品详情
            if (remainder.matches("\\d+")) return true;
            // /api/goods/26/images → 商品全部图片
            if (remainder.matches("\\d+/images")) return true;
        }

        return false;
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