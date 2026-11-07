package com.qmz.canteen.controller;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.BannerRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.BannerService;
import com.qmz.canteen.vo.BannerVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/banners")
@RequiredArgsConstructor
public class BannerController {

    private final BannerService bannerService;

    @GetMapping("/list")
    public Result<List<BannerVO>> listEnabled() {
        return Result.success(bannerService.listEnabled());
    }

    @GetMapping
    public Result<PageResult<BannerVO>> pageAll(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") Long current,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页条数不能小于1")
            @Max(value = 100, message = "每页条数最多100条") Long size) {
        UserContext.requireAdmin();
        return Result.success(bannerService.pageAll(current, size));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid BannerRequest request) {
        UserContext.requireAdmin();
        return Result.success(bannerService.create(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @Positive(message = "广告ID不合法") Long id,
                               @RequestBody @Valid BannerRequest request) {
        UserContext.requireAdmin();
        bannerService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "广告ID不合法") Long id) {
        UserContext.requireAdmin();
        bannerService.delete(id);
        return Result.success();
    }
}
