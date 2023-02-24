package com.gdue.community.controller;

import com.gdue.community.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping
public class DataController {

    @Autowired
    private DataService dataService;

    //跳转统计页面
    @GetMapping("/data")
    @PostMapping("/data")
    public String   getDataPage(){
        return "/site/admin/data";
    }

    //统计网站UV
    @PostMapping("/data/uv")
    public String   getUV(@DateTimeFormat(pattern = "yyyy-MM-dd") Date  start,  //要求网页把时间传过来
                          @DateTimeFormat(pattern = "yyyy-MM-dd")Date  end, Model model){
        long uv = dataService.calculateUV(start, end);
        model.addAttribute("uvResult",uv);
        model.addAttribute("uvStartDate",start);
        model.addAttribute("uvEndDate",end);

//        return "forward:/data"; //跳转到同级别下的请求路径
        return "/site/admin/data";
    }

    //统计活跃用户
    @PostMapping("/data/dau")
    public String   getDAU(@DateTimeFormat(pattern = "yyyy-MM-dd") Date  start,  //要求网页把时间传过来
                          @DateTimeFormat(pattern = "yyyy-MM-dd")Date  end, Model model){
        long uv = dataService.calculateDAU(start, end);
        model.addAttribute("dauResult",uv);
        model.addAttribute("dauStartDate",start);
        model.addAttribute("dauEndDate",end);

//        return "forward:/data"; //跳转到同级别下的请求路径
        return "/site/admin/data";
    }

}
