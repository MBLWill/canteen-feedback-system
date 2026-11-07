package com.qmz.canteen.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReplyRequest {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 1000, message = "回复内容不能超过1000字")
    private String replyContent;

    /** PROCESSING-处理中 RESOLVED-已解决 */
    @Pattern(regexp = "^(PROCESSING|RESOLVED)?$", message = "处理状态不合法")
    private String status;
}
