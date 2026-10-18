package com.jiayi.campustrade.mapper;

import com.jiayi.campustrade.entity.KbDoc;

import java.util.List;

public interface KbDocMapper {

    // 按标题查询文档（重建索引时按标题 upsert）
    KbDoc findByTitle(String title);

    // 新增文档（返回自增 docId）
    int insert(KbDoc doc);

    // 按主键更新元信息（标题/分类/来源/版本/启用/更新时间），重建索引时使用
    int updateMeta(KbDoc doc);

    // 查询全部启用文档
    List<KbDoc> findAllEnabled();
}
