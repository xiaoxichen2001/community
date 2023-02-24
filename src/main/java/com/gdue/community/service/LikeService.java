package com.gdue.community.service;


public interface LikeService {

    void    like(int    userId,int  entityType,int  entityId,int    entityUserId);

    long    findEntitylikeCount(int entityType,int  entityId);

    int findEntityLikeStatus(int userId,int entityType,int entityId);

    int findUserLikeCount(int   userId);
}
