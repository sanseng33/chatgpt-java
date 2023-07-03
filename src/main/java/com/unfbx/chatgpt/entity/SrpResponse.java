package com.unfbx.chatgpt.entity;

import com.unfbx.chatgpt.aop.Description;

public class SrpResponse {

    @Description(value = "专利标题", type = "string", name = "TITLE")
    private String title;

    @Description(value = "专利id", type = "string", name = "SOLUTION_ID")
    private String solutionId;

}

