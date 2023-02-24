package com.gdue.community.service;


import com.gdue.community.entity.LoginTicket;
import com.gdue.community.entity.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Map;

public interface UserService {
    public User findById(int  id);

    public Map<String,Object>   register(User   user);

    public int  activation(int  userId,String   code);

    public Map<String,Object>   login(String username,String password,long expiredSeconds);

    public void logout(String   ticket);

    public LoginTicket findByTiket(String ticket);

    public int  updateHeaderById(int    userId,String headerUrl);

    User    findUserByName(String   username);

    Collection<? extends GrantedAuthority>  getAuthorities(int  userId);

}
