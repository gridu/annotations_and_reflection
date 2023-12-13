package com.example.server;

import com.example.myspringboot.MyRequestHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;

public class MyHttpServer {

    private HttpServer server;
    private int port;
    private HashSet<String> existingPaths;

    public MyHttpServer(int port) throws IOException {
        this.port = port;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        existingPaths = new HashSet<>();
    }

    public void startServer() {
        server.start();
    }

    public void stopServer() {
        server.stop(5000);
    }

    public void addContext(MyRequestHandler myHandler, String path) {
        if(server == null) {
            throw new RuntimeException("MyHttpServer is null.");
        } else {
            if(!existingPaths.contains(path)) {
                existingPaths.add(path);
                server.createContext(path, myHandler);
                //System.out.println("----> Adding path: " + path + " to server context.");
                //System.out.println("----> Request type: " + myHandler.getRequestType());
            } else {
                throw new RuntimeException("HTTP Request Path: -->" + path + " is duplicated.");
            }
        }
    }
}
