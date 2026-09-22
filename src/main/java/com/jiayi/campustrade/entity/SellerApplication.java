package com.jiayi.campustrade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卖家申请
 *
 * 状态 status：
 *   0 = 待审核
 *   1 = 已通过（通过后 user.role 同步变为 seller）
 *   2 = 已拒绝（user.role 保持 student，允许重新申请）
 */
@Data
public class SellerApplication {

    private Integer applicationId;

    private Integer userId;

    // 申请原因（10-500 字）
    private String reason;

    private Integer status;

    // 审核管理员 ID
    private Integer reviewerId;

    // 审核意见（拒绝时必填）
    private String reviewComment;

    private LocalDateTime createTime;

    private LocalDateTime reviewTime;

}
