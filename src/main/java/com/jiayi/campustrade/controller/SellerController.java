package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.SellerApplication;
import com.jiayi.campustrade.service.SellerApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 学生侧「申请成为卖家」接口
 *
 * 路径挂在 /api/seller/** 下，但这两个接口对 student 开放：
 * AuthInterceptor 对 /seller/apply 与 /seller/application/my 精确放行，
 * 真正的角色 / 状态校验在 SellerApplicationService 中完成（权限在 Service 层兜底）。
 *
 * 注意：这里不是卖家业务接口（发布商品等卖家功能仍在 goods 等模块内做角色校验）。
 */
@RestController
@RequestMapping("/seller")
public class SellerController {

    @Autowired
    private SellerApplicationService sellerApplicationService;

    /**
     * 提交卖家申请
     * POST /api/seller/apply   body: { "reason": "..." }
     */
    @PostMapping("/apply")
    public String apply(@RequestBody Map<String, Object> body) {

        Object reasonObj = body == null ? null : body.get("reason");
        String reason = reasonObj == null ? null : String.valueOf(reasonObj);

        try {
            sellerApplicationService.apply(reason);
            return "申请提交成功，请等待管理员审核";
        } catch (RuntimeException e) {
            return e.getMessage();
        }
    }

    /**
     * 查看当前用户自己最新一条申请状态
     * GET /api/seller/application/my
     * 从未申请过时 application 为 null
     */
    @GetMapping("/application/my")
    public Map<String, Object> myApplication() {

        SellerApplication application = sellerApplicationService.findMyLatest();

        Map<String, Object> result = new HashMap<>();
        result.put("application", application);

        return result;
    }

}
