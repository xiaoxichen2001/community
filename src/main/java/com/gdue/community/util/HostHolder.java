package com.gdue.community.util;

import com.gdue.community.entity.User;
import org.springframework.stereotype.Component;

//多线程状态下获取相对应的用户信息
@Component
public class HostHolder {
    private ThreadLocal<User>   users=new ThreadLocal<>();

    public  void setUser(User   user){
        users.set(user);
    }

    public User getUser(){
        return users.get();
    }

    public void clear(){
        users.remove();
    }
}
