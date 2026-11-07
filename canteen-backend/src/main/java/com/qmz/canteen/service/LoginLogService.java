package com.qmz.canteen.service;

import com.qmz.canteen.entity.LoginLog;
import com.qmz.canteen.entity.User;

public interface LoginLogService {

    void record(String username, User user, String ip, boolean success);
}
