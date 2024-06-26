package com.example.CampusPartnerBackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.CampusPartnerBackend.modal.domain.User;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 13425
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-03-06 19:21:19
 */
public interface UserService extends IService<User> {
    Long userRegister(String user_account, String userPassword, String checkPassword);

    User userLogin(String user_account, String userPassword, HttpServletRequest request);

    User getSafetyUser(User user);

    int userLogout(HttpServletRequest request);

    List<User> searchUsersBytags(List<String> tags);

    User getUserById(Long id);

    boolean isAdmin(HttpServletRequest request);

    boolean isAdmin(User loginUser);

    User getLoginUser(HttpServletRequest request);


    int updateUser(User user ,User loginUser);

    Page<User> selectByRedis(int pageNum, int pageSize, HttpServletRequest request);

    List<User> matchUsers(long num, User loginUser);
}
