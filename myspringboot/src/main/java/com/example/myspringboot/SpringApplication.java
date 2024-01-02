package com.example.myspringboot;

import com.example.enums.ClassScope;
import com.example.myspringannotations.RequestMapping;
import com.example.myspringannotations.Port;
import com.example.myspringannotations.GetMapping;
import com.example.myspringannotations.PostMapping;
import com.example.myspringannotations.PutMapping;
import com.example.myspringannotations.Scope;
import com.example.myspringannotations.SpringBootApplication;

import com.example.server.MyHttpServer;
import com.example.util.Constants;
import org.reflections.Reflections;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *  This class searches for classes that contains Mapping methods,
 *  then use them to inject a context listener to the server.
 *
 *  Basically it searches for controllers and then generate a lister
 *  for each http method.
 *
 * @author jgomez
 * @version 1
 * @created   November 30, 2023
 */
public class SpringApplication {

    private static MyHttpServer myHttpServer;

    /**
     * Method that initialize My-SpringBootApplication
     *
     * @param mainClass main class
     * @param args
     * @throws IOException
     */
    public static void runApp(Class<?> mainClass, String[] args) {
        String packagePath = null;
        int port;

        if(!isMainClass(mainClass.getMethods())) {
            throw new RuntimeException("Error while running app, main method not found in class: " + mainClass.getName());
        };

        if (mainClass.isAnnotationPresent(SpringBootApplication.class)) {
            SpringBootApplication mySBApp = mainClass.getAnnotation(SpringBootApplication.class);
            packagePath = mySBApp.startPackage();

            System.out.println("Base package: " + packagePath);
        } else {
            throw new RuntimeException("Missing annotation: @SpringBootApplication");
        }

        if (mainClass.isAnnotationPresent(Port.class)) {
            Port mainAnnotation = mainClass.getAnnotation(Port.class);
            port = mainAnnotation.port();
        } else {
            port = 8080;
        }
        System.out.println("Selected port: " + port + " for current app.");

        // Scan selected package to search controller classes
        Set<Class<?>> clazzes = getClassesWithAnnotation(packagePath, RequestMapping.class);

        // create server
        try {
            myHttpServer = new MyHttpServer(port);
        } catch (IOException e) {
            throw new RuntimeException("Error while creating server: " + e);
        }

        // search for controller classes and add the request handlers to the server.
        extractHttpHandlers(clazzes);

        // start server
        myHttpServer.startServer();
    }

    /**
     * Determine if methods array contains the main method.
     *
     * @param methods list of method.
     *
     * @return boolean - if false means the array do not contain a main method.
     */
    public static boolean isMainClass(Method[] methods) {

        for (Method method: methods) {
            if(method.getName().equals(Constants.MAIN) &&
                    Modifier.isPublic(method.getModifiers()) &&
                    Modifier.isStatic(method.getModifiers()) &&
                    method.getReturnType() ==  void.class &&
                    method.getParameters().length == 1 &&
                    method.getParameterTypes()[0] == String[].class) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extract methods from controllers, and select the ones that contains an annotation to listen for HTTP request,
     * then extract the method type, the path and the method reference itself and the instance of the controller,
     * Finally, use that information to create an HTTP handler object which will be added to the instance of the HttpServer
     *
     * @param clazzes List of classes found in the package specified, these classes might be controllers (that's what we are searching for)
     */
    public static void extractHttpHandlers(Set<Class<?>> clazzes) {
        clazzes.stream().forEach(clazz -> {
            Object instance;

            // create instance of each class found
            try {
                instance = clazz.getDeclaredConstructor().newInstance();
            }  catch (Exception e) {
                throw new RuntimeException(e);
            }

            // get all the request handlers from current class, those handlers will listen HTTP request calls for each controller method
            Set<MyRequestHandler> myHandlers = getMethodsWithAnnotation(clazz, instance);

            if (myHandlers!= null && !myHandlers.isEmpty()) {
                RequestMapping classRequestPath = clazz.getAnnotation(RequestMapping.class);
                String clazzUrl = classRequestPath.url();

                // Extract the corresponding path for each method, in this way our handlers can listen those specific paths
                // and then call the required controller method which needs to handle that request.
                myHandlers.forEach(myHandler -> {
                    String handlerPath = "";

                    if(myHandler.getMethod().getAnnotation(GetMapping.class) != null) {
                        handlerPath = clazzUrl + myHandler.getMethod().getAnnotation(GetMapping.class).path();
                    } else if (myHandler.getMethod().getAnnotation(PostMapping.class) != null) {
                        handlerPath = clazzUrl + myHandler.getMethod().getAnnotation(PostMapping.class).path();
                    } else if (myHandler.getMethod().getAnnotation(PutMapping.class) != null) {
                        handlerPath = clazzUrl + myHandler.getMethod().getAnnotation(PutMapping.class).path();
                    } else {
                        throw new RuntimeException("No path found for current handler: " + myHandler.getMethod());
                    }

                    myHandler.setRequestPath(handlerPath);
                    myHttpServer.addContext(myHandler, handlerPath);
                });
            }
        });
    }

    public static Set<Class<?>> getClassesWithAnnotation(String packageToScan, Class<? extends Annotation> annotation) {
        Reflections reflections = new Reflections(packageToScan);
        return reflections.getTypesAnnotatedWith(annotation);
    }

    /**
     * Iterate class methods and select all the methods that contains GetRequest, PostRequest or PutRequest annotations
     * in this way we can create a request handler for each method, each request handler will be added to a server
     * and then our server will be able to listen HTTP and know exactly which request will call which method inside the
     * current instance.
     * Annotations being searched for:
     * --> @GetRequest
     * --> @PostRequest
     * --> @PutRequest
     *
     * @param clazz current class in which we can search for request methods.
     * @param instance Instance of current Clazz.
     * @return Set that contains a request handler for each HTTP request handler method
     */
    public static Set<MyRequestHandler> getMethodsWithAnnotation(Class<?> clazz, Object instance){
        Set<MyRequestHandler> methodsWithAnnotation = new HashSet<>();

        Arrays.stream(clazz.getMethods()).forEach(method -> {
            Arrays.stream(method.getAnnotations()).forEach(annotation -> {

                String annotationName = annotation.annotationType().getSimpleName();
                // If current annotation is for Post, Put or Get, then create a new request handler
                if(Constants.httpAnnotations.get(annotationName) != null) {
                    //System.out.println("----> Method inside class contains the annotation: " + annotation.toString());
                    String requestType = Constants.httpAnnotationsType.get(annotationName);

                    /* The new request handler will contain the params:
                        --> instance: we can use this class with to call ir manipulate the methods inside this controller.
                        --> method: represent the current http request method found inside the controller class,
                                    we need this method to be able to call it when an HTTP call match this method path + type.
                        --> requestType: this is required to remember with HTTP request type will be handled in this method,
                                         in this way can can validate if someone use it the correct path but not the correct
                                         HTTP request method
                     */
                    methodsWithAnnotation.add(new MyRequestHandler(instance, method, requestType, extractScope(clazz)));
                }

            });
        });

        return methodsWithAnnotation;
    }

    private static String extractScope(Class<?> clazz) {
        Scope scope = clazz.getAnnotation(Scope.class);

        if (scope != null) {
            return scope.value();
        } else {
            return ClassScope.SINGLETON;
        }
    }
}
