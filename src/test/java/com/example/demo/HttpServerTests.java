package com.example.demo;

import com.example.demo.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServerTests {

    @Test
    void testInvokeGreetingServiceWithParam() throws Exception {
        // prepare: ensure services are loaded
        HttpServer.loadServices(new String[]{"com.example.demo.examples.GreetingController"});
        URI uri = new URI("/app/greeting?name=TestUser");
        String response = HttpServer.invokeService(uri);
        assertTrue(response.contains("Hola TestUser"));
    }

    @Test
    void testDefaultResponseServesIndex() {
        String resp = HttpServer.defaultResponse();
        assertNotNull(resp);
        assertTrue(resp.toLowerCase().contains("<!doctype html") || resp.contains("<html"));
        // ensure index text present
        assertTrue(resp.contains("Servidor Web en Java") || resp.toLowerCase().contains("form with get"));
    }
}
