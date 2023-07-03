package com.unfbx.chatgpt.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unfbx.chatgpt.aop.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SrpResponse {

    @Description(value = "专利id", type = "string", name = "SOLUTION_ID")
    @JsonProperty("SOLUTION_ID")
    private String solutionId;

//    @Description(value = "专利正文", type = "string", name = "CONTENT")
//    private String content;
//
//    @Description(value = "专利正文的翻译", type = "string", name = "CONTENT_TRAN")
//    private String contentTran;
//
//    @Description(value = "专利技术标题", type = "string", name = "TECHNICAL_TITLE")
//    private String technicalTitle;
//
//    @Description(value = "专利标题", type = "string", name = "TITLE")
//    private String title;
}

