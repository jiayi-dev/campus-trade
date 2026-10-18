package com.jiayi.campustrade.service;

import com.jiayi.campustrade.entity.Goods;
import com.jiayi.campustrade.entity.User;
import com.jiayi.campustrade.exception.BusinessException;
import com.jiayi.campustrade.mapper.GoodsImageMapper;
import com.jiayi.campustrade.mapper.GoodsMapper;
import com.jiayi.campustrade.mapper.OrderMapper;
import com.jiayi.campustrade.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jiayi.campustrade.auth.AuthContext;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private GoodsImageMapper goodsImageMapper;


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
            Integer sellerId,
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
                sellerId,
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
            String campus,
            Integer sellerId) {

        return goodsMapper.count(
                goodsName,
                categoryId,
                minPrice,
                maxPrice,
                conditionLevel,
                campus,
                sellerId
        );
    }


    // =========================
    // 商品详情
    // =========================

    public Goods findById(Integer goodsId) {

        Goods goods = goodsMapper.findById(goodsId);

        // "已售出"不是独立 status 值：下架(status=0) 且存在关联订单即为售出，
        // 详情页据此区分展示「已售出 / 已下架」两种真实业务状态
        if (goods != null) {
            goods.setSold(goods.getStatus() != null
                    && goods.getStatus() != 1
                    && orderMapper.countByGoodsId(goodsId) > 0);
        }

        return goods;

    }


    // =========================
    // 浏览量 +1（进入商品详情页时调用）
    // =========================

    /**
     * 记录一次商品浏览，返回自增后的最新浏览量
     *
     * 业务规则：
     * 1. 商品不存在（已删除）时不加，返回 -1 由前端忽略
     * 2. 卖家本人浏览不计入：自己反复查看/编辑自己的商品不该虚增热度
     * 3. 不引入浏览去重体系：每次成功进入详情页计一次（当前项目无浏览记录表，
     *    简单方案更贴合课程项目阶段，避免为"完美去重"引入 Redis 等重设施）
     */
    public int increaseView(Integer goodsId) {

        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            return -1;
        }

        Integer userId = AuthContext.getUserId();

        if (userId != null && userId.equals(goods.getUserId())) {
            // 本人浏览不计，直接返回当前值（用于前端回显）
            return goods.getViewCount() == null ? 0 : goods.getViewCount();
        }

        goodsMapper.incrementViewCount(goodsId);

        int current = goods.getViewCount() == null ? 0 : goods.getViewCount();

        return current + 1;

    }


    // =========================
    // 商品图片：保存/替换/查询
    // =========================

    /**
     * 全量替换商品图片（发布后首次保存与编辑商品保存共用同一个方法）
     *
     * 业务规则：
     * 1. 归属校验：只有发布者本人（或管理员）能维护商品图片
     * 2. 先删后插：天然幂等，发布/编辑重复提交不会产生重复图片记录
     * 3. 第一张自动设为主图（is_primary=1），列表页封面与详情页首图都取它
     * 4. 最多 9 张，超出部分截断（与前端上传限制保持一致）
     */
    @Transactional(rollbackFor = Exception.class)
    public void replaceImages(Integer goodsId, List<String> imageUrls) {

        checkOwnership(goodsId);

        List<String> urls = imageUrls == null
                ? Collections.emptyList()
                : imageUrls;

        // 去空串、去首尾空格、去重，限制数量
        List<String> cleanUrls = urls.stream()
                .filter(url -> url != null && !url.trim().isEmpty())
                .map(String::trim)
                .distinct()
                .limit(9)
                .collect(Collectors.toList());

        goodsImageMapper.deleteByGoodsId(goodsId);

        for (int i = 0; i < cleanUrls.size(); i++) {
            goodsImageMapper.insert(goodsId, cleanUrls.get(i), i, i == 0 ? 1 : 0);
        }

    }


    // 查询商品全部图片 URL（主图在前），详情页多图展示用
    public List<String> findImageUrls(Integer goodsId) {

        return goodsImageMapper.findUrlsByGoodsId(goodsId);

    }


    // =========================
    // 新增商品
    // =========================

    public void insert(Goods goods) {

        // 发布人一律取当前登录用户，前端传什么 userId 都不生效
        Integer userId = AuthContext.getUserId();

        if (userId == null) {
            throw new BusinessException("当前用户未登录");
        }

        // 发布权限：卖家可发布；管理员保留平台管理兜底；
        // 学生需先走「申请成为卖家 → 管理员审核」流程（与前端 /publish 引导页一致）
        User publisher = userMapper.findById(userId);

        boolean canPublish = publisher != null
                && ("seller".equals(publisher.getRole())
                    || "admin".equals(publisher.getRole()));

        if (!canPublish) {
            throw new BusinessException("成为卖家后即可发布商品，请先完成卖家申请");
        }

        // 必填项与合法性校验（与 update 对齐，防止绕过前端直接提交脏数据）
        if (goods.getGoodsName() == null || goods.getGoodsName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }

        if (goods.getCategoryId() == null) {
            throw new BusinessException("请选择商品分类");
        }

        if (goods.getPrice() == null) {
            throw new BusinessException("商品价格不能为空");
        }

        if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("商品价格不能小于0");
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
            throw new BusinessException("缺少商品ID");
        }

        // 归属校验：不是自己的商品（且非管理员）直接拒绝
        checkOwnership(goods.getGoodsId());

        // 编辑内容校验：编辑弹窗为整表提交，必填项缺失会导致动态 SQL 组装出空 SET
        if (goods.getGoodsName() == null || goods.getGoodsName().trim().isEmpty()) {
            throw new BusinessException("商品名称不能为空");
        }

        if (goods.getCategoryId() == null) {
            throw new BusinessException("请选择商品分类");
        }

        if (goods.getPrice() == null) {
            throw new BusinessException("商品价格不能为空");
        }

        if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("商品价格不能小于0");
        }

        // 编辑只允许修改名称/分类/描述/价格，不允许通过编辑改动商品状态
        // 已售商品不能借“编辑”复活，状态流转统一走专门的下架/上架接口
        goods.setStatus(null);

        goodsMapper.update(goods);

    }


    // =========================
    // 删除商品
    // =========================

    /**
     * 删除商品：goods 主记录与 goods_image 图片记录同一事务内删除，
     * 避免商品删除后图片记录变成孤儿数据（物理图片文件保留，不影响磁盘安全）
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer goodsId) {

        // 归属校验：不是自己的商品（且非管理员）直接拒绝
        checkOwnership(goodsId);

        // 已参与交易的商品承载着订单历史，物理删除会破坏 orders.goods_id 的业务关系，
        // 因此直接拒绝删除并提示改用下架
        int orderCount = orderMapper.countByGoodsId(goodsId);

        if (orderCount > 0) {
            throw new BusinessException("该商品已产生交易记录，不能删除，可将其下架");
        }

        goodsMapper.deleteById(goodsId);

        // 同步清理图片记录
        goodsImageMapper.deleteByGoodsId(goodsId);

    }


    // =========================
    // 商品状态流转：下架(1→0) / 上架(0→1)
    // =========================

    public void updateStatus(Integer goodsId, Integer targetStatus) {

        Goods goods = checkOwnership(goodsId);

        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new BusinessException("不支持的商品状态");
        }

        if (targetStatus.equals(goods.getStatus())) {
            throw new BusinessException(targetStatus == 1 ? "商品已在售" : "商品已下架");
        }

        if (targetStatus == 0) {
            // 下架复用下单并发的条件更新：只有仍在售时才能下架，
            // 商品刚被买走时这里影响 0 行，避免把“已售”状态覆盖成“手动下架”
            int rows = goodsMapper.updateStatusIfOnSale(goodsId, 0);

            if (rows == 0) {
                throw new BusinessException("商品已下架或已被购买");
            }

            return;
        }

        // 上架前必须确认没有交易记录：已售商品保持“已售”终态，不允许复活；
        // 窗口期内不会有新订单产生（下单要求商品在售，此时状态仍为已下架）
        int orderCount = orderMapper.countByGoodsId(goodsId);

        if (orderCount > 0) {
            throw new BusinessException("该商品已产生交易记录，不能重新上架");
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
            throw new BusinessException("当前用户未登录");
        }

        Goods goods = goodsMapper.findById(goodsId);

        if (goods == null) {
            throw new BusinessException("商品不存在或已被删除");
        }

        if (!userId.equals(goods.getUserId()) && !isAdmin(userId)) {
            throw new BusinessException("只能操作自己发布的商品");
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
            throw new BusinessException("商品数据不能为空");
        }

        for (int i = 0; i < goodsList.size(); i++) {

            Goods goods = goodsList.get(i);

            int rowNum = i + 1;


            // 发布人一律取当前登录用户，前端传什么 userId 都不生效
            Integer currentUserId = AuthContext.getUserId();

            if (currentUserId == null) {
                throw new BusinessException("当前用户未登录");
            }

            goods.setUserId(currentUserId);


            // 校验商品名称
            if (goods.getGoodsName() == null
                    || goods.getGoodsName().trim().isEmpty()) {

                throw new BusinessException(
                        "第" + rowNum + "行：商品名称不能为空"
                );
            }


            // 校验价格
            if (goods.getPrice() == null) {

                throw new BusinessException(
                        "第" + rowNum + "行：商品价格不能为空"
                );
            }


            // 价格不能小于0
            if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {

                throw new BusinessException(
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
            throw new BusinessException("当前用户未登录");
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