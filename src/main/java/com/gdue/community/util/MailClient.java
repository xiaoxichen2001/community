package com.gdue.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

//发送邮件的工具类
@Component
public class MailClient {

    //用于打印日志文件
    private static final Logger LOGGER  =LoggerFactory.getLogger(MailClient.class);

    @Autowired
    private JavaMailSender  mailSender;

    //发送人
    @Value("${spring.mail.username}")
    private String  from;

    /**
     *
     * @param to        接收方
     * @param subject   标题
     * @param content   内容
     */
    public void sendMail(String to,String subject,String content) {

        try {
            //创建一个存储内容的对象
            MimeMessage message=mailSender.createMimeMessage();
            //操作存储对象的相关数据
            MimeMessageHelper   helper=new MimeMessageHelper(message);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content,true);   //true表示启用模板，可以输入html相关语句

            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            LOGGER.error("发送邮件失败"   +   e.getMessage());
        }

    }




}
