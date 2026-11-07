package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequest {

    private Long stallId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题不能超过100字")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 1000, message = "内容不能超过1000字")
    private String content;

    /** SUGGEST-建议 COMPLAINT-投诉 PRAISE-表扬 OTHER-其他 */
    @Pattern(regexp = "^(SUGGEST|COMPLAINT|PRAISE|OTHER)?$", message = "留言类型不合法")
    private String type;
}
