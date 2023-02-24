package com.gdue.community.service;

import com.gdue.community.entity.Comment;

import java.util.List;

public interface CommentService {

    List<Comment>   findCommenyByEntity(int entityType,int  entityId,int offset,int limit);

    int findCountByEntity(int entityType,int entityId);

    int addComment(Comment  comment);

    Comment findCommentById(int id);
}
