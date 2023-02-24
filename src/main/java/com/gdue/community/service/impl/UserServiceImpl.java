package com.gdue.community.service.impl;

//import com.nowcoder.community.dao.LoginTicketMapper;
import com.gdue.community.dao.UserMapper;
import com.gdue.community.entity.LoginTicket;
import com.gdue.community.entity.User;
import com.gdue.community.service.UserService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.CommunityUtil;
import com.gdue.community.util.MailClient;
import com.gdue.community.util.RedisKeyUtil;
import com.nowcoder.community.util.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

//用户业务
@Service
public class UserServiceImpl implements UserService, CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Value("${community.path.domain}")
    private String  domain;

    @Value("${server.servlet.context-path}")
    private String  contextPath;

    @Autowired
    private TemplateEngine  templateEngine;

    @Autowired
    private MailClient mailClient;



//    @Autowired
//    private LoginTicketMapper   loginTicketMapper;

    @Autowired
    private RedisTemplate   redisTemplate;

    @Override
    public User findById(int id) {
//        return userMapper.selectById(id);
        User    user=getCache(id);
        if (user==null){
            user= initCache(id);
        }
        return user;
    }


    //用户注册业务
    @Override
    public Map<String, Object> register(User user) {

        Map<String,Object>  map =new HashMap<>();

        //验证数据是否空值
        if (user==null){
            throw new RuntimeException("参数不能为空！");
        }

        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        //查询用户是否已存在
        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u!=null){
            map.put("usernameMsg","账号已存在");
            return map;
        }
        //验证邮箱
        u   =  userMapper.selectByEmail(user.getEmail());
        if (u!=null){
            map.put("emailMsg","此邮箱已注册");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));  //用来密码md5加密
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt())); //密码md5加密
        user.setType(0);    //设置用户状态
        user.setActivationCode(CommunityUtil.generateUUID());   //设置用户激活代码
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000))); //设置用户默认头像，从牛客网中随机取
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //激活邮件
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        String  url =  domain   +   contextPath +   "/activation"   + "/"+  user.getId()    +   "/" +   user.getActivationCode();
        context.setVariable("url",url);
        String  content=templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(), "激活账号",content);

        return map;
    }

    //用户激活业务
    public int  activation(int  id,String   code){
        User user = userMapper.selectById(id);
        System.out.println(user.getStatus() +   "-------");
        if (user.getStatus() == 1) {    //重复激活
            return ACTIVATION_REPEAT;
        }else if(user.getActivationCode().equals(code)){    //比对激活码
            userMapper.updateStatus(id,1);
            clearCache(id);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAILURE;
        }
    }

    //登录验证
    @Override
    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String,Object>  map=new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(username);
        if (user==null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        //验证状态
        if (user.getStatus()==0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }
        //验证密码
        password=CommunityUtil.md5(password +   user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis()  +   expiredSeconds*1000));

//        loginTicketMapper.insertLoginTicket(loginTicket);
        //存入redis中
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey,loginTicket);

        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    @Override
    public void logout(String ticket) {
//        loginTicketMapper.updateStatus(ticket,1);
        //从redis中取出loginTicket，然后更改status再存入redis中
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(ticketKey,loginTicket);
    }

    @Override
    public LoginTicket findByTiket(String ticket) {
//        return loginTicketMapper.selectByTicket(ticket);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }


    @Override
    public int updateHeaderById(int userId, String headerUrl) {
        int rows = userMapper.updateHeader(userId, headerUrl);
        clearCache(userId);
        return rows;
    }

    @Override
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }


    //优先从缓存中取值
    private User getCache(int   userId){
        String  redisKey    =RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //取不到时初始化缓存
    private User    initCache(int   userId){
        User    user    =userMapper.selectById(userId);
        String  redisKey    =RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //数据变更时清除缓存数据
    private void clearCache(int userId){
        String  redisKey    =RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

    //获取用户信息，返回用户认证
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User    user=this.findById(userId);

        List<GrantedAuthority>  list=new ArrayList<>();
        list.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                switch (user.getType()){
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOP;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }

}
