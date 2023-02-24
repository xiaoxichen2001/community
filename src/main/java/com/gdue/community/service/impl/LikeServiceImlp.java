package com.gdue.community.service.impl;

import com.gdue.community.service.LikeService;
import com.gdue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

//点赞业务
@Service
public class LikeServiceImlp implements LikeService {

    @Autowired
    private RedisTemplate   redisTemplate;

    //点赞
    @Override
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String  entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
                String  userlikeKey=RedisKeyUtil.getUserLikeKey(entityUserId);

                boolean isMember    =   operations.opsForSet().isMember(entityLikeKey,userId);

                //开启redis事务
                operations.multi();
                if (isMember){  //存在就取消点赞，用户获赞量减一
                    operations.opsForSet().remove(entityLikeKey,userId);
                    operations.opsForValue().decrement(userlikeKey);
                }else { //不存在就点赞，用户获赞量加一
                    operations.opsForSet().add(entityLikeKey,userId);
                    operations.opsForValue().increment(userlikeKey);
                }

                return operations.exec();
            }
        });
    }

    //查询某实体获赞数量
    @Override
    public long findEntitylikeCount(int entityType, int entityId) {
        String  entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询用户对某实体的点赞状态
    @Override
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String  entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey,userId) ? 1 : 0;
    }

    //查找用户获赞数量
    @Override
    public int findUserLikeCount(int userId) {
        String  userlikeKey=RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userlikeKey);
        return count == null? 0 : count.intValue();
    }
}
