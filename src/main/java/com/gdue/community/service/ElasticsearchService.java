package com.gdue.community.service;

import com.gdue.community.entity.DiscussPost;
import com.gdue.community.entity.SearchResult;

import java.io.IOException;

public interface ElasticsearchService {

    void    saveDiscussPost(DiscussPost post);

    void deleteDiscussPost(int  id);

    SearchResult searchDiscussPost(String keyword, int current, int limit)  throws IOException;

}
