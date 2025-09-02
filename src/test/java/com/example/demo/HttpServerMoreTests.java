package com.example.demo;

import com.example.demo.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class HttpServerMoreTests {

    @Test
    void testGreetingUsesDefaultWhenNoParam() throws Exception {
        // ensure services loaded
        HttpServer.loadServices(new String[]{"com.example.demo.examples.GreetingController"});
        URI uri = new URI("/app/greeting"); // no query
        String resp = HttpServer.invokeService(uri);
        assertTrue(resp.contains("Hola World"), "Expected default greeting when no name provided");
    }

    @Test
    void testAutoDiscoveryRegistersControllers() throws Exception {
        // clear maps then auto-discover
        // loadServices with empty args triggers classpath scan
        HttpServer.loadServices(new String[]{});
        assertTrue(HttpServer.services.containsKey("/greeting"), "Auto-discovery should register /greeting");
    }

    @Test
    void testStaticAppJsIsPackaged() {
        // resource must be available on classpath under webroot
        assertNotNull(HttpServer.class.getClassLoader().getResourceAsStream("webroot/app.js"), "app.js should be present in classpath under webroot");
    }

}
