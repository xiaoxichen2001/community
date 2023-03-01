package com.gdue.community.service.impl;

import com.gdue.community.service.DiscussPostService;
import com.gdue.community.util.SensitiveFilter;
import com.gdue.community.dao.DiscussPostMapper;
import com.gdue.community.entity.DiscussPost;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.aspectj.lang.annotation.Pointcut;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

//帖子业务
@Service
public class DiscussPostServiceImpl implements DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    //Caffeine核心接口   Cache  LoadingCache     AsyncLoadingCache

    //帖子列表缓存
    private LoadingCache<String,List<DiscussPost>>  postListCache;

    //帖子总数缓存
    private LoadingCache<Integer,Integer>   postRowsCache;

    @PostConstruct
    public void init(){
        //初始化帖子列表缓存
        postListCache= Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key==null||key.length()==0){
                            throw   new IllegalArgumentException("参数错误");
                        }
                        String[]    params=key.split(":");
                        if (params==null||params.length!=2){
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset= Integer.valueOf(params[0]);
                        int limit= Integer.valueOf(params[1]);

                        //二级缓存  redis   ->  mysql


                        return discussPostMapper.selectDiscussPosts(0,offset,limit,1);
                    }
                });

        //初始化帖子总数缓存
        postRowsCache=Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(@NonNull Integer key) throws Exception {
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }



    //查询帖子
    @Override
    public List<DiscussPost> findDiscussPost(int userId, int offset, int limit,int orderMode) {
        if (userId==0&&orderMode==1){
            //当满足条件时，优先从Caffeine缓存中取数据
            return postListCache.get(offset+":"+limit);
        }

        return discussPostMapper.selectDiscussPosts(userId,offset,limit,orderMode);
    }

    //查询帖子总数
    @Override
    public int findDiscussPostRows(int userId) {

        if (userId==0){
            //当满足条件时，优先从缓存中取数据
            return postRowsCache.get(userId);
        }
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    //添加帖子
    @Override
    public int addDiscussPost(DiscussPost discussPost) {
        if (discussPost==null){
            throw   new IllegalArgumentException("参数不能为空！");
        }

        //转义html标记
        discussPost.setTitle(HtmlUtils.htmlEscape(discussPost.getTitle()));
        discussPost.setContent(HtmlUtils.htmlEscape(discussPost.getContent()));

        //敏感词过滤
        discussPost.setTitle(sensitiveFilter.filter(discussPost.getTitle()));
        discussPost.setContent(sensitiveFilter.filter(discussPost.getContent()));

        return discussPostMapper.insertDiscussPost(discussPost);
    }

    //根据id查询帖子详细信息
    @Override
    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    //更新帖子的评论数量
    @Override
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    @Override
    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    @Override
    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    @Override
    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
