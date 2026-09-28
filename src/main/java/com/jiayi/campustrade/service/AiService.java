package com.jiayi.campustrade.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 校园 AI 助手服务（当前为 Mock 实现）
 *
 * 边界约定：
 * - Controller 只做参数解析，对话能力全部收敛在本类；
 * - 未来接入真实 LLM 时，仅替换本类内部实现（或改为 LLM Service + Tool Calling），
 *   POST /api/ai/chat 的请求/响应契约与前端无需改动。
 *
 * 当前能力：基于关键词的规则问答，回答内容全部来自平台真实业务规则
 * （发布、收藏、下单、面交交付、买家确认收货、校区筛选等），不编造平台没有的功能。
 *
 * 不做的事（留给后续 AI 批次）：
 * 不调用任何外部大模型、不做会话持久化、不查用户私有数据、不做 Tool Calling。
 */
@Service
public class AiService {

    // 默认追问建议：任何回答下都可以继续问的高频问题
    private static final List<String> DEFAULT_SUGGESTIONS = Arrays.asList(
            "怎么发布商品？",
            "交易流程是什么？",
            "怎么收藏商品？"
    );

    /**
     * 处理一轮对话
     *
     * @param message        用户本轮输入（Controller 已保证非空、已 trim、已限长）
     * @param conversationId 前端生成的会话标识；当前不持久化，仅为未来多轮对话预留字段
     * @return { reply: 回答文本, suggestions: 建议追问 }
     */
    public Map<String, Object> chat(String message, String conversationId) {

        String text = message == null ? "" : message.trim();
        String lower = text.toLowerCase();

        String reply;
        List<String> suggestions;

        // ---------- 寒暄 ----------
        if (isGreeting(text, lower)) {
            reply = "你好，我是校园交易助手。\n你可以问我商品推荐、发布方法、收藏购买、交易流程等问题，也可以试试下面的常见问题。";
            suggestions = Arrays.asList(
                    "推荐适合我的商品",
                    "怎么发布商品？",
                    "交易流程是什么？"
            );
        }
        // ---------- 成为卖家 ----------
        else if (containsAny(text, "成为卖家", "申请卖家", "卖家申请", "卖家认证", "怎么卖东西", "如何卖")) {
            reply = "学生账号可以在左侧菜单「成为卖家」页提交申请，简单填写信息后由管理员审核；"
                    + "审核通过后你的身份会升级为卖家，就可以发布商品了。";
            suggestions = Arrays.asList(
                    "怎么发布商品？",
                    "发布商品需要填写哪些信息？",
                    "交易流程是什么？"
            );
        }
        // ---------- 发布商品 ----------
        else if (containsAny(text, "发布", "上架", "出售", "卖闲置", "挂出", "发布商品")) {
            reply = "登录且成为卖家后，进入「发布商品」页面：\n"
                    + "1. 填写商品名称、分类、价格、成色；\n"
                    + "2. 选择校区和交易地点，补充商品描述；\n"
                    + "3. 上传商品图片后提交即可发布。\n"
                    + "商品发布后会出现在商品中心，其他同学可以按分类、价格和校区筛选到它。";
            suggestions = Arrays.asList(
                    "如何申请成为卖家？",
                    "交易流程是什么？",
                    "可以按校区筛选商品吗？"
            );
        }
        // ---------- 确认收货 ----------
        else if (containsAny(text, "确认收货", "收到货", "收货", "完成订单")) {
            reply = "卖家线下面交交付商品后，订单会进入「待买家确认」状态；"
                    + "你在「交易中心」找到对应订单，点击「确认收货」，订单即变为已完成。"
                    + "注意只有买家本人可以确认收货。";
            suggestions = Arrays.asList(
                    "交易流程是什么？",
                    "卖家什么时候确认已交付？",
                    "怎么购买商品？"
            );
        }
        // ---------- 收藏 ----------
        else if (containsAny(text, "收藏", "想买的", "心愿单")) {
            reply = "登录后进入任意商品详情页，点击「收藏商品」即可保存；"
                    + "收藏后可以慢慢对比挑选，再次点击可以取消收藏。";
            suggestions = Arrays.asList(
                    "推荐适合我的商品",
                    "怎么购买商品？",
                    "交易流程是什么？"
            );
        }
        // ---------- 留言 / 联系卖家 ----------
        else if (containsAny(text, "留言", "联系卖家", "联系", "沟通", "私信", "问卖家")) {
            reply = "在商品详情页底部的留言区可以向卖家询问成色、配件、面交时间等细节，"
                    + "登录后即可发送留言，卖家会在商品页看到并回复。";
            suggestions = Arrays.asList(
                    "怎么购买商品？",
                    "交易流程是什么？",
                    "面交在哪里进行？"
            );
        }
        // ---------- 购买 / 下单 ----------
        else if (containsAny(text, "购买", "下单", "怎么买", "想买", "拍下", "立即购买")) {
            reply = "在商品详情页点击「立即购买」即可下单：\n"
                    + "下单后商品会暂时下架并生成一笔「待交易」订单，防止被其他人重复购买；\n"
                    + "随后与卖家约定时间地点线下面交，卖家确认已交付、你确认收货后交易完成。";
            suggestions = Arrays.asList(
                    "交易流程是什么？",
                    "面交在哪里进行？",
                    "怎么收藏商品？"
            );
        }
        // ---------- 交易流程（含“校园二手交易怎么进行”） ----------
        else if (containsAny(text, "流程", "怎么交易", "如何交易", "面交", "交易步骤", "怎么进行",
                "二手交易", "交付", "确认完成")) {
            reply = "平台采用真实校园面交流程：\n"
                    + "1. 买家下单，订单为「待交易」；\n"
                    + "2. 买卖双方约定时间地点，卖家线下面交并点击「确认已交付」；\n"
                    + "3. 订单变为「待买家确认」，买家收到商品后点击「确认收货」；\n"
                    + "4. 订单完成。\n"
                    + "平台目前不提供在线支付、物流和自动收货，商品与货款在面交时当面交接。";
            suggestions = Arrays.asList(
                    "怎么购买商品？",
                    "怎么发布商品？",
                    "面交在哪里进行？"
            );
        }
        // ---------- 商品推荐 / 找好物 / 附近 ----------
        else if (containsAny(text, "推荐", "好物", "有什么", "找", "淘", "适合", "种草", "附近")) {
            reply = "当前你可以在「商品中心」按商品名称、分类、价格区间、成色和校区组合筛选好物；"
                    + "也可以从首页场景入口快速进入感兴趣的品类，想找同校区的商品直接按校区筛选即可。\n"
                    + "我目前是基础版助手，暂时不能直接读取商品库做个性化推荐；"
                    + "未来接入更强的 AI 能力后，可以直接用一句话帮你搜索并整理商品。";
            suggestions = Arrays.asList(
                    "怎么购买商品？",
                    "可以按校区筛选商品吗？",
                    "怎么收藏商品？"
            );
        }
        // ---------- 校区筛选 ----------
        else if (containsAny(text, "校区", "北区", "南区", "东区", "西区")) {
            reply = "商品中心支持按校区筛选，校区选项来自平台当前真实在售商品的数据，"
                    + "不是固定写死的列表；目前平台有在售商品的校区都会自动出现在筛选里。";
            suggestions = Arrays.asList(
                    "附近有什么好物？",
                    "推荐适合我的商品",
                    "怎么购买商品？"
            );
        }
        // ---------- 兜底 ----------
        else {
            reply = "目前我是校园交易助手的基础版本，可以帮助你了解商品、发布和交易流程，"
                    + "例如怎么发布商品、如何购买、交易流程是怎样的。\n"
                    + "未来会接入更强的 AI 能力，支持直接搜索商品和查询订单。";
            suggestions = new ArrayList<>(DEFAULT_SUGGESTIONS);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply);
        result.put("suggestions", suggestions);
        return result;
    }

    /**
     * 寒暄判断：英文问候精确匹配；中文问候允许后跟 1~3 个语气词/标点
     * （如“你好呀！”），但“你好，请问怎么发布商品”这类带业务意图的长句不算寒暄
     */
    private boolean isGreeting(String text, String lower) {
        if ("hi".equals(lower) || "hello".equals(lower) || "hey".equals(lower)) {
            return true;
        }
        String[] greetings = {"你好", "您好", "嗨", "哈喽", "在吗", "在么"};
        for (String greeting : greetings) {
            if (text.startsWith(greeting) && text.length() <= greeting.length() + 3) {
                return true;
            }
        }
        return false;
    }

    /**
     * 关键词包含判断：任一关键词命中即 true
     */
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
