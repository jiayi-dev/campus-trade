package com.jiayi.campustrade.controller;

import com.jiayi.campustrade.config.AiProperties;
import com.jiayi.campustrade.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理员 · AI 知识库管理（B12 RAG）
 *
 * 挂在 /api/admin/** 下，AuthInterceptor 已统一要求登录且 role=admin：
 * 普通用户/游客/卖家访问会被拦截器直接返回 401/403，不进入本类，不新增任何鉴权代码。
 *
 * 当前仅提供「从种子文档重建知识库」入口，供管理员手动触发；
 * 不做自动定时重建，不提供面向普通用户的知识库写接口。
 */
@RestController
@RequestMapping("/admin/knowledge")
public class AdminKnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private AiProperties aiProperties;

    /**
     * 从 classpath:knowledge/*.md 重建知识库：
     * 解析文档 → upsert kb_doc → 分块 → Embedding 向量化 → 写 kb_chunk → 重载内存索引。
     *
     * Embedding API 失败等业务异常会逃逸到 GlobalExceptionHandler，
     * 返回 HTTP 400 + {code,message,data}，明确告知管理员失败原因，绝不静默成功。
     */
    @PostMapping("/rebuild")
    public Map<String, Object> rebuild() {
        KnowledgeService.RebuildReport report = knowledgeService.rebuildFromSeed();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("docCount", report.docCount);
        result.put("chunkCount", report.chunkCount);
        result.put("embedCount", report.embedCount);
        result.put("elapsedMs", report.elapsedMs);
        result.put("embeddingModel", aiProperties.getEmbeddingModel());
        result.put("indexReady", true);
        result.put("message", "知识库重建完成：" + report.docCount + " 篇文档，"
                + report.chunkCount + " 个知识块已向量化，耗时 " + report.elapsedMs + "ms");
        return result;
    }
}
