package com.qmz.canteen.service;

import com.qmz.canteen.common.PageResult;
import com.qmz.canteen.dto.MerchantTodoRequest;
import com.qmz.canteen.entity.MerchantTodo;
import com.qmz.canteen.entity.Stall;
import com.qmz.canteen.vo.MerchantMessageVO;

import java.util.List;

public interface MerchantService {

    /** 商家待办列表（触发差评预警扫描） */
    List<MerchantTodo> listTodos(Long stallId);

    void doneTodo(Long id, Long stallId);

    /** 管理员发整改敦促 */
    Long createInspectTodo(MerchantTodoRequest request);

    void sendMessage(Long userId, Long stallId, String content);

    List<MerchantMessageVO> listMyMessages(Long stallId);

    PageResult<MerchantMessageVO> pageAllMessages(Long current, Long size);

    void replyMessage(Long id, String reply);

    /** 商家档口证照 */
    Stall getCertificates(Long stallId);
}
