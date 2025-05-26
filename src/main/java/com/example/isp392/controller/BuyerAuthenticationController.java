package com.example.isp392.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class BuyerAuthenticationController {

    @RequestMapping(value = "/buyer/login", method = RequestMethod.GET)
    public String buyerLogin(){
        return "buyer/login";
    }


    @RequestMapping(value = "/buyer/signup", method = RequestMethod.GET)
    public String buyerSignup(){
        return "buyer/signup";
    }

    @RequestMapping(value = "/forgot-password", method = RequestMethod.GET)
    public String buyerForgotPassword(){
        return "forgot-password";
    }

    @RequestMapping(value = "/new-password", method = RequestMethod.GET)
    public String buyerProfile(){
        return "new-password";
    }

    @RequestMapping(value = "/otp", method = RequestMethod.GET)
    public String buyerProfilePage(){
        return "otp";
    }
}
