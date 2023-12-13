package com.example.util;

import com.example.myspringannotations.GetMapping;
import com.example.myspringannotations.PostMapping;
import com.example.myspringannotations.PutMapping;

import java.lang.annotation.Annotation;
import java.util.HashMap;

public class Constants {
    public final static HashMap<String, Class<? extends Annotation>> httpAnnotations;
    public final static HashMap<String, String> httpAnnotationsType;

    static {
        httpAnnotations = new HashMap<>();
        httpAnnotations.put("GetMapping", GetMapping.class);
        httpAnnotations.put("PostMapping", PostMapping.class);
        httpAnnotations.put("PutMapping", PutMapping.class);

        httpAnnotationsType = new HashMap<>();
        httpAnnotationsType.put("GetMapping", "GET");
        httpAnnotationsType.put("PostMapping", "POST");
        httpAnnotationsType.put("PutMapping", "PUT");
    }

    // http request constants
    public final static String CONTENT_TYPE = "Content-Type";
    public final static String APP_OCTET_STREAM = "application/octet-stream";
    public final static String REQUEST_BODY = "RequestBody";
    public final static String REQUEST_PARAM = "RequestParam";

    // clazz constants
    public final static String MAIN = "main";
    public final static String VOID = "void";
    public final static String AND = "&";
    public final static String EQUALS = "=";
    public final static String EMPTY_STRING = "";

}
