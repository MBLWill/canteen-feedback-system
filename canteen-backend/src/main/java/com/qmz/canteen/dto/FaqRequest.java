package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FaqRequest {

    @Size(max = 30, message = "分类不能超过30字")
    private String category;

    @NotBlank(message = "问题不能为空")
    @Size(max = 200, message = "问题不能超过200字")
    private String question;

    @NotBlank(message = "答案不能为空")
    @Size(max = 2000, message = "答案不能超过2000字")
    private String answer;

    @Size(max = 500, message = "关键词不能超过500字")
    private String keywords;

    private Integer sortNo;

    private Integer status;
}
