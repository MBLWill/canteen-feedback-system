package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.BannerRequest;
import com.qmz.canteen.entity.Banner;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.mapper.BannerMapper;
import com.qmz.canteen.mapper.StallMapper;
import com.qmz.canteen.service.BannerService;
import com.qmz.canteen.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;
    private final StallMapper stallMapper;

    @Override
    public List<BannerVO> listEnabled() {
        List<Banner> banners = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortNo)
                .orderByDesc(Banner::getId));
        return attachStallName(banners);
    }

    @Override
    public PageResult<BannerVO> pageAll(Long current, Long size) {
        IPage<Banner> page = bannerMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSortNo)
                        .orderByDesc(Banner::getId));
        Page<BannerVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setPages(page.getPages());
        voPage.setRecords(attachStallName(page.getRecords()));
        return PageResult.of(voPage);
    }

    @Override
    public Long create(BannerRequest request) {
        ensureStallExists(request.getStallId());
        Banner banner = new Banner();
        BeanUtils.copyProperties(request, banner);
        banner.setSortNo(banner.getSortNo() == null ? 99 : banner.getSortNo());
        banner.setStatus(banner.getStatus() == null ? 1 : banner.getStatus());
        bannerMapper.insert(banner);
        return banner.getId();
    }

    @Override
    public void update(Long id, BannerRequest request) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "广告不存在");
        }
        ensureStallExists(request.getStallId());
        BeanUtils.copyProperties(request, banner);
        bannerMapper.updateById(banner);
    }

    /** 关联档口被删后广告会指向不存在的档口，管理端列表只剩一个空名字 */
    private void ensureStallExists(Long stallId) {
        if (stallId != null && stallMapper.selectById(stallId) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "关联档口不存在");
        }
    }

    @Override
    public void delete(Long id) {
        if (bannerMapper.selectById(id) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "广告不存在");
        }
        bannerMapper.deleteById(id);
    }

    private List<BannerVO> attachStallName(List<Banner> banners) {
        if (banners == null || banners.isEmpty()) {
            return List.of();
        }
        List<Long> stallIds = banners.stream().map(Banner::getStallId).filter(id -> id != null).toList();
        Map<Long, Stall> stalls = stallIds.isEmpty()
                ? Map.of()
                : stallMapper.selectList(new LambdaQueryWrapper<Stall>()
                        .select(Stall::getId, Stall::getName)
                        .in(Stall::getId, stallIds))
                .stream()
                .collect(Collectors.toMap(Stall::getId, Function.identity(), (a, b) -> a));

        return banners.stream().map(banner -> {
            BannerVO vo = new BannerVO();
            BeanUtils.copyProperties(banner, vo);
            Stall stall = banner.getStallId() == null ? null : stalls.get(banner.getStallId());
            vo.setStallName(stall == null ? null : stall.getName());
            return vo;
        }).toList();
    }
}
