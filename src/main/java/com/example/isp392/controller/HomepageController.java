package com.example.isp392.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomepageController {

    @GetMapping("/")
    public String index(){
        return "home";
    }

    @RequestMapping(value = "/home", method = RequestMethod.GET)
    public String homepage(){
        return "home";
    }

    @RequestMapping(value = "/blog", method = RequestMethod.GET)
    public String blog(){
        return "blog";
    }

    @RequestMapping(value = "/blog-single", method = RequestMethod.GET)
    public String blogSingle(){
        return "blog-single";
    }
}
