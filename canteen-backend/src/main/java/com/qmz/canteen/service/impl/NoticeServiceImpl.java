package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.NoticeRequest;
import com.qmz.canteen.entity.Notice;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.NoticeMapper;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.service.NoticeService;
import com.qmz.canteen.vo.NoticeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

    private final NoticeMapper noticeMapper;
    private final UserMapper userMapper;

    @Override
    public List<NoticeVO> listPublished() {
        List<Notice> notices = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getCreateTime)
                .last("LIMIT 10"));
        return toVOList(notices);
    }

    public PageResult<NoticeVO> pageAll(Long current, Long size) {
        IPage<Notice> page = noticeMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Notice>().orderByDesc(Notice::getCreateTime));
        return PageResult.of(page, this::toVO);
    }

    @Override
    public Long createNotice(NoticeRequest request, Long publisherId) {
        Notice notice = new Notice();
        BeanUtils.copyProperties(request, notice);
        notice.setPublisherId(publisherId);
        notice.setStatus(notice.getStatus() == null ? 1 : notice.getStatus());
        noticeMapper.insert(notice);
        return notice.getId();
    }

    @Override
    public void updateNotice(Long id, NoticeRequest request) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "公告不存在");
        }
        BeanUtils.copyProperties(request, notice);
        noticeMapper.updateById(notice);
    }

    @Override
    public void deleteNotice(Long id) {
        Notice notice = noticeMapper.selectById(id);
        if (notice == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "公告不存在");
        }
        noticeMapper.deleteById(id);
    }

    private List<NoticeVO> toVOList(List<Notice> notices) {
        if (notices == null || notices.isEmpty()) {
            return List.of();
        }
        Map<Long, String> names = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getRealName)
                        .in(User::getId, notices.stream().map(Notice::getPublisherId).toList()))
                .stream()
                .collect(Collectors.toMap(User::getId, User::getRealName, (a, b) -> a));

        return notices.stream().map(notice -> {
            NoticeVO vo = new NoticeVO();
            BeanUtils.copyProperties(notice, vo);
            vo.setPublisherName(names.getOrDefault(notice.getPublisherId(), "系统"));
            return vo;
        }).toList();
    }

    private NoticeVO toVO(Notice notice) {
        NoticeVO vo = new NoticeVO();
        BeanUtils.copyProperties(notice, vo);
        return vo;
    }
}
