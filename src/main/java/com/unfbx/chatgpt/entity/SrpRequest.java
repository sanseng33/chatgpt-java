package com.unfbx.chatgpt.entity;

import com.unfbx.chatgpt.aop.Description;

public class SrpRequest {

    @Description(value = "检索语句，比如查询苹果公司的专利，查询语句应该是:ANCS:\"苹果公司\"", type = "string")
    private String q;

    @Description(value = "列表结果每页有几条专利", type = "integer")
    private int limit;

    @Description(value = "默认检索方式：query", type = "string", name = "_type")
    private String type;

    @Description(value = "列表结果的页数", type = "integer")
    private int page;

    @Description(value = "检索类型，比如简单检索语句的为简单检索（SmartSearch），检索语句超过100字符的为语义检索（NoveltySearch）", type = "string", enumValues = {"SmartSearch", "NoveltySearch"})
    private Playbook playbook;

    @Description(value = "获取专利的排序方式，比如按最新申请排序（desc）、按相关度最高排序（sdesc）", type = "string", enumValues = {"sdesc", "desc"})
    private String sort;
}

