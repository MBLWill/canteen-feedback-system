package com.qmz.canteen.controller;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.FaqQuery;
import com.qmz.canteen.dto.FaqRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.FaqService;
import com.qmz.canteen.vo.FaqVO;
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

@Validated
@RestController
@RequestMapping("/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public Result<PageResult<FaqVO>> page(@Valid FaqQuery query) {
        UserContext.requireAdmin();
        return Result.success(faqService.page(query));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid FaqRequest request) {
        UserContext.requireAdmin();
        return Result.success(faqService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @Positive(message = "条目ID不合法") Long id,
                               @RequestBody @Valid FaqRequest request) {
        UserContext.requireAdmin();
        faqService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "条目ID不合法") Long id) {
        UserContext.requireAdmin();
        faqService.delete(id);
        return Result.success();
    }
}
