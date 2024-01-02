package com.example.demo;

import com.example.demo.annotations.MyCustomAnnotation;
import com.example.demo.controller.TestController;
import com.example.myspringannotations.Port;
import com.example.myspringannotations.SpringBootApplication;
import com.example.myspringboot.SpringApplication;

@SpringBootApplication
@Port(port = 8083)
public class DemoApplication {
	public static void main(String[] args) {
		String annotationValue = TestController.class.getAnnotation(MyCustomAnnotation.class).value();
		System.out.println("---> " + annotationValue);

		SpringApplication.runApp(DemoApplication.class, args);
	}
}