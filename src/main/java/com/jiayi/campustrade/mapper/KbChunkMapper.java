package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.KbChunk;

import java.util.List;

public interface KbChunkMapper {

    // 新增知识块（返回自增 chunkId）
    int insert(KbChunk chunk);

    // 查询某文档下全部知识块（按 seq 升序）
    List<KbChunk> findByDocId(Integer docId);

    // 删除某文档下全部知识块（重建索引时先删后插）
    int deleteByDocId(Integer docId);

    /**
     * 加载全部「启用文档 + 已向量化」的知识块（联表带出文档标题），
     * 供 KnowledgeService 构建 JVM 内存检索索引。
     */
    List<KbChunk> findEnabled();
}
