package com.gdue.community.controller.interceptor;

import com.gdue.community.entity.User;
import com.gdue.community.entity.LoginTicket;
import com.gdue.community.service.UserService;
import com.gdue.community.util.CookieUtil;
import com.gdue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

//登录拦截器,判断用户是否登录，用于显示和隐藏某些信息
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder  hostHolder;

    //访问controller前
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String  ticket= CookieUtil.getValue(request,"ticket");
        if (ticket!=null){
            //查询凭证
            LoginTicket loginTicket=userService.findByTiket(ticket);
            //检查凭证是否有效
            if (loginTicket!=null   && loginTicket.getStatus()==0   && loginTicket.getExpired().after(new Date())){
                //根据凭证查询用户
                User user = userService.findById(loginTicket.getUserId());
                //在本次请求中持有用户
                hostHolder.setUser(user);

                //构建用户认证的结果，并存入SecurityContext，以便于Security授权
                Authentication  authentication=new UsernamePasswordAuthenticationToken(user,user.getPassword(),userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    //访问controller后,执行完方法后立即执行模板
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
        User    user=hostHolder.getUser();
        if (user!=null  &&  modelAndView!=null){
            modelAndView.addObject("loginUser",user);
        }
    }

    //访问结束
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
        hostHolder.clear();
//        SecurityContextHolder.clearContext();
    }
}
