package com.gdue.community.dao;

import com.gdue.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    /**
     *  根据用户id查询讨论帖子，并加以排序
     * @param userId    用户id
     * @param offset    起始行行号
     * @param limit     每一页显示多少条数据
     * @return
     */
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int  orderMode);

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    //查询帖子总数，便于分页
    int selectDiscussPostRows(@Param("userId") int userId);

    //增加帖子
    int insertDiscussPost(DiscussPost   discussPost);

    //根据用户id查询帖子(帖子详情)
    DiscussPost selectDiscussPostById(int   id);

    //更新用户帖子中评论的数量
    int updateCommentCount(int  id,int  commentCount);

    //更新帖子类型
    int updateType(int id,int type);

    //更新帖子状态
    int updateStatus(int id,int status);

    //更新帖子分数
    int updateScore(int id,double score);
}
