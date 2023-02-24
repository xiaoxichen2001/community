package com.gdue.community.controller;


import com.gdue.community.service.LikeService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.entity.DiscussPost;
import com.gdue.community.entity.Page;
import com.gdue.community.entity.SearchResult;
import com.gdue.community.service.ElasticsearchService;
import com.gdue.community.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping
public class ElasticsearchController implements CommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(ElasticsearchController.class);

    @Autowired
    private ElasticsearchService    elasticsearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @GetMapping("/search")
    public String   search(String keyword, Page page, Model model){

        //搜索帖子
        try {

            SearchResult    searchResult=elasticsearchService.searchDiscussPost(keyword,page.getCurrent(), page.getLimit());
            List<Map<String,Object>>  discussPosts=new ArrayList<>();
            List<DiscussPost>   list=searchResult.getList();

            if (list == null) {
                for (DiscussPost    post:list){
                    Map<String,Object>  map=new HashMap<>();
                    //帖子和作者
                    map.put("post",post);
                    map.put("user",userService.findById(post.getUserId()));

                    //点赞数量
                    map.put("likeCount",likeService.findEntitylikeCount(ENTITY_TYPE_POST, post.getId()));

                    discussPosts.add(map);
                }
            }
            model.addAttribute("discussPOsts",discussPosts);
            model.addAttribute("keyword",keyword);

            //分页信息
            page.setPath("'/search?keyword="+keyword);
            page.setRows(searchResult.getTotal()==0?0: (int) searchResult.getTotal());

        }catch (Exception e){
            logger.error("搜索发生异常："+e.getMessage());
        }

        return "/site/search";
    }

}
