package com.gdue.community.controller.interceptor;

import com.gdue.community.entity.User;
import com.gdue.community.service.MessageService;
import com.gdue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder  hostHolder;

    @Autowired
    private MessageService  messageService;

    //统计登录用户的未读消息有多少
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user    =   hostHolder.getUser();
        if (user!=null&&modelAndView!=null){
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(),null);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);

            modelAndView.addObject("allUnreadCOunt",letterUnreadCount   +   noticeUnreadCount);
        }
    }
}
