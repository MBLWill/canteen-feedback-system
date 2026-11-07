package com.qmz.canteen.service;

import com.qmz.canteen.dto.NoticeRequest;
import com.qmz.canteen.vo.NoticeVO;

import java.util.List;

public interface NoticeService {

    List<NoticeVO> listPublished();

    Long createNotice(NoticeRequest request, Long publisherId);

    void updateNotice(Long id, NoticeRequest request);

    void deleteNotice(Long id);
}
