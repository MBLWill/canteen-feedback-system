package com.qmz.canteen.service.impl;

import com.qmz.canteen.entity.LoginLog;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.LoginLogMapper;
import com.qmz.canteen.service.LoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    @Override
    public void record(String username, User user, String ip, boolean success) {
        LoginLog entity = new LoginLog();
        entity.setUsername(username);
        entity.setRealName(user == null ? null : user.getRealName());
        entity.setRole(user == null ? null : user.getRole());
        entity.setIp(ip);
        entity.setStatus(success ? 1 : 0);
        entity.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(entity);
    }
}
