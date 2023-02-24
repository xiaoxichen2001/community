package com.gdue.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.gdue.community.entity.User;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.util.CommunityUtil;
import com.gdue.community.util.HostHolder;
import com.gdue.community.entity.Message;
import com.gdue.community.entity.Page;
import com.gdue.community.service.impl.MessageServiceImpl;
import com.gdue.community.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
@RequestMapping
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageServiceImpl  messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserServiceImpl userService;

    //私信列表
    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page   page){
        User user=hostHolder.getUser();

        //分页信息
        page.setPath("/letter/list");
        page.setLimit(5);
        page.setRows(messageService.findConversationCount(user.getId()));

        //会话列表
        List<Message> conversationsList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>>  conversations=new ArrayList<>();
        if (conversationsList!=null){
            for (Message message:conversationsList) {
                Map<String,Object>  map=new HashMap<>();
                map.put("conversation",message);
                //私信数量
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                //私信未读数量
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                //发送人or目标人
                int targetId= user.getId()==message.getFromId()? message.getToId() : message.getFromId();
                map.put("target",userService.findById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        //查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId() , null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount   =   messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/letter";
    }

    //查询私信详情
    @GetMapping("/letter/detail/{conversationId}")
    public  String  getLetterDetail(@PathVariable("conversationId") String  conversationId,Page page,Model  model){
        //分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        //私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>>    letters=new ArrayList<>();
        if (letters!=null){
            for (Message message:letterList){
                Map<String,Object>  map=new HashMap<>();
                map.put("letter",message);
                map.put("fromUser",userService.findById(message.getFromId()));
                letters.add(map);
            }
        }

        model.addAttribute("letters",letters);

        //私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

        //设置已读
        List<Integer> ids=getLetterIds(letterList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    private User getLetterTarget(String    conversationId){
        String[]    ids=conversationId.split("_");
        int id0=Integer.parseInt(ids[0]);
        int id1=Integer.parseInt(ids[1]);

        if (hostHolder.getUser().getId()==id0){
            return userService.findById(id1);
        }else{
            return userService.findById(id0);
        }
    }

    private List<Integer>   getLetterIds(List<Message>  letterList){
        List<Integer>   ids=new ArrayList<>();

        if (letterList!=null){
            for (Message    message:    letterList){
                if (hostHolder.getUser().getId()== message.getToId()&&message.getStatus()==0){
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    //发送私信
    @PostMapping("/letter/send")
    @ResponseBody
    public String sendMessage(String    toName,String   content){
        User target = userService.findUserByName(toName);
        Integer.valueOf("abc");
        if (target==null){
            throw new IllegalArgumentException("目标用户不存在!");
        }

        Message message=new Message();
        message.setContent(content);
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId()>message.getToId()){
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }else {
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        }
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    //系统通知
    @GetMapping("/notice/list")
    public String   getNoticelist(Model model){

        User    user    =   hostHolder.getUser();

        //查询评论类通知
        Message message =messageService.findLatesNotice(user.getId(),TOPIC_COMMENT);

        if (message!=null){
            Map<String,Object>  messageVO   =   new HashMap<>();
            messageVO.put("message",message);
            //因为存的时候转换了，取出来的时候要转换回去
            String  content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object>  data    = JSONObject.parseObject(content,HashMap.class);

            messageVO.put("user",userService.findById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            int count   =   messageService.findNoticeCount(user.getId(),TOPIC_COMMENT);
            messageVO.put("count",count);

            int unread  =   messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVO.put("unread",unread);
            model.addAttribute("commentNotice",messageVO);
        }


        //查询点赞类通知
         message =messageService.findLatesNotice(user.getId(),TOPIC_LIKE);

         if (message!=null){
             Map<String,Object> messageVO   =   new HashMap<>();
             messageVO.put("message",message);
             //因为存的时候转换了，取出来的时候要转换回去
             String  content = HtmlUtils.htmlUnescape(message.getContent());
             Map<String,Object>  data    = JSONObject.parseObject(content,HashMap.class);

             messageVO.put("user",userService.findById((Integer) data.get("userId")));
             messageVO.put("entityType",data.get("entityType"));
             messageVO.put("entityId",data.get("entityId"));
             messageVO.put("postId",data.get("postId"));

             int count   =   messageService.findNoticeCount(user.getId(),TOPIC_LIKE);
             messageVO.put("count",count);

             int unread  =   messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
             messageVO.put("unread",unread);
             model.addAttribute("likeNotice",messageVO);
         }


        //查询关注类通知
        message =messageService.findLatesNotice(user.getId(),TOPIC_FOLLOW);
        if (message!=null){
            Map<String,Object> messageVO   =   new HashMap<>();
            messageVO.put("message",message);
            //因为存的时候转换了，取出来的时候要转换回去
            String  content = HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object>  data    = JSONObject.parseObject(content,HashMap.class);

            messageVO.put("user",userService.findById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));

            int count   =   messageService.findNoticeCount(user.getId(),TOPIC_FOLLOW);
            messageVO.put("count",count);

            int unread  =   messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVO.put("unread",unread);
            model.addAttribute("followNotice",messageVO);
        }


        //查询未读消息数量
        int letterUnraedCount   =   messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnraedCount);
        int noticeUnreadCount   =   messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/notice";
    }

    //系统通知指定主题通知列表
    @GetMapping("/notice/detail/{topic}")
    public String   getNoticeDetail(@PathVariable("topic") String  topic,Page page,Model model){

        User    user    =   hostHolder.getUser();

        //设置分页
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));

        List<Message>   noticeList=messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String,Object>>    noticeVoList    =   new ArrayList<>();
        if (noticeList!=null){
            for (Message notice : noticeList){
                Map<String,Object>  map=new HashMap<>();
                //通知
                map.put("notice",notice);
                //内容
                String  content =   HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object>  data    =   JSONObject.parseObject(content,HashMap.class);

                map.put("user",userService.findById((Integer) data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                //通知作者
                map.put("fromUser",userService.findById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices",noticeVoList);
        //设置已读


        List<Integer>   ids=getLetterIds(noticeList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
            System.out.println("================");
        }
        return "/site/notice-detail";
    }

}
