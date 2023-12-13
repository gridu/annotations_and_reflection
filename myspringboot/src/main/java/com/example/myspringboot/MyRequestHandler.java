package com.example.myspringboot;

import com.example.enums.ClassScope;
import com.example.myspringannotations.RequestBody;
import com.example.myspringannotations.RequestParam;
import com.example.util.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is used to handle an HTTP request, it contains a reference to the controller class, and one
 * instance of the method that we should call inside this controller.
 *
 * @author jgomez
 * @version 1
 * @created   November 30, 2023
 */
public class MyRequestHandler implements HttpHandler {

    private Object instance;
    private final Method target;
    private String requestType;
    private String requestPath;
    private String scope;

    // default primitive values
    private final byte byteValue = 0;
    private final short shortValue = 0;
    private final int intValue = 0;
    private final long longValue = 0L;
    private final float floatValue = 0.0f;
    private final double doubleValue = 0.0d;
    private final boolean booleanValue = false;
    private final char charValue = '\u0000';

    public MyRequestHandler(Object instance, Method target, String requestType, String scope) {
        this.instance = instance;
        this.target = target;
        this.requestType = requestType;
        this.scope = scope;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if(validateRequest(exchange)) {
            return;
        }

        try {
            Object objectResponse = null;

            Object[] requestParam = handleRequestParams(exchange);

            // if method is not void means, we need to return data to the client
            if(target.getReturnType().getSimpleName() != Constants.VOID) {
                // invoke the controller method and receive the response
                objectResponse = executeRequest(instance, requestParam);
                //objectResponse = target.invoke(instance, requestParam);

                // serialize the response
                String jsonObject = serializeObject(objectResponse);

                // configure the HTTP headers
                exchange.getResponseHeaders().set(Constants.CONTENT_TYPE, Constants.APP_OCTET_STREAM);
                exchange.sendResponseHeaders(200, jsonObject.length());

                // send the serialized object to the client
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(jsonObject.getBytes());
                }
            } else {
                // if method from controller do not contain a response, return a generic message
                //target.invoke(instance, requestParam);
                executeRequest(instance, requestParam);

                // Si el método no devuelve un valor, solo envía una respuesta de éxito
                String response = "Method executed successfully";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        } catch (Exception e) {
            System.out.println(e);
            throw new RuntimeException(e);
        }
    }

    public Object executeRequest(Object instance, Object[] requestParam) throws InvocationTargetException, IllegalAccessException {
        Object responseObject = target.invoke(instance, requestParam);

        if(scope.equals(ClassScope.PROTOTYPE)) {
            try {
                this.instance = instance.getClass().getDeclaredConstructor().newInstance();
                System.out.println(ClassScope.PROTOTYPE + ":Creating new instance for each request");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return responseObject;
    }

    /**
     * Extract request params from request return an object array, each element is a request param
     *
     * @param exchange
     * @return
     */
    private Object[] handleRequestParams(HttpExchange exchange) throws IOException {
        String queryString = exchange.getRequestURI().getQuery();
        Map<String, List<String>> requestPathVariables = this.extractParams(queryString);

        // read the parameters from our target method (the method inside the controller class)
        Parameter[] parameters = this.target.getParameters();
        // now we can create an object that can be used in the target method call
        Object[] parameterValues = new Object[parameters.length];

        int index = 0;
        for (Parameter param: parameters) {
            // verify if current param contains an annotation of the data that should be injected
            RequestParam parameterAnnotation = param.getAnnotation(RequestParam.class);

            if(parameterAnnotation != null) {
                // verify if one of HTTP request params has the same name as current annotation RequestParam(value = "name")
                if(requestPathVariables.containsKey(parameterAnnotation.value())
                    && !requestPathVariables.get(parameterAnnotation.value()).get(0).isEmpty()) {
                    // extract the request param and convert it into input stream
                    InputStream inputParamStream = new ByteArrayInputStream(requestPathVariables.get(parameterAnnotation.value())
                            .get(0).getBytes());
                    // deserialize the param
                    Object requestParam = deserializeObjectRequestObject(inputParamStream, param.getType(), Constants.REQUEST_PARAM);
                    parameterValues[index] = requestParam;
                } else {
                    injectDefaultValue(parameterValues, index, param.getType());
                }

            // Extract request body
            } else if (param.getAnnotation(RequestBody.class) != null) {
                Object requestBody = deserializeObjectRequestObject(exchange.getRequestBody(), param.getType(), Constants.REQUEST_BODY);
                parameterValues[index] = requestBody;
            }  else {
                injectDefaultValue(parameterValues, index, param.getType());
            }
            index++;
        }
        return parameterValues;
    }

    /**
     * When the request param is empty, set the default value to each primitive variable, they cannot be null like a
     * regular Object.
     *
     * @param parameterValues array that contains params.
     * @param index index that is used to save a new param.
     * @param paramType param used to select the default value to primitive variables.
     */
    private void injectDefaultValue(Object[] parameterValues, int index, Class paramType) {

        if(paramType == byte.class) {
            parameterValues[index] = byteValue;
        } else if (paramType == short.class) {
            parameterValues[index] = shortValue;
        } else if (paramType == int.class) {
            parameterValues[index] = intValue;
        } else if (paramType == long.class) {
            parameterValues[index] = longValue;
        } else if (paramType == float.class) {
            parameterValues[index] = floatValue;
        } else if (paramType == double.class) {
            parameterValues[index] = doubleValue;
        } else if (paramType == boolean.class) {
            parameterValues[index] = booleanValue;
        } else if (paramType == char.class) {
            parameterValues[index] = charValue;
        } else if (paramType == String.class) {
            parameterValues[index] = Constants.EMPTY_STRING;
        } else {
            parameterValues[index] = null;
        }
    }

    /**
     * Deserialize an InputStream into a specific class.
     *
     * @param objectStream input stream that represents an object.
     * @return
     */
    private Object deserializeObjectRequestObject(InputStream objectStream, Class myClass, String requestType) {
        Object response = null;

        if(objectStream == null) {
            return null;
        }

        try {
            response = deserializeObject(objectStream, myClass);
        } catch (IOException e) {
            throw new RuntimeException("Error while deserializing " + requestType + " expected type: " + myClass.getTypeName() + " : " + e);
        }

        return response;
    }

    /**
     * Method used to extract params from query request.
     * Example: key1=value1&key2=value2
     *
     * @param queryString
     * @return map that contains all the key:values
     */
    private Map<String, List<String>> extractParams(String queryString) {
        Map<String, List<String>> params = null;
        if(queryString == null || queryString.isBlank()) {
            return params;
        }

        return Arrays.stream(queryString.split(Constants.AND))
                .map(param -> param.split(Constants.EQUALS))
                .collect(Collectors.groupingBy(
                        entry -> entry[0],
                        Collectors.mapping(entry -> entry.length > 1 ? entry[1] : Constants.EMPTY_STRING, Collectors.toList())
                ));
    }

    /**
     * Validate common error and trow exception when request is incorrect.
     *
     * @param exchange object generated for every requests
     * @return boolean - when error detected return true
     * @throws IOException
     */
    private boolean validateRequest(HttpExchange exchange) throws IOException {
        int errorCode = -1;
        String errorResponse = Constants.EMPTY_STRING;

        String currentPath = exchange.getRequestURI().getPath();
        if(!this.requestPath.equals(currentPath)) {
            errorCode = 404;
            errorResponse = "Method not found";
        }
        if(!exchange.getRequestMethod().equals(requestType)) {
            errorCode = 405;
            errorResponse = "Method Not Allowed";
        }

        if (errorResponse != Constants.EMPTY_STRING) {
            exchange.sendResponseHeaders(errorCode, errorResponse.length());

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(errorResponse.getBytes());
            }
            return true;
        }

        return false;
    }

    /**
     * Object serialization, convert the object to JSON and after that, the string can be serialized.
     * @param object generic object to be serialized.
     * @return object in JSON format.
     * @throws IOException
     */
    public static String serializeObject(Object object) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(object);
    }

    /**
     * Object deserialization, convert the JSON into Object.
     * @param inputStream request body as InputStream format
     * @return object in JSON format.
     * @throws IOException
     */
    public static <T> T deserializeObject(InputStream inputStream, Class<T> myClass) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonInput = new String(inputStream.readAllBytes());

        return objectMapper.readValue(jsonInput, myClass);
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public Method getMethod() {
        return target;
    }

    public String getRequestType() {
        return requestType;
    }
}
