package com.example.util;

import java.util.HashMap;

public class Constants {
    public final static HashMap<String, String> HTTP_REQUEST_TYPES;

    public final static String GET_MAPPING = "GetMapping";
    public final static String POST_MAPPING = "PostMapping";
    public final static String PUT_MAPPING = "PutMapping";

    static {
        HTTP_REQUEST_TYPES = new HashMap<>();
        HTTP_REQUEST_TYPES.put(GET_MAPPING, "GET");
        HTTP_REQUEST_TYPES.put(POST_MAPPING, "POST");
        HTTP_REQUEST_TYPES.put(PUT_MAPPING, "PUT");
    }

    // http request constants
    public final static String CONTENT_TYPE = "Content-Type";
    public final static String APP_OCTET_STREAM = "application/octet-stream";
    public final static String REQUEST_BODY = "RequestBody";
    public final static String REQUEST_PARAM = "RequestParam";

    // regular constants
    public final static String MAIN = "main";
    public final static String VOID = "void";
    public final static String AND = "&";
    public final static String EQUALS = "=";
    public final static String EMPTY_STRING = "";

}
