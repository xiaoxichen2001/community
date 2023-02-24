package com.gdue.community.service;


import com.gdue.community.entity.DiscussPost;

import java.util.List;

public interface DiscussPostService {

    List<DiscussPost> findDiscussPost(int id,int offset,int limit,int orderMode);

    int findDiscussPostRows(int userId);

    int addDiscussPost(DiscussPost  discussPost);

    DiscussPost findDiscussPostById(int id);

    int updateCommentCount(int  id,int  commentCount);

    int updateType(int id,int type);

    int updateStatus(int id,int status);

    int updateScore(int id,double   score);
}
