package com.gdue.community.controller;

import com.gdue.community.entity.Event;
import com.gdue.community.event.EventProducer;
import com.gdue.community.service.CommentService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.HostHolder;
import com.gdue.community.util.RedisKeyUtil;
import com.gdue.community.entity.Comment;
import com.gdue.community.entity.DiscussPost;
import com.gdue.community.service.DiscussPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private CommentService commentService;

    @Autowired
    private DiscussPostService  discussPostService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate   redisTemplate;

    //添加评论
    @PostMapping("/add/{discussPostId}")
    public String   addComment(@PathVariable("discussPostId") int  discussPostId, Comment  comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件
        Event event=new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);

        if (comment.getEntityType()==ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }else if (comment.getEntityType()==ENTITY_TYPE_COMMENT){
            Comment target  = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        //调用生产者
        eventProducer.fireEvent(event);

        if (comment.getEntityType()==ENTITY_TYPE_POST){
            //触发发帖事件
            event=new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getUserId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);

            //计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }

        return "redirect:/discuss/detail/"   +   discussPostId;
    }
}
