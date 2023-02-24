package com.gdue.community.dao;

import com.gdue.community.entity.LoginTicket;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
@Deprecated
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired}) "
    })
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id,user_id,status,ticket,expired ",
            "from login_ticket ",
            "where  ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String   ticket);

    @Update({
            "<script> ",
            "update login_ticket ",
            "set status=#{status} where ticket=#{ticket} ",
            "</script>"
    })
    int updateStatus(String ticket,int  status);
}
