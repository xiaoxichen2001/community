package com.gdue.community.event;

import com.alibaba.fastjson.JSONObject;
import com.gdue.community.service.ElasticsearchService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.entity.DiscussPost;
import com.gdue.community.entity.Event;
import com.gdue.community.entity.Message;
import com.gdue.community.service.DiscussPostService;
import com.gdue.community.service.MessageService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventComsumer implements CommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(EventComsumer.class);

    @Autowired
    private MessageService  messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @KafkaListener(topics = {TOPIC_FOLLOW,TOPIC_COMMENT,TOPIC_LIKE})
    public void handleCommentMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空！");
            return;
        }

        Event   event   = JSONObject.parseObject(record.value().toString(),Event.class);

        if (event==null){
            logger.error("消息格式错误!");
            return;
        }

        //发送站内消息
        Message message=new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        //其他数据
        Map<String,Object>  content=new HashMap<>();
        content.put("userId",event.getUserId());
        content.put("entityType",event.getEntityType());
        content.put("entityId",event.getEntityId());

        //如果传进来的消息的中data有数据，就把他取出来，装进content中，再把content装进message的内容中
        if (!event.getData().isEmpty()){
            for (Map.Entry<String,Object>   entry   : event.getData().entrySet() ){
                content.put(entry.getKey(),entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(content));

        messageService.addMessage(message);
    }

    //消费发帖事件
    @KafkaListener(topics = TOPIC_PUBLISH)
    public void handlePublishMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event   event=JSONObject.parseObject(record.value().toString(),Event.class);

        if (event==null){
            logger.error("消息格式错误");
        }

        DiscussPost discussPost=discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(discussPost);
    }

    //消费删帖事件
    @KafkaListener(topics = TOPIC_DELETE)
    public void handleDeleteMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        Event   event=JSONObject.parseObject(record.value().toString(),Event.class);

        if (event==null){
            logger.error("消息格式错误");
        }

        elasticsearchService.deleteDiscussPost(event.getEntityId());
    }
}
