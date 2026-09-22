package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.SellerApplication;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface SellerApplicationMapper {

    // 学生提交申请（status 由数据库默认值 0 兜底）
    int insert(SellerApplication application);

    // 按主键查询
    SellerApplication findById(@Param("applicationId") Integer applicationId);

    // 查询某用户当前是否存在待审核申请（防重复提交）
    SellerApplication findPendingByUserId(@Param("userId") Integer userId);

    // 查询某用户最新一条申请（学生状态页展示）
    SellerApplication findLatestByUserId(@Param("userId") Integer userId);

    // 管理员分页列表：JOIN 申请人信息与审核人姓名
    List<Map<String, Object>> findAdminPage(
            @Param("status") Integer status,
            @Param("offset") Integer offset,
            @Param("pageSize") Integer pageSize
    );

    // 管理员列表总数（与 findAdminPage 同条件）
    int countByStatus(@Param("status") Integer status);

    /**
     * 条件更新审核结果：只有 status = 0（待审核）才能更新成功。
     * 并发场景下第二个管理员审核会影响 0 行，由 Service 判定为重复审核。
     *
     * @return 实际更新行数
     */
    int review(
            @Param("applicationId") Integer applicationId,
            @Param("status") Integer status,
            @Param("reviewerId") Integer reviewerId,
            @Param("reviewComment") String reviewComment
    );

}
