package com.qmz.canteen.controller;

import com.qmz.canteen.common.Result;
import com.qmz.canteen.dto.MerchantPasswordRequest;
import com.qmz.canteen.dto.StallRequest;
import com.qmz.canteen.security.UserContext;
import com.qmz.canteen.service.StallService;
import com.qmz.canteen.vo.StallVO;
import com.qmz.canteen.vo.StallWindowVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
@RequestMapping("/stalls")
@RequiredArgsConstructor
public class StallController {

    private final StallService stallService;

    @GetMapping
    public Result<List<StallVO>> list(@RequestParam(required = false) @Size(max = 50, message = "关键字过长") String keyword,
                                      @RequestParam(required = false) String area,
                                      @RequestParam(required = false) Integer floor) {
        return Result.success(stallService.listWithStats(keyword, area, floor));
    }

    @GetMapping("/{id}")
    public Result<StallVO> detail(@PathVariable @Positive(message = "档口ID不合法") Long id) {
        return Result.success(stallService.getStallDetail(id));
    }

    /** 某区域楼层的已占用窗口编号（编号选择器用） */
    @GetMapping("/window-nos")
    public Result<List<StallWindowVO>> windowNos(@RequestParam String area,
                                                 @RequestParam Integer floor) {
        UserContext.requireAdmin();
        return Result.success(stallService.listWindowNos(area, floor));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid StallRequest request) {
        UserContext.requireAdmin();
        return Result.success(stallService.createStall(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable @Positive(message = "档口ID不合法") Long id,
                               @RequestBody @Valid StallRequest request) {
        UserContext.requireAdmin();
        stallService.updateStall(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive(message = "档口ID不合法") Long id) {
        UserContext.requireAdmin();
        stallService.deleteStall(id);
        return Result.success();
    }

    /** 管理员重置某档口商家账号密码 */
    @PutMapping("/{id}/merchant-password")
    public Result<Void> resetMerchantPassword(@PathVariable @Positive(message = "档口ID不合法") Long id,
                                              @RequestBody @Valid MerchantPasswordRequest request) {
        UserContext.requireAdmin();
        stallService.resetMerchantPassword(id, request.getPassword());
        return Result.success();
    }
}
