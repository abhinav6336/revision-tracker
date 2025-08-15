package com.abhinav.demo.controller;

import org.springframework.web.bind.annotation.RestController;

import com.abhinav.demo.model.Revision;
import com.abhinav.demo.service.revisiontrackerservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@CrossOrigin
public class homecontroller {
    @Autowired
    revisiontrackerservice service;
    @RequestMapping("/")
    public String greet(){
        return "Welcome to revision-tracker";
    }
    @RequestMapping("/about")
    public String about(){
        return "hello i am abhinav and this is my first project.";
    }
    @PostMapping("/topics")
    public String addtopics(@RequestBody Revision topic){
        System.out.println(topic);
        service.addtopics(topic);
        return "Topic added successfully!";
    }
    @GetMapping("/topics")
    public List<Revision> gettopics(){
        return service.gettopics();
    }
}
