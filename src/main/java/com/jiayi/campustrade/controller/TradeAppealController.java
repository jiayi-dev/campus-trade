package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.entity.TradeAppeal;
import com.jiayi.campustrade.service.TradeAppealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 交易申诉
 *
 * 用户端：
 *   POST /appeals            提交申诉 { orderId, reason, detail }
 *   GET  /appeals/my         我提交的申诉
 * 管理员：
 *   GET  /appeals            全部申诉 ?status=0
 *   PUT  /appeals/{id}/handle 处理申诉 { status: 1|2, comment }
 */
@RestController
@RequestMapping("/appeals")
public class TradeAppealController {

    @Autowired
    private TradeAppealService tradeAppealService;


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

        try {

            Integer appealId = tradeAppealService.create(orderId, reason, detail);

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
