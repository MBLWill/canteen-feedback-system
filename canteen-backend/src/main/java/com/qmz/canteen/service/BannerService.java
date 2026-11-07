package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.BannerRequest;
import com.qmz.canteen.vo.BannerVO;

import java.util.List;

public interface BannerService {

    List<BannerVO> listEnabled();

    PageResult<BannerVO> pageAll(Long current, Long size);

    Long create(BannerRequest request);

    void update(Long id, BannerRequest request);

    void delete(Long id);
}
