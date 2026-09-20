package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jiayi.campustrade.auth.AuthContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;


    // =========================
    // 实施六：分页查询（JOIN 主图 + 成色/校区筛选 + 多种排序）
    // =========================

    public List<Goods> findPage(
            String goodsName,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer conditionLevel,
            String campus,
            String sortBy,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return goodsMapper.findPage(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                conditionLevel,
                campus,
                sortBy,
                offset,
                pageSize
        );
    }


    // =========================
    // 查询商品总数（与 findPage 同筛选条件）
    // =========================

    public int count(
            String goodsName,
            Integer categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Integer conditionLevel,
            String campus) {

        return goodsMapper.count(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                conditionLevel,
                campus
        );
    }


    // =========================
    // 商品详情
    // =========================

    public Goods findById(Integer goodsId) {

        return goodsMapper.findById(goodsId);

    }


    // =========================
    // 新增商品
    // =========================

    public void insert(Goods goods) {

        // 发布人一律取当前登录用户，前端传什么 userId 都不生效
        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }

        goods.setUserId(userId);

        // 新商品默认在售，不信任前端传入的状态
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }

        goodsMapper.insert(goods);

    }


    // =========================
    // 修改商品
    // =========================

    public void update(Goods goods) {

        if (goods.getGoodsId() == null) {
            throw new RuntimeException("缺少商品ID");
        }

        // 归属校验：不是自己的商品（且非管理员）直接拒绝
        checkOwnership(goods.getGoodsId());

        // 编辑内容校验：编辑弹窗为整表提交，必填项缺失会导致动态 SQL 组装出空 SET
        if (goods.getGoodsName() == null || goods.getGoodsName().trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }

        if (goods.getCategoryId() == null) {
            throw new RuntimeException("请选择商品分类");
        }

        if (goods.getPrice() == null) {
            throw new RuntimeException("商品价格不能为空");
        }

        if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("商品价格不能小于0");
        }

        // 编辑只允许修改名称/分类/描述/价格，不允许通过编辑改动商品状态
        // 已售商品不能借“编辑”复活，状态流转统一走专门的下架/上架接口
        goods.setStatus(null);

        goodsMapper.update(goods);

    }


    // =========================
    // 删除商品
    // =========================

    public void deleteById(Integer goodsId) {

        // 归属校验：不是自己的商品（且非管理员）直接拒绝
        checkOwnership(goodsId);

        // 已参与交易的商品承载着订单历史，物理删除会破坏 orders.goods_id 的业务关系，
        // 因此直接拒绝删除并提示改用下架
        int orderCount = orderMapper.countByGoodsId(goodsId);

        if (orderCount > 0) {
            throw new RuntimeException("该商品已产生交易记录，不能删除，可将其下架");
        }

        goodsMapper.deleteById(goodsId);

    }


    // =========================
    // 商品状态流转：下架(1→0) / 上架(0→1)
    // =========================

    public void updateStatus(Integer goodsId, Integer targetStatus) {

        Goods goods = checkOwnership(goodsId);

        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new RuntimeException("不支持的商品状态");
        }

        if (targetStatus.equals(goods.getStatus())) {
            throw new RuntimeException(targetStatus == 1 ? "商品已在售" : "商品已下架");
        }

        if (targetStatus == 0) {
            // 下架复用下单并发的条件更新：只有仍在售时才能下架，
            // 商品刚被买走时这里影响 0 行，避免把“已售”状态覆盖成“手动下架”
            int rows = goodsMapper.updateStatusIfOnSale(goodsId, 0);

            if (rows == 0) {
                throw new RuntimeException("商品已下架或已被购买");
            }

            return;
        }

        // 上架前必须确认没有交易记录：已售商品保持“已售”终态，不允许复活；
        // 窗口期内不会有新订单产生（下单要求商品在售，此时状态仍为已下架）
        int orderCount = orderMapper.countByGoodsId(goodsId);

        if (orderCount > 0) {
            throw new RuntimeException("该商品已产生交易记录，不能重新上架");
        }

        goodsMapper.updateStatus(goodsId, 1);

    }


    // =========================
    // 归属校验与角色判断
    // =========================

    /**
     * 校验当前登录用户是否可以操作该商品
     *
     * 商品归属以数据库中的 goods.userId 为准（不是前端传的 userId），
     * 当前登录用户从 Token → AuthContext 获取；管理员保留平台管理能力。
     *
     * @return 查到的商品（供后续业务继续使用）
     */
    private Goods checkOwnership(Integer goodsId) {

        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }

        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            throw new RuntimeException("商品不存在或已被删除");
        }

        if (!userId.equals(goods.getUserId()) && !isAdmin(userId)) {
            throw new RuntimeException("只能操作自己发布的商品");
        }

        return goods;

    }

    private boolean isAdmin(Integer userId) {

        User user = userMapper.findById(userId);

        return user != null && "admin".equals(user.getRole());

    }


    // =========================
    // 实施八：批量新增商品
    // =========================

    @Transactional(rollbackFor = Exception.class)
    public void batchInsert(List<Goods> goodsList) {

        if (goodsList == null || goodsList.isEmpty()) {
            throw new RuntimeException("商品数据不能为空");
        }

        for (int i = 0; i < goodsList.size(); i++) {

            Goods goods = goodsList.get(i);

            int rowNum = i + 1;


            // 发布人一律取当前登录用户，前端传什么 userId 都不生效
            Integer currentUserId = AuthContext.getUserId();

            if (currentUserId == null) {
                throw new RuntimeException("当前用户未登录");
            }

            goods.setUserId(currentUserId);


            // 校验商品名称
            if (goods.getGoodsName() == null
                    || goods.getGoodsName().trim().isEmpty()) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品名称不能为空"
                );
            }


            // 校验价格
            if (goods.getPrice() == null) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品价格不能为空"
                );
            }


            // 价格不能小于0
            if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {

                throw new RuntimeException(
                        "第" + rowNum + "行：商品价格不能小于0"
                );
            }


            // 执行新增
            goodsMapper.insert(goods);

        }

    }


    /*
     * ============================
     * 实施九：统计功能
     * ============================
     */


    // 商品首页统计
    public Map<String, Object> statistics() {

        return goodsMapper.statistics();

    }


    // 各分类平均价格
    public List<Map<String, Object>> categoryAvg() {

        return goodsMapper.categoryAvg();

    }


    // 商品价格区间统计
    public List<Map<String, Object>> priceLevelCount() {

        return goodsMapper.priceLevelCount();

    }

    public List<Goods> findMyGoods() {
        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new RuntimeException("当前用户未登录");
        }

        return goodsMapper.findMyGoods(userId);
    }


    // =========================
    // 管理员：商品分页查询（JOIN 卖家名与分类名）
    // =========================

    public List<Map<String, Object>> findAdminPage(
            String goodsName,
            Integer categoryId,
            Integer status,
            Integer pageNum,
            Integer pageSize) {

        Integer offset = (pageNum - 1) * pageSize;

        return goodsMapper.findAdminPage(goodsName, categoryId, status, offset, pageSize);
    }

    public int countAdmin(
            String goodsName,
            Integer categoryId,
            Integer status) {

        return goodsMapper.countAdmin(goodsName, categoryId, status);
    }

}