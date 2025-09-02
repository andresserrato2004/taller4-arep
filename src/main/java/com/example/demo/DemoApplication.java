package com.example.demo;

import com.example.demo.httpserver.HttpServer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class DemoApplication {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting MicroSpringBoot");
    // start with empty args to trigger classpath auto-discovery of @RestController
    HttpServer.start(new String[]{});
    }
}
