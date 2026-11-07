package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.NoticeRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.NoticeService;
import com.qmz.canteen.vo.NoticeVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public Result<List<NoticeVO>> listPublished() {
        return Result.success(noticeService.listPublished());
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid NoticeRequest request) {
        UserContext.requireAdmin();
        return Result.success(noticeService.createNotice(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @Positive(message = "公告ID不合法") Long id,
                               @RequestBody @Valid NoticeRequest request) {
        UserContext.requireAdmin();
        noticeService.updateNotice(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "公告ID不合法") Long id) {
        UserContext.requireAdmin();
        noticeService.deleteNotice(id);
        return Result.success();
    }
}
