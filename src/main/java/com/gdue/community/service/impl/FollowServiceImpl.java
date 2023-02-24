package com.gdue.community.service.impl;

import com.gdue.community.entity.User;
import com.gdue.community.service.FollowService;
import com.gdue.community.util.RedisKeyUtil;
import com.gdue.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowServiceImpl implements FollowService, CommunityConstant {

    @Autowired
    private RedisTemplate   redisTemplate;

    @Autowired
    private UserServiceImpl userService;

    //关注某实体
    @Override
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                //用户关注列表加一
                operations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                //关注用户的粉丝加一
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    //取消关注某实体
    @Override
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                operations.multi();

                //用户关注列表减一
                operations.opsForZSet().remove(followeeKey,entityId);
                //关注用户的粉丝减一
                operations.opsForZSet().remove(followerKey,userId);

                return operations.exec();
            }
        });
    }

    //查询某实体关注数量
    @Override
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        //zCard计算集合中元素的数量
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询某实体粉丝数量
    @Override
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询某用户对某实体的关注状态
    @Override
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey,entityId)   !=  null;
    }

    //查询某实体关注列表详情
    @Override
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        //写死了用户
        String followeeKey= RedisKeyUtil.getFolloweeKey(userId,ENTITY_TYPE_USER);
        Set<Integer>  targetIds= redisTemplate.opsForZSet().reverseRange(followeeKey,offset,offset   +   limit   -   1);

        if (targetIds==null){
            return null;
        }

        List<Map<String,Object>>    list    =new ArrayList<>();
        for (Integer    targetId  :   targetIds){
            Map<String,Object>  map =new HashMap<>();
            User user    =   userService.findById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("folloeTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    //查询某实体粉丝列表详情
    @Override
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        //写死了用户
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER,userId);
        Set<Integer>  targetIds= redisTemplate.opsForZSet().reverseRange(followerKey,offset,offset   +   limit   -   1);

        if (targetIds==null){
            return null;
        }

        List<Map<String,Object>>    list    =new ArrayList<>();
        for (Integer    targetId  :   targetIds){
            Map<String,Object>  map =new HashMap<>();
            User    user    =   userService.findById(targetId);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("folloeTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
