package com.example.demo.controller;

import com.example.demo.annotations.MyCustomAnnotation;
import com.example.enums.ClassScope;
import com.example.myspringannotations.PostMapping;
import com.example.myspringannotations.PutMapping;
import com.example.myspringannotations.RequestBody;
import com.example.myspringannotations.RequestMapping;
import com.example.myspringannotations.GetMapping;
import com.example.myspringannotations.RequestParam;
import com.example.myspringannotations.Scope;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@MyCustomAnnotation()//value = "new value")
@RequestMapping(url = "/v1/users")
//@Scope(ClassScope.REQUEST)
public class TestController {

    @GetMapping(path = "/get")
    public String anotherGetRequest() {

        return "Regular mapping is active!";
    }

    @PutMapping(path = "/put")
    public void requestWithParams(@RequestParam(value = "userId") String userId, @RequestParam(value = "userId2") String userId2) {
        System.out.println("Save userId: " + userId);
        System.out.println("Save userId2: " + userId2);
    }

    @PostMapping(path = "/post")
    public List<String> getRequest(@RequestParam("intParam") int myInt,
                                   @RequestParam("testParam") String myString,
                                   @RequestBody() Map<String, String> myDouble) {
        System.out.println("Param 1: " + myInt);
        System.out.println("Param 2: " + myString);
        System.out.println("Param 3: " + myDouble);

        return myDouble.entrySet().stream().map(element -> element.getValue() + ":processed").collect(Collectors.toList());
    }
}
