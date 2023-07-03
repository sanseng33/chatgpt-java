package com.unfbx.chatgpt.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unfbx.chatgpt.aop.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;

/**
 * 专利原始字段检索请求
 *
 * @author chengshuo
 * @date 2022/12/15
 */
@Data
public class PatentOriginalRequest {

    /**
     * 专利id集合
     */
    @Description(value = "专利的id的集合", type = "array", name = "pids", arrayType = "string")
    private List<String> patentIds;

    /**
     * 字段集合
     */
    @Description(value = "获取专利信息，需要用到的专利字段的集合，比如获取摘要(ABST),获取标题(TITLE)", type = "array", name = "fl", arrayType = "string")
    private Set<String> fields;

    /**
     * 限定语言
     */
    @Description(value = "语言场景，比如用户设置的语言(UserSettingLang),网站语言(SiteLang)", type = "string", name = "lang_type", enumValues = {"UserSettingLang", "SiteLang"})
    private LangSearchType langType;

    /**
     * 字段搜索场景
     */
    @Description(value = "使用场景，默认为CommonSearch，专利对比搜索场景为CompareSearch", type = "string", name = "search_type", enumValues = {"CommonSearch", "CompareSearch"})
    private PatentSearchFieldType searchFieldType;

}
