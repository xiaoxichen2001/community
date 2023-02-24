package com.gdue.community.controller;

import com.gdue.community.entity.User;
import com.gdue.community.service.LikeService;
import com.gdue.community.util.CommunityConstant;
import com.gdue.community.entity.DiscussPost;
import com.gdue.community.entity.Page;
import com.gdue.community.service.DiscussPostService;
import com.gdue.community.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 网站首页
 */
//@RestController
@Controller
@RequestMapping
public class HomeController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService  discussPostService;

    @Autowired
    private LikeService likeService;

    @GetMapping("/index")
    public /*ModelAndView*/ String getIndexPage(Model  model, Page page,
                                                @RequestParam(name = "orderMode",defaultValue = "0") int orderMode){

//        int discussPostRows = discussPostService.findDiscussPostRows(0);
        //获取总条数
        page.setRows(discussPostService.findDiscussPostRows(0));
        page.setPath("/index");
        //获取帖子相关数据
        List<DiscussPost>   list=discussPostService.findDiscussPost(0, page.getOffset(), page.getLimit(),orderMode);
        //创建一个list集合用于存放discussPost和user数据
        List<Map<String,Object>>    discussPosts=new ArrayList<>();
        if (list!=null){
            for (DiscussPost    post:list) {
                Map<String,Object>  map=new HashMap<>();
                map.put("post",post);
                User user    =   userService.findById(post.getUserId());

                long likeCount = likeService.findEntitylikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);

                map.put("user",user);
                discussPosts.add(map);
            }
        }
        //把查询到的所有数据放到model中
        model.addAttribute("discussPosts",discussPosts);

//        System.out.println(discussPosts);
        //如果想直接return "/index";，就不能使用RestControlller，必须使用Controller
        return "/index";

        //与RestController配合使用
//        ModelAndView modelAndView=new ModelAndView();
//        modelAndView.setViewName("index");
//        return modelAndView;
    }

    @GetMapping("/error")
    public String   getErrorPage(){
        return "/error/500";
    }

    @GetMapping("/denied")
    public String   getDeniedPage(){
        return "/error/404";
    }
}

/**
 * 笔记：
 *   Controller类只返回json类型数据，使用@RestController
 *   Controlelr类只返回thymeleaf.html类型页面，使用@Controller ,方法前只使用@RequestMapping，不使用@ResponseBody
 *   Controlelr返回json也返回html页面时，不使用@RestController，其他注解混合使用
 */
