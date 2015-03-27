package de.sschleis.showcase.rest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyRestController {

    @RequestMapping("/")
    public String isAlive() {
        return "True";
    }
}
