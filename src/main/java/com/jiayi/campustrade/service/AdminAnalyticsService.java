package com.jiayi.campustrade.service;

import com.jiayi.campustrade.mapper.AdminAnalyticsMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台运营数据统计 Service（仅管理员使用）
 *
 * 统计口径全部来自真实数据库表，不做任何硬编码或模拟数据。
 * 趋势类数据按日期分组后由本层补齐为连续日期序列（缺失日期补 0），
 * 便于前端直接绘制折线/柱状图。
 */
@Service
public class AdminAnalyticsService {

    @Autowired
    private AdminAnalyticsMapper adminAnalyticsMapper;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 趋势天数允许范围
    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 90;

    /**
     * 平台运营总览
     *
     * @param days 趋势天数（近 N 天），收敛到 [7, 90]，null 默认 14
     */
    public Map<String, Object> overview(Integer days) {

        int safeDays = normalizeDays(days);

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. 总量指标（真实计数）
        result.put("totals", adminAnalyticsMapper.countTotals());

        // 2. 分布类
        result.put("categoryDistribution", adminAnalyticsMapper.categoryDistribution());
        result.put("orderStatusDistribution", adminAnalyticsMapper.orderStatusDistribution());
        result.put("campusDistribution", adminAnalyticsMapper.campusDistribution());

        // 3. 趋势类（补齐为连续日期序列）
        result.put("days", safeDays);
        result.put("goodsTrend", buildSeries(adminAnalyticsMapper.goodsTrend(safeDays), safeDays));
        result.put("orderTrend", buildSeries(adminAnalyticsMapper.orderTrend(safeDays), safeDays));
        result.put("userTrend", buildSeries(adminAnalyticsMapper.userTrend(safeDays), safeDays));

        // 4. 近期活动
        result.put("recentOrders", adminAnalyticsMapper.recentOrders(6));
        result.put("recentGoods", adminAnalyticsMapper.recentGoods(6));

        // 5. 异常数据提示（由真实计数派生，非模拟告警）
        result.put("anomalies", buildAnomalies());

        return result;
    }

    /**
     * 将「按日期分组」的稀疏结果补齐为近 days 天（含今天）的连续序列。
     * 说明：SQL 侧过滤条件为 create_time >= 今天 - days 天，
     *       本方法取最近 days 个自然日（今天往前 days-1 天）对齐展示，
     *       落在此区间外的分组行会被自然丢弃。
     */
    private List<Map<String, Object>> buildSeries(List<Map<String, Object>> rows, int days) {

        // 汇总为 date -> count
        Map<String, Integer> counted = new HashMap<>();

        if (rows != null) {
            for (Map<String, Object> row : rows) {

                Object dateObj = row.get("date");

                if (dateObj == null) {
                    continue;
                }

                counted.put(String.valueOf(dateObj), toInt(row.get("count")));
            }
        }

        LocalDate today = LocalDate.now();

        List<Map<String, Object>> series = new ArrayList<>(days);

        for (int i = days - 1; i >= 0; i--) {

            String date = today.minusDays(i).format(DATE_FMT);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("count", counted.getOrDefault(date, 0));

            series.add(point);
        }

        return series;
    }

    /**
     * 异常数据提示：基于真实计数派生
     * level：warn 需关注 / info 常规提示
     */
    private List<Map<String, Object>> buildAnomalies() {

        List<Map<String, Object>> anomalies = new ArrayList<>();

        int zeroPrice = adminAnalyticsMapper.countZeroPriceGoods();
        int disabledUsers = adminAnalyticsMapper.countDisabledUsers();
        int offSale = adminAnalyticsMapper.countOffSaleGoods();

        anomalies.add(anomaly(
                "价格异常商品",
                zeroPrice,
                zeroPrice > 0 ? "warn" : "info",
                "商品价格小于等于 0，疑似录入错误，建议在商品管理中核查"));

        anomalies.add(anomaly(
                "已禁用账号",
                disabledUsers,
                disabledUsers > 0 ? "warn" : "info",
                "账号状态为禁用的用户，在前台不可登录，请确认是否为预期操作"));

        anomalies.add(anomaly(
                "已下架商品",
                offSale,
                "info",
                "状态为下架的商品（含已售出与主动下架），仅作规模提示，不代表异常"));

        return anomalies;
    }

    private Map<String, Object> anomaly(String label, int count, String level, String hint) {

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("label", label);
        item.put("count", count);
        item.put("level", level);
        item.put("hint", hint);

        return item;
    }

    private int normalizeDays(Integer days) {

        int value = days == null ? 14 : days;

        if (value < MIN_DAYS) {
            value = MIN_DAYS;
        }

        if (value > MAX_DAYS) {
            value = MAX_DAYS;
        }

        return value;
    }

    private int toInt(Object value) {

        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
