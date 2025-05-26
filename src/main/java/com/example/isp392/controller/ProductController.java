package com.example.isp392.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class ProductController {

    @RequestMapping(value = "/all-category", method = RequestMethod.GET)
    public String listProduct(){
        return "all-category";
    }

    @RequestMapping(value = "/product-detail", method = RequestMethod.GET)
    public String detailProduct() {
        return "product-detail";
    }
}
