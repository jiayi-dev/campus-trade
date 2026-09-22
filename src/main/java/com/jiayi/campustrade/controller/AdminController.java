package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.AdminRecord;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.service.AdminService;
import com.jiayi.campustrade.service.GoodsService;
import com.jiayi.campustrade.service.OrderService;
import com.jiayi.campustrade.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员后台接口
 *
 * 全部挂在 /api/admin/** 下，由 AuthInterceptor 统一校验 admin 角色。
 * 非管理员访问会被拦截器返回 403，不进入这里。
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoodsService goodsService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private AdminService adminService;


    // =========================
    // 用户管理
    // =========================

    // 用户分页列表（用户名/手机号模糊、角色、状态筛选）
    @GetMapping("/users")
    public Map<String, Object> userPage(

            @RequestParam(required = false) String username,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "8") Integer pageSize) {

        List<User> list = userService.findPage(username, phone, role, status, pageNum, pageSize);
        int total = userService.count(username, phone, role, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    // 启用 / 禁用用户：PUT /admin/users/{id}/status  body { status: 0 禁用 / 1 启用 }
    @PutMapping("/users/{id}/status")
    public String updateUserStatus(
            @PathVariable("id") Integer userId,
            @RequestBody Map<String, Object> body) {

        Object statusObj = body.get("status");

        if (statusObj == null) {
            return "缺少用户状态";
        }

        Integer status;

        try {
            status = Integer.valueOf(String.valueOf(statusObj));
        } catch (NumberFormatException e) {
            return "用户状态格式不正确";
        }

        try {
            adminService.updateUserStatus(userId, status);
            return status == 1 ? "用户已启用" : "用户已禁用";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }


    // =========================
    // 商品管理
    // =========================

    // 商品分页列表（名称、分类、状态筛选，JOIN 卖家名与分类名）
    @GetMapping("/goods")
    public Map<String, Object> goodsPage(

            @RequestParam(required = false) String goodsName,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "8") Integer pageSize) {

        List<Map<String, Object>> list =
                goodsService.findAdminPage(goodsName, categoryId, status, pageNum, pageSize);
        int total = goodsService.countAdmin(goodsName, categoryId, status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    // 下架商品：复用 GoodsService 业务规则（管理员通过归属校验，但仍受订单保护）
    @PutMapping("/goods/{id}/offSale")
    public String offSaleGoods(@PathVariable("id") Integer goodsId) {

        try {
            adminService.offSaleGoods(goodsId);
            return "商品已下架";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    // 删除商品：复用 GoodsService.deleteById（有订单的商品会被拒绝）
    @DeleteMapping("/goods/{id}")
    public String deleteGoods(@PathVariable("id") Integer goodsId) {

        try {
            adminService.deleteGoods(goodsId);
            return "商品删除成功";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }


    // =========================
    // 订单管理
    // =========================

    // 平台全部订单（可选订单状态筛选，一条 SQL JOIN 出商品名与买卖双方）
    @GetMapping("/orders")
    public List<Map<String, Object>> orders(
            @RequestParam(required = false) Integer orderStatus) {

        return orderService.findAllForAdmin(orderStatus);
    }


    // =========================
    // 卖家申请审核
    // =========================

    // 申请分页列表（status 筛选：0待审核 1已通过 2已拒绝；JOIN 申请人/审核人信息）
    @GetMapping("/seller-applications")
    public Map<String, Object> sellerApplications(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "8") Integer pageSize) {

        List<Map<String, Object>> list =
                adminService.findApplications(status, pageNum, pageSize);
        int total = adminService.countApplications(status);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);

        return result;
    }

    // 通过申请：申请状态→已通过，user.role→seller（同一事务）
    // body: { "reviewComment": "可选审核意见" }
    @PutMapping("/seller-applications/{id}/approve")
    public String approveApplication(
            @PathVariable("id") Integer applicationId,
            @RequestBody(required = false) Map<String, Object> body) {

        String comment = body == null ? null : String.valueOf(body.get("reviewComment"));

        try {
            adminService.approveApplication(applicationId, "null".equals(comment) ? null : comment);
            return "申请已通过，用户已成为卖家";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    // 拒绝申请：body: { "reviewComment": "必填审核意见" }，user.role 保持 student
    @PutMapping("/seller-applications/{id}/reject")
    public String rejectApplication(
            @PathVariable("id") Integer applicationId,
            @RequestBody Map<String, Object> body) {

        Object commentObj = body == null ? null : body.get("reviewComment");
        String comment = commentObj == null ? null : String.valueOf(commentObj);

        try {
            adminService.rejectApplication(applicationId, comment);
            return "申请已拒绝";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }


    // =========================
    // 操作记录
    // =========================

    @GetMapping("/records")
    public List<AdminRecord> records() {
        return adminService.findRecords();
    }

}
