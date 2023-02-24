package com.gdue.community.ascept;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

//统一记录日志
@Component
@Aspect
public class ServiceLogAscept {

    private static final Logger logger= LoggerFactory.getLogger(ServiceLogAscept.class);

    //com.nowcoder.community.service包下的任意类，任意方法，任意参数    任意返回类型
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){}

    public void before(JoinPoint    joinPoint){

        //获取request
        ServletRequestAttributes    attributes= (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes  ==  null){
            return;
        }

        HttpServletRequest  request=attributes.getRequest();

        String ip = request.getRemoteHost();
        String  now =   new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        //访问了什么位置的什么方法
        String  target=joinPoint.getSignature().getDeclaringTypeName()  +   "." +joinPoint.getSignature().getName();

//        logger.info(String.format("用户[%s],在[%s],访问了[%s]",ip,now,target));
    }
}
