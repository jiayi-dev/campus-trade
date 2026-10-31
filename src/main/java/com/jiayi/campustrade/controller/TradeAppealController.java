package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.TradeAppeal;
import com.jiayi.campustrade.service.TradeAppealService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交易申诉（支持证据图片）
 *
 * 用户端：
 *   POST /appeals            提交申诉 { orderId, reason, detail, evidences? }
 *                            evidences: 图片 URL 数组，可选，最多 5 张
 *   GET  /appeals/my         我提交的申诉（含 evidences JSON 字符串）
 * 管理员：
 *   GET  /appeals            全部申诉 ?status=0
 *   PUT  /appeals/{id}/handle 处理申诉 { status: 1|2, comment }
 *
 * 证据图片上传：
 *   前端先用已有的 POST /api/upload/image 拿到 URL（如 /api/files/appeal/xxx.jpg），
 *   然后把 URL 数组传给本接口 evidences 字段。
 */
@RestController
@RequestMapping("/appeals")
public class TradeAppealController {

    @Autowired
    private TradeAppealService tradeAppealService;

    private final ObjectMapper om = new ObjectMapper();


    @PostMapping
    public String create(@RequestBody Map<String, Object> body) {

        Object orderIdObj = body.get("orderId");

        if (orderIdObj == null) {
            return "缺少订单ID";
        }

        Integer orderId;

        try {
            orderId = Integer.valueOf(String.valueOf(orderIdObj));
        } catch (NumberFormatException e) {
            return "订单ID格式不正确";
        }

        Object reasonObj = body.get("reason");
        Object detailObj = body.get("detail");

        String reason = reasonObj == null ? null : String.valueOf(reasonObj);
        String detail = detailObj == null ? null : String.valueOf(detailObj);

        // evidences 兼容两种前端传法：数组 或 JSON 字符串
        List<String> evidences = null;

        Object evidencesObj = body.get("evidences");

        if (evidencesObj != null) {

            try {
                if (evidencesObj instanceof List) {
                    evidences = om.convertValue(evidencesObj, new TypeReference<List<String>>() {});
                } else if (evidencesObj instanceof String s && !s.isBlank()) {
                    evidences = om.readValue(s, new TypeReference<List<String>>() {});
                }
            } catch (Exception e) {
                return "证据图片格式不正确（需要 URL 数组）";
            }

        }

        try {

            Integer appealId = tradeAppealService.create(orderId, reason, detail, evidences);

            return "申诉已提交，申诉ID：" + appealId;

        } catch (RuntimeException e) {

            return e.getMessage();

        }
    }


    @GetMapping("/my")
    public List<TradeAppeal> my() {

        return tradeAppealService.findMine();

    }


    @GetMapping
    public List<TradeAppeal> findAll(
            @RequestParam(required = false) Integer status) {

        return tradeAppealService.findAllForAdmin(status);

    }


    @PutMapping("/{id}/handle")
    public String handle(
            @PathVariable("id") Integer appealId,
            @RequestBody Map<String, Object> body) {

        Object statusObj = body.get("status");

        if (statusObj == null) {
            return "缺少处理结果";
        }

        Integer status;

        try {
            status = Integer.valueOf(String.valueOf(statusObj));
        } catch (NumberFormatException e) {
            return "处理结果格式不正确";
        }

        Object commentObj = body.get("comment");

        String comment = commentObj == null ? null : String.valueOf(commentObj);

        try {

            tradeAppealService.handle(appealId, status, comment);

            return status == 1 ? "申诉已受理" : "申诉已驳回";

        } catch (RuntimeException e) {

            return e.getMessage();

        }
    }
}
