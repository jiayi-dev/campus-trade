package com.jiayi.campustrade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 助手配置（B3：OpenAI-Compatible LLM）
 *
 * 读取 application-local.yml 中 llm.* 配置。
 * api-key 为空时 AiService 自动降级为内置关键词规则问答。
 */
@Component
@ConfigurationProperties(prefix = "llm")
public class AiProperties {

    /** OpenAI 兼容接口 Base URL，如 https://api.deepseek.com */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key，从环境变量 LLM_API_KEY 注入，留空则降级 */
    private String apiKey = "";

    /** 模型名称，如 deepseek-chat / gpt-4o-mini */
    private String model = "deepseek-chat";

    /** 单次请求超时（毫秒） */
    private long timeout = 120000L;

    /** 单次最大生成 token 数 */
    private int maxTokens = 1024;

    /** 采样温度 0~1 */
    private double temperature = 0.7;

    /** SseEmitter 超时（毫秒） */
    private long sseTimeout = 180000L;

    /** Embedding 模型（B12 RAG 知识库向量化），DashScope 兼容模式默认 text-embedding-v3（1024 维） */
    private String embeddingModel = "text-embedding-v3";

    /** Embedding 单次请求超时（毫秒），向量接口响应较快，默认 30s */
    private long embeddingTimeout = 30000L;

    /** RAG 知识库检索参数（B12）：top-k 返回块数与相似度命中阈值 */
    private Rag rag = new Rag();

    /**
     * RAG 检索配置，对应 yml：
     * <pre>
     * llm:
     *   rag:
     *     top-k: 3
     *     threshold: 0.55
     * </pre>
     */
    public static class Rag {
        /** 单次检索返回的最大知识块数量 */
        private int topK = 3;
        /** 余弦相似度命中阈值，低于该值视为未命中、不注入提示词 */
        private double threshold = 0.55;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }

        public double getThreshold() { return threshold; }
        public void setThreshold(double threshold) { this.threshold = threshold; }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public long getSseTimeout() { return sseTimeout; }
    public void setSseTimeout(long sseTimeout) { this.sseTimeout = sseTimeout; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public long getEmbeddingTimeout() { return embeddingTimeout; }
    public void setEmbeddingTimeout(long embeddingTimeout) { this.embeddingTimeout = embeddingTimeout; }

    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }
}
