package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.FaqQuery;
import com.qmz.canteen.dto.FaqRequest;
import com.qmz.canteen.entity.Faq;
import com.qmz.canteen.mapper.FaqMapper;
import com.qmz.canteen.service.ChatService;
import com.qmz.canteen.service.FaqService;
import com.qmz.canteen.vo.FaqVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FaqServiceImpl implements FaqService {

    private final FaqMapper faqMapper;
    /** ChatService 反过来依赖 FaqService，这里用 @Lazy 打破循环依赖 */
    private final ChatService chatService;

    public FaqServiceImpl(FaqMapper faqMapper, @Lazy ChatService chatService) {
        this.faqMapper = faqMapper;
        this.chatService = chatService;
    }

    @Override
    public PageResult<FaqVO> page(FaqQuery query) {
        LambdaQueryWrapper<Faq> wrapper = new LambdaQueryWrapper<Faq>()
                .eq(StringUtils.hasText(query.getCategory()), Faq::getCategory, query.getCategory())
                .and(StringUtils.hasText(query.getKeyword()), w -> w
                        .like(Faq::getQuestion, query.getKeyword())
                        .or()
                        .like(Faq::getAnswer, query.getKeyword()))
                .orderByDesc(Faq::getSortNo)
                .orderByDesc(Faq::getId);
        IPage<Faq> page = faqMapper.selectPage(new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return PageResult.of(page, this::toVO);
    }

    @Override
    public List<FaqVO> listEnabled() {
        List<Faq> list = faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getStatus, 1)
                .orderByDesc(Faq::getSortNo));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public Long create(FaqRequest request) {
        String question = request.getQuestion().trim();
        ensureQuestionAvailable(question, null);
        Faq faq = new Faq();
        BeanUtils.copyProperties(request, faq);
        faq.setQuestion(question);
        faq.setCategory(faq.getCategory() == null ? "OTHER" : faq.getCategory());
        faq.setSortNo(faq.getSortNo() == null ? 0 : faq.getSortNo());
        faq.setStatus(faq.getStatus() == null ? 1 : faq.getStatus());
        faqMapper.insert(faq);
        invalidateChatIndex();
        return faq.getId();
    }

    @Override
    public void update(Long id, FaqRequest request) {
        Faq faq = faqMapper.selectById(id);
        if (faq == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该条目不存在");
        }
        String question = request.getQuestion().trim();
        ensureQuestionAvailable(question, id);
        BeanUtils.copyProperties(request, faq);
        faq.setQuestion(question);
        faqMapper.updateById(faq);
        invalidateChatIndex();
    }

    /**
     * 同问题重复录入会让智能体检索命中两条、答案互相打架，这里拦掉。
     * 比较走数据库（utf8mb4_0900_ai_ci 大小写不敏感），与人工肉眼判断一致。
     */
    private void ensureQuestionAvailable(String question, Long excludeId) {
        long count = faqMapper.selectCount(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getQuestion, question)
                .ne(excludeId != null, Faq::getId, excludeId));
        if (count > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(),
                    "已存在相同问题「" + question + "」，请直接编辑原条目");
        }
    }

    @Override
    public void delete(Long id) {
        Faq faq = faqMapper.selectById(id);
        if (faq == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "该条目不存在");
        }
        faqMapper.deleteById(id);
        invalidateChatIndex();
    }

    /** 知识库变更后让客服检索索引立即失效，避免「改完要等 5 分钟才生效」 */
    private void invalidateChatIndex() {
        if (chatService instanceof ChatServiceImpl impl) {
            impl.invalidateIndex();
        }
    }

    private FaqVO toVO(Faq faq) {
        FaqVO vo = new FaqVO();
        BeanUtils.copyProperties(faq, vo);
        return vo;
    }
}
