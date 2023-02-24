package com.gdue.community.controller;

import com.gdue.community.entity.Event;
import com.gdue.community.entity.Page;
import com.gdue.community.entity.User;
import com.gdue.community.event.EventProducer;
import com.gdue.community.service.FollowService;
import com.gdue.community.service.UserService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.CommunityUtil;
import com.gdue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
public class FollowController implements CommunityConstant {

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    @PostMapping("/follow")
    @ResponseBody
    public String   follow(int  entityType,int  entityId){
        User user=hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);

        //触发关注事件
        Event event   =   new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0,"已关注");
    }

    @PostMapping("/unfollow")
    @ResponseBody
    public String   unfollow(int  entityType,int  entityId){
        User    user=hostHolder.getUser();
        followService.unfollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"已取消关注");
    }

    //获取用户关注列表
    @GetMapping("/followees/{userId}")
    public String   getFollowees(@PathVariable("userId") int userId, Page page, Model   model){
        User    user=userService.findById(userId);

        if (user==null){
            throw new RuntimeException("该用户不存在!");
        }

        //获取的用户信息
        model.addAttribute("user",user);

        //设置分页信息
        page.setPath("/followees/"+userId);
        page.setLimit(5);
        page.setRows((int)followService.findFolloweeCount(userId,ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        if (userList == null) {
            for (Map<String,Object> map:userList){
                User    u   = (User) map.get("user");
                //存到map中即可，因为map是在userList中循环取得，存到mao中即存到userList中
                map.put("hasFollowed",hasFollow(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/followee";
    }

    //获取用户粉丝列表
    @GetMapping("/followers/{userId}")
    public String   getFollowers(@PathVariable("userId") int userId, Page page, Model   model){
        User    user=userService.findById(userId);

        if (user==null){
            throw new RuntimeException("该用户不存在!");
        }

        //获取的用户信息
        model.addAttribute("user",user);

        //设置分页信息
        page.setPath("/followers/"+userId);
        page.setLimit(5);
        page.setRows((int)followService.findFollowerCount(userId,ENTITY_TYPE_USER));

        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        if (userList == null) {
            for (Map<String,Object> map:userList){
                User    u   = (User) map.get("user");
                //存到map中即可，因为map是在userList中循环取得，存到mao中即存到userList中
                map.put("hasFollowed",hasFollow(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/follower";
    }

    private boolean hasFollow(int   userId){
        if (hostHolder.getUser()==null){
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
    }
}
