package com.gdue.community.controller;

import com.gdue.community.entity.*;
import com.gdue.community.event.EventProducer;
import com.gdue.community.service.CommentService;
import com.gdue.community.service.DiscussPostService;
import com.gdue.community.service.LikeService;
import com.gdue.community.service.UserService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.CommunityUtil;
import com.gdue.community.util.HostHolder;
import com.gdue.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate   redisTemplate;

    //发布帖子
    @PostMapping("/add")
    @ResponseBody
    public String   addDiscussPost(String   title,String    content){
        User user = hostHolder.getUser();
        if (user==null){
            return CommunityUtil.getJSONString(403,"你还没有登录哦!");
        }

        DiscussPost post=new DiscussPost();
        post.setContent(content);
        post.setTitle(title);
        post.setCreateTime(new Date());
        post.setUserId(user.getId());

        discussPostService.addDiscussPost(post);

            //触发发帖事件
            Event event=new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(user.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(post.getId());
            eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,post.getId());

        return CommunityUtil.getJSONString(0,"发布成功");
    }

    //获取帖子详细信息（帖子信息，评论信息）
    @GetMapping("/detail/{discussPostId}")
    public String   getDiscussPost(@PathVariable("discussPostId") int  discussPostId, Model    model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);

        //作者
        User user = userService.findById(post.getUserId());
        model.addAttribute("user",user);

        //点赞数量
        long likeCount = likeService.findEntitylikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount",likeCount);

        //点赞状态

        int likeStatus  =   hostHolder.getUser()==null? 0   :
                likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("likeStatus",likeStatus);

        //评论分页信息
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());
        page.setLimit(5);

//        System.out.println(page.getLimit());
//        System.out.println(page.getOffset());

        /**评论：给帖子的评论
        //回复：给评论的评论*/
        //评论列表
        List<Comment> commentList = commentService.findCommenyByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论VO列表
        List<Map<String,Object>> commentVOList=new ArrayList<>();
        for (Comment    comment:commentList) {
            Map<String,Object>  commenyVO=new HashMap<>();
            //评论
            commenyVO.put("comment",comment);
            //作者
            commenyVO.put("user",userService.findById(comment.getUserId()));

            //点赞数量
            likeCount = likeService.findEntitylikeCount(ENTITY_TYPE_COMMENT, comment.getId());
            commenyVO.put("likeCount",likeCount);
            //点赞状态
            likeStatus=hostHolder.getUser()==null?  0 :
                    likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,comment.getId());
            commenyVO.put("likeStatus",likeStatus);

            //回复列表
            List<Comment> replyList = commentService.findCommenyByEntity(ENTITY_TYPE_COMMENT, comment.getId(),0,Integer.MAX_VALUE);
            //回复VO列表
            List<Map<String,Object>>    replyVOList=new ArrayList<>();
            for(Comment reply:replyList){
                Map<String,Object>  replyVo=new HashMap<>();
                //回复
                replyVo.put("reply",reply);
                //作者
                replyVo.put("user",userService.findById(reply.getUserId()));

                //点赞数量
                likeCount = likeService.findEntitylikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                replyVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus=hostHolder.getUser()==null?  0 :
                        likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,reply.getId());
                replyVo.put("likeStatus",likeStatus);

                //回复目标
                User    target=reply.getTargetId()==0?null:userService.findById(reply.getTargetId());
                replyVo.put("target",target);

                replyVOList.add(replyVo);
            }
            commenyVO.put("replys",replyVOList);

            //回复数量
            int replyCount = commentService.findCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
            commenyVO.put("replyCount",replyCount);

            commentVOList.add(commenyVO);
        }

        model.addAttribute("comments",commentVOList);

        return "/site/discuss-detail";
    }

    //置顶
    @PostMapping("/top")
    @ResponseBody
    public String   setTop(int  id){
        discussPostService.updateType(id,1);

        //触发发帖事件
        Event   event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

    //加精
    @PostMapping("/wonderful")
    @ResponseBody
    public String   setWonderful(int  id){
        discussPostService.updateStatus(id,1);

        //触发发帖事件
        Event   event=new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子分数
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJSONString(0);
    }

    //删除
    @PostMapping("/delete")
    @ResponseBody
    public String   setDelete(int  id){
        discussPostService.updateStatus(id,2);

        //触发删帖事件
        Event   event=new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }

}
