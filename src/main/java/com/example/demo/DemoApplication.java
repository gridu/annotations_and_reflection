package com.example.demo;

import com.example.demo.annotations.MyCustomAnnotation;
import com.example.demo.controller.TestController;
import com.example.myspringannotations.Port;
import com.example.myspringannotations.SpringBootApplication;
import com.example.myspringboot.SpringApplication;

import java.io.IOException;

@SpringBootApplication
@Port(port = 8083)
public class DemoApplication {
	public static void main(String[] args) throws IOException {
		String annotationValue = TestController.class.getAnnotation(MyCustomAnnotation.class).value();
		System.out.println(annotationValue);

		SpringApplication.runApp(DemoApplication.class, args);
	}
}