package com.example.isp392.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class HomepageController {

    @GetMapping("/")
    public String index(){
        return "homepage";
    }

    @RequestMapping(value = "/homepage", method = RequestMethod.GET)
    public String homepage(){
        return "homepage";
    }
}
