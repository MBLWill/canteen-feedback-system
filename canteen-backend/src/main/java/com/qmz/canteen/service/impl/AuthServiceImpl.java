package com.qmz.canteen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qmz.canteen.common.BusinessException;
import com.qmz.canteen.common.ResultCode;
import com.qmz.canteen.dto.LoginRequest;
import com.qmz.canteen.dto.ProfileUpdateRequest;
import com.qmz.canteen.dto.RegisterRequest;
import com.qmz.canteen.entity.DishFavorite;
import com.qmz.canteen.entity.DishReview;
import com.qmz.canteen.entity.DiningOrder;
import com.qmz.canteen.entity.Feedback;
import com.qmz.canteen.entity.User;
import com.qmz.canteen.mapper.DishFavoriteMapper;
import com.qmz.canteen.mapper.DishReviewMapper;
import com.qmz.canteen.mapper.FeedbackMapper;
import com.qmz.canteen.mapper.OrderMapper;
import com.qmz.canteen.mapper.UserMapper;
import com.qmz.canteen.security.JwtUtil;
import com.qmz.canteen.security.LoginUser;
import com.qmz.canteen.security.UserRole;
import com.qmz.canteen.service.AuthService;
import com.qmz.canteen.service.LoginLogService;
import com.qmz.canteen.vo.AccountSummaryVO;
import com.qmz.canteen.vo.LoginVO;
import com.qmz.canteen.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    /** 改密码验证码，按用户存放 */
    private static final java.util.Map<Long, CodeEntry> CODE_STORE = new java.util.concurrent.ConcurrentHashMap<>();

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginLogService loginLogService;
    private final DishReviewMapper dishReviewMapper;
    private final DishFavoriteMapper dishFavoriteMapper;
    private final FeedbackMapper feedbackMapper;
    private final OrderMapper orderMapper;

    @Override
    public LoginVO login(LoginRequest request, String ip) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername()));

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginLogService.record(request.getUsername(), null, ip, false);
            throw new BusinessException("账号或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setRealName(user.getRealName());
        loginUser.setRole(user.getRole());
        loginUser.setStallId(user.getStallId());

        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(loginUser));
        vo.setUserInfo(toUserInfo(user));
        loginLogService.record(request.getUsername(), user, ip, true);
        return vo;
    }

    @Override
    public void register(RegisterRequest request) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.getUsername())) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该账号已存在");
        }
        if (StringUtils.hasText(request.getStudentNo())
                && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, request.getStudentNo())) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该学号已注册");
        }
        if (StringUtils.hasText(request.getPhone())
                && userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getPhone, request.getPhone())) > 0) {
            throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该手机号已被绑定");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setStudentNo(request.getStudentNo());
        user.setPhone(request.getPhone());
        user.setRole(UserRole.STUDENT.name());
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public UserInfoVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return toUserInfo(user);
    }

    @Override
    public AccountSummaryVO getAccountSummary(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        AccountSummaryVO vo = new AccountSummaryVO();
        vo.setBalance(user.getBalance() == null ? BigDecimal.ZERO : user.getBalance());

        List<DiningOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<DiningOrder>()
                .eq(DiningOrder::getUserId, userId));
        long orderCount = orders.size();
        long completedCount = orders.stream()
                .filter(o -> "DONE".equals(o.getStatus()))
                .count();
        BigDecimal totalSpent = orders.stream()
                .filter(o -> "PAID".equals(o.getPayStatus()) && o.getTotalAmount() != null)
                .map(DiningOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        vo.setOrderCount(orderCount);
        vo.setCompletedCount(completedCount);
        vo.setTotalSpent(totalSpent);
        return vo;
    }

    private UserInfoVO toUserInfo(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setRoleLabel(UserRole.valueOf(user.getRole()).getLabel());
        vo.setStallId(user.getStallId());
        vo.setStudentNo(user.getStudentNo());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setBalance(user.getBalance());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        vo.setReviewCount(dishReviewMapper.selectCount(new LambdaQueryWrapper<DishReview>()
                .eq(DishReview::getUserId, user.getId())));
        vo.setFavoriteCount(dishFavoriteMapper.selectCount(new LambdaQueryWrapper<DishFavorite>()
                .eq(DishFavorite::getUserId, user.getId())));
        vo.setFeedbackCount(feedbackMapper.selectCount(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, user.getId())));
        return vo;
    }

    @Override
    public UserInfoVO updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        // 商家与学生一样支持绑定邮箱，用于安全验证与通知
        if (StringUtils.hasText(request.getRealName())) {
            user.setRealName(request.getRealName().trim());
        }
        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (StringUtils.hasText(email)
                    && userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, email).ne(User::getId, userId)) > 0) {
                throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该邮箱已被其他账号绑定");
            }
            user.setEmail(email.isEmpty() ? null : email);
        }
        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (StringUtils.hasText(phone)
                    && userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, phone).ne(User::getId, userId)) > 0) {
                throw new BusinessException(ResultCode.DATA_EXISTS.getCode(), "该手机号已被其他账号绑定");
            }
            user.setPhone(phone.isEmpty() ? null : phone);
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().trim().isEmpty() ? null : request.getAvatar().trim());
        }
        userMapper.updateById(user);
        return getProfile(userId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword, String verifyCode) {
        CodeEntry entry = CODE_STORE.get(userId);
        if (entry == null || entry.expireAt() < System.currentTimeMillis()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码已过期，请重新获取");
        }
        if (!entry.code().equals(verifyCode == null ? "" : verifyCode.trim())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "验证码不正确");
        }
        CODE_STORE.remove(userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "新密码不能与原密码相同");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(update);
    }

    @Override
    public String sendVerifyCode(Long userId, String target) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        // 优先邮箱，其次电话；都没绑定就不能验证
        String bound = StringUtils.hasText(user.getEmail()) ? user.getEmail()
                : StringUtils.hasText(user.getPhone()) ? user.getPhone() : null;
        if (bound == null || target == null || !bound.equals(target.trim())) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "请先绑定邮箱或电话后再验证");
        }
        String code = String.valueOf(100000 + (int) (Math.random() * 900000));
        // 真实短信/邮件服务未接入，验证码直接返回给前端用于演示
        CODE_STORE.put(userId, new CodeEntry(code, System.currentTimeMillis() + 5 * 60 * 1000));
        return code;
    }

    /** 改密码验证码，5 分钟有效 */
    private record CodeEntry(String code, long expireAt) {
    }
}
