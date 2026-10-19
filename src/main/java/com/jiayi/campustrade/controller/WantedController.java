package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.WantedPost;
import com.jiayi.campustrade.entity.WantedResponse;
import com.jiayi.campustrade.service.WantedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wanted")
public class WantedController {

    @Autowired
    private WantedService wantedService;


    // =========================
    // 求购分页查询
    // =========================

    @GetMapping("/page")
    public Map<String, Object> findPage(

            // 标题模糊
            @RequestParam(required = false)
            String keyword,

            // 期望分类
            @RequestParam(required = false)
            Integer categoryId,

            // 期望校区
            @RequestParam(required = false)
            String campus,

            // 低预算筛选：只看预算上限 <= 该值的求购
            @RequestParam(required = false)
            BigDecimal budgetMax,

            // 状态：1 进行中 / 2 已关闭；不传则全部返回（前端默认只传 1）
            @RequestParam(required = false)
            Integer status,

            // 排序：latest 最新发布 / budget_desc 预算高→低 / budget_asc 预算低→高
            @RequestParam(defaultValue = "latest")
            String sortBy,

            @RequestParam(defaultValue = "1")
            Integer pageNum,

            @RequestParam(defaultValue = "9")
            Integer pageSize) {

        return wantedService.findPage(
                keyword,
                categoryId,
                campus,
                budgetMax,
                status,
                sortBy,
                pageNum,
                pageSize
        );
    }


    // =========================
    // 求购详情（不存在返回 null）
    // =========================

    @GetMapping("/{id}")
    public WantedPost findById(
            @PathVariable("id") Integer id) {

        return wantedService.findById(id);

    }


    // =========================
    // 发布求购：POST /wanted
    // =========================

    @PostMapping
    public String publish(
            @RequestBody WantedPost post) {

        try {

            Integer wantedId = wantedService.publish(post);

            return "求购发布成功，求购ID：" + wantedId;

        } catch (RuntimeException e) {

            // 业务失败（校验不通过等）以文本返回，与项目现有接口风格一致
            return e.getMessage();

        }
    }


    // =========================
    // 修改求购（仅发布者本人）：PUT /wanted
    // =========================

    @PutMapping
    public String update(
            @RequestBody WantedPost post) {

        try {

            wantedService.update(post);

            return "求购更新成功";

        } catch (RuntimeException e) {

            return e.getMessage();

        }
    }


    // =========================
    // 关闭求购：PUT /wanted/{id}/status，请求体 { status: 2 }
    // =========================

    @PutMapping("/{id}/status")
    public String updateStatus(
            @PathVariable("id") Integer wantedId,
            @RequestBody Map<String, Object> body) {

        Object statusObj = body.get("status");

        if (statusObj == null) {
            return "缺少求购状态";
        }

        Integer targetStatus;

        try {
            targetStatus = Integer.valueOf(String.valueOf(statusObj));
        } catch (NumberFormatException e) {
            return "求购状态格式不正确";
        }

        try {

            wantedService.updateStatus(wantedId, targetStatus);

            return "求购已关闭";

        } catch (RuntimeException e) {

            return e.getMessage();

        }
    }


    // =========================
    // 求购响应列表：GET /wanted/{id}/responses
    // =========================

    @GetMapping("/{id}/responses")
    public List<WantedResponse> findResponses(
            @PathVariable("id") Integer wantedId) {

        return wantedService.findResponses(wantedId);

    }


    // =========================
    // 提交响应：POST /wanted/{id}/responses，请求体 { message }
    // =========================

    @PostMapping("/{id}/responses")
    public String respond(
            @PathVariable("id") Integer wantedId,
            @RequestBody Map<String, Object> body) {

        Object messageObj = body.get("message");

        String message = messageObj == null ? null : String.valueOf(messageObj);

        try {

            wantedService.respond(wantedId, message);

            return "响应已提交";

        } catch (RuntimeException e) {

            return e.getMessage();

        }
    }
}
