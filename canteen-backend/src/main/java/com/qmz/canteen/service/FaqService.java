package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.FaqQuery;
import com.qmz.canteen.dto.FaqRequest;
import com.qmz.canteen.vo.FaqVO;

import java.util.List;

public interface FaqService {

    PageResult<FaqVO> page(FaqQuery query);

    /** 已启用的知识库条目，供客服匹配使用 */
    List<FaqVO> listEnabled();

    Long create(FaqRequest request);

    void update(Long id, FaqRequest request);

    void delete(Long id);
}
