package com.example.CampusPartnerBackend.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.CampusPartnerBackend.common.ErrorCode;
import com.example.CampusPartnerBackend.constant.UserConstant;
import com.example.CampusPartnerBackend.exception.BusinessException;
import com.example.CampusPartnerBackend.modal.domain.User;
import com.example.CampusPartnerBackend.service.UserService;
import com.example.CampusPartnerBackend.mapper.UserMapper;
import com.example.CampusPartnerBackend.utils.AlgorithmUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.example.CampusPartnerBackend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 13425
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2024-03-06 19:21:19
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    final String SALT = "yupi";


    @Override
    public Long userRegister(String user_account, String userPassword, String checkPassword) {
        //1.账户，密码，校验码为空
        if (StringUtils.isAnyBlank(user_account, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号，密码不能为空");
        }
        // 2.账户小于4位
        if (user_account.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户过短");
        }
        // 3.密码，校验码小于8位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        //星球编号 <= 5位
//        if (userCode.length() > 5) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "星球编号过长");
//        }
        // 4.账户包含特殊字符（正则表达式）
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(user_account);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        // 5.密码和校验码不同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不同");
        }
        // 6.账户重复,放在后面，可以节省查询次数，节省内存性能
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", user_account);
        Long count = userMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
        }
        //校验完成后，加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        //注册成功，插入数据到数据库
        User user = new User();
        user.setUserAccount(user_account);
        user.setUserPassword(encryptPassword);
        boolean result = this.save(user);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "插入失败");
        }
        return user.getId();
    }

    @Override
    public User userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //1.账户，密码，校验码为空
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号，密码不能为空");
        }
        // 2.账户小于4位
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账户过短");
        }
        // 3.密码，校验码小于8位
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        // 4.账户包含特殊字符（正则表达式）
        String validPattern = "[`~!@#$%^&*()+=|{}':;',\\\\[\\\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (matcher.find()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号包含特殊字符");
        }
        //校验完成后，加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 6.账户重复,放在后面，可以节省查询次数，节省内存性能
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        queryWrapper.eq("user_password", encryptPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("userPassword can not match user_account");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        //用户信息脱敏
        User safetyUser = getSafetyUser(user);
        //将用户信息保存到session中
        request.getSession().setAttribute(USER_LOGIN_STATE, safetyUser);
        return safetyUser;
    }

    @Override
    public User getSafetyUser(User user) {
        User safetyUser = new User();
        safetyUser.setId(user.getId());
        safetyUser.setUsername(user.getUsername());
        safetyUser.setUserAccount(user.getUserAccount());
        safetyUser.setAvatarUrl(user.getAvatarUrl());
        safetyUser.setGender(user.getGender());
        safetyUser.setPhone(user.getPhone());
        safetyUser.setEmail(user.getEmail());
        safetyUser.setStatus(user.getStatus());
        safetyUser.setRole(user.getRole());
        safetyUser.setCreateTime(user.getCreateTime());
        safetyUser.setTags(user.getTags());
        return safetyUser;
    }

    @Override
    public int userLogout(HttpServletRequest request) {
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return 1;
    }

    @Override
    public List<User> searchUsersBytags(List<String> tags) {
        if (CollectionUtils.isEmpty(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        //从数据库中进行模糊查询
//        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
//        for(String tag : tags){
//            queryWrapper.like("tags",tag);
//        }
//        List<User> userList = userMapper.selectList(queryWrapper);
//        //脱敏
//        return userList.stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        List<User> userList = userMapper.selectList(queryWrapper);
        Gson gson = new Gson();
//        for (User user : userList) {
        //内存中用户的标签
//            String Usertags = user.getTags();
//            Set<String> tagSet = gson.fromJson(Usertags, new TypeToken<Set<String>>() {
//            }.getType());
        //遍历传入的标签列表，如果有一个标签用户不存在，就不行，全部存在才可以
//            for (String tag : tags) {
//                if (!tagSet.contains(tag)) {
//                    return false;
//                }
//            }
//            return true;
//        }
        //语法糖
        return userList.stream().filter(user -> {
            String Usertags = user.getTags();
            if (StringUtils.isBlank(Usertags)) {
                return false;
            }
            Set<String> tagSet = gson.fromJson(Usertags, new TypeToken<Set<String>>() {
            }.getType());
            tagSet = Optional.ofNullable(tagSet).orElse(new HashSet<>());
            for (String tag : tags) {
                if (!tagSet.contains(tag)) {
                    return false;
                }
            }
            return true;
        }).map(user -> getSafetyUser(user)).collect(Collectors.toList());
//        return userList.stream().map(user -> getSafetyUser(user)).collect(Collectors.toList());
    }

    @Override
    public User getUserById(Long id) {
        if (id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User safetyUser = getSafetyUser(user);
        return safetyUser;
    }

    /**
     * 是否是管理员
     */
    public boolean isAdmin(HttpServletRequest request) {
        Object userobj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User user = (User) userobj;
        if (user == null || user.getRole() != UserConstant.ADMIN_ROLE) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null || loginUser.getRole() != UserConstant.ADMIN_ROLE) {
            return false;
        }
        return true;
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN);
        }
        User safetyUser = getSafetyUser(user);
        return safetyUser;
    }

    @Override
    public int updateUser(User user, User loginUser) {
        Long userId = user.getId();
        if (userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (!isAdmin(loginUser) && !userId.equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH);
        }
        User user1 = userMapper.selectById(userId);
        if (user1 == null) {
            throw new BusinessException(ErrorCode.NULL_ERROR);
        }
        int i = userMapper.updateById(user);
        return i;
    }

    @Override
    public Page<User> selectByRedis(int pageNum, int pageSize, HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        String redisKey = String.format("Campus:partner:%s", loginUser.getId());
        Page<User> userList = (Page<User>) redisTemplate.opsForValue().get(redisKey);

        //缓存中有数据
        if (userList != null) {
            return userList;
        }
        //缓存中无数据
        //从数据库中查
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //pageNum当前请求页码   pageSize每页数据条数
        Page<User> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        //写入缓存
        try {
            redisTemplate.opsForValue().set(redisKey, page, 20, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("set redisKey error", e);
        }
        return page;
    }

    @Override
    public List<User> matchUsers(long num, User loginUser) {
        //先全部查出来，不查tags为空的，只查id与tags两列
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "tags");
        queryWrapper.isNotNull("tags");
        List<User> userList = this.list();
        Gson gson = new Gson();
        String tag = loginUser.getTags();
        List<String> tagList1 = gson.fromJson(tag, new TypeToken<List<String>>() {
        }.getType());
        //答案
        List<Pair<User, Long>> list = new ArrayList<>();
        //反序列化为list<String>
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String tags = user.getTags();
            List<String> tagsList2 = gson.fromJson(tags, new TypeToken<List<String>>() {
            }.getType());
            //依次计算所有用户与当前用户的匹配度（无标签和为当前用户就跳过）
            if (StringUtils.isEmpty(tags) || Objects.equals(user.getId(), loginUser.getId())) {
                continue;
            }
            long distance = AlgorithmUtils.minDistance(tagList1, tagsList2);
            list.add(new Pair<>(user,distance));
        }
        //按编辑距离从小到大排序
        List<Pair<User, Long>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        // 原本顺序的 userId 列表
        List<Long> userIdList = topUserPairList.stream().map(pair -> pair.getKey().getId()).collect(Collectors.toList());
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.in("id",userIdList);//变无序了
        // 1, 3, 2
        // User1、User2、User3
        // 1 => User1, 2 => User2, 3 => User3
        Map<Long, List<User>> listMap = this.list(userQueryWrapper)
                .stream()
                .map(user -> getSafetyUser(user))
                .collect(Collectors.groupingBy(User::getId));
        //最终列表
        List<User> finalUserList = new ArrayList<>();
        for(Long userId:userIdList){
            finalUserList.add(listMap.get(userId).get(0));
        }
        return finalUserList;
    }
}




