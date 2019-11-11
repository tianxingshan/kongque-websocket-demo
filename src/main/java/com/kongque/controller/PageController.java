package com.kongque.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class PageController {

    @RequestMapping("customer")
    public String customer() {
        return "customer";
    }

    @RequestMapping("official")
    public String official() {
        return "official";
    }
}
