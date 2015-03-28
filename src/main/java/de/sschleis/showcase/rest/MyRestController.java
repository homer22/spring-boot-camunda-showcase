package de.sschleis.showcase.rest;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyRestController {

    @Autowired
    private RuntimeService runtimeService;

    @RequestMapping("/")
    public String isAlive() {
        runtimeService.startProcessInstanceByKey("loanApproval");
        return "True";
    }
}
