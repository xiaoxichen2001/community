package com.gdue.community.controller;

import com.gdue.community.entity.User;
import com.gdue.community.service.LikeService;
import com.gdue.community.util.RedisKeyUtil;
import com.gdue.community.entity.Event;
import com.gdue.community.event.EventProducer;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.CommunityUtil;
import com.gdue.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/like")
public class LikeController implements CommunityConstant {

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder  hostHolder;

    @Autowired
    private EventProducer   eventProducer;

    @Autowired
    private RedisTemplate   redisTemplate;

    @PostMapping
    @ResponseBody
    public String   like(int entityType,int entityId,int entityUserId,int   postId){
        User user=hostHolder.getUser();

        //点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);
        //数量
        long    likeCount=likeService.findEntitylikeCount(entityType,entityId);
        //状态
        int likeStatus  =   likeService.findEntityLikeStatus(user.getId(),entityType,entityId);
        //返回结果
        Map<String,Object>  map=new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);

        //触发点赞事件
        if (likeStatus==1){
            Event   event  =  new Event()
                    .setTopic(TOPIC_LIKE)
                    .setEntityType(entityType)
                    .setEntityUserId(entityUserId)
                    .setEntityId(entityId)
                    .setUserId(hostHolder.getUser().getId())
                    .setData("postId",postId);
            eventProducer.fireEvent(event);
        }

        if (entityType==ENTITY_TYPE_POST){
            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }

        return CommunityUtil.getJSONString(0,null,map);
    }
}
