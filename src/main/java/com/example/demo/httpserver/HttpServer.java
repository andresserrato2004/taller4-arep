package com.example.demo.httpserver;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.example.demo.annotations.*;

import java.lang.reflect.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.nio.file.*;
import java.util.Enumeration;

public class HttpServer {

    // map path -> (method)
    public static Map<String, Method> services = new HashMap<>();
    // map method -> instance
    public static Map<Method, Object> instances = new HashMap<>();

    public static void loadServices(String[] args) {
        try {
            if (args != null && args.length > 0) {
                // load provided classes
                for (String className : args) {
                    try {
                        Class<?> c = Class.forName(className);
                        registerControllerClass(c);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Class not found: " + className);
                    }
                }
            } else {
                // auto-discover controllers on the classpath
                for (String cpEntry : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
                    Path p = Paths.get(cpEntry);
                    if (Files.isDirectory(p)) {
                        // scan directory for .class files
                        Files.walk(p)
                                .filter(fp -> fp.toString().endsWith(".class"))
                                .forEach(fp -> {
                                    String rel = p.relativize(fp).toString();
                                    String className = rel.replace(FileSystems.getDefault().getSeparator(), ".").replaceAll("\\.class$", "");
                                    try {
                                        Class<?> c = Class.forName(className);
                                        registerControllerClass(c);
                                    } catch (Throwable ex) {
                                        // ignore load errors
                                    }
                                });
                    } else if (cpEntry.endsWith(".jar")) {
                        try (JarFile jf = new JarFile(cpEntry)) {
                            Enumeration<JarEntry> en = jf.entries();
                            while (en.hasMoreElements()) {
                                JarEntry je = en.nextElement();
                                String name = je.getName();
                                if (name.endsWith(".class")) {
                                    // handle Spring Boot repackaged JARs where classes are under BOOT-INF/classes/
                                    String className = name;
                                    if (className.startsWith("BOOT-INF/classes/")) {
                                        className = className.substring("BOOT-INF/classes/".length());
                                    }
                                    className = className.replace('/', '.').replaceAll("\\.class$", "");
                                    try {
                                        Class<?> c = Class.forName(className);
                                        registerControllerClass(c);
                                    } catch (Throwable ex) {
                                        // ignore
                                    }
                                }
                            }
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.getLogger(HttpServer.class.getName()).log(Level.SEVERE, null, e);
            e.printStackTrace();
        }

    }

    private static void registerControllerClass(Class<?> c) {
        if (c == null) return;
        if (c.isAnnotationPresent(RestController.class)) {
            try {
                Object instance = c.getDeclaredConstructor().newInstance();
                Method[] methods = c.getDeclaredMethods();
                for (Method m : methods) {
                    if (m.isAnnotationPresent(GetMapping.class)) {
                        String mapping = m.getAnnotation(GetMapping.class).value();
                        services.put(mapping, m);
                        instances.put(m, instance);
                        System.out.println("Registered: " + mapping + " -> " + c.getName() + "." + m.getName());
                    }
                }
            } catch (Throwable t) {
                // ignore instantiation errors
            }
        }
    }

    public static void runServer(String[] args) throws IOException, URISyntaxException, IllegalAccessException, InvocationTargetException {
        loadServices(args);

        
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(35000);
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        }
        Socket clientSocket = null;

        boolean running = true;
        while (running) {
            try {
                System.out.println("Listo para recibir ...");
                System.out.println("por el puerto  35000");
                System.out.println("http://localhost:35000/");
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept failed.");
                System.exit(1);
            }

            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            clientSocket.getInputStream()));
            String inputLine, outputLine;

            String path = null;
            boolean firstline = true;
            URI requri = null;

            while ((inputLine = in.readLine()) != null) {
                if (firstline) {
                    requri = new URI(inputLine.split(" ")[1]);
                    System.out.println("Path: " + requri.getPath());
                    firstline = false;
                }
                System.out.println("Received: " + inputLine);
                if (!in.ready()) {
                    break;
                }
            }

            String reqPath = requri.getPath();
            if (reqPath.startsWith("/app/")) {
                outputLine = invokeService(requri);
            } else if (services.containsKey(reqPath)) {
                // allow direct paths like /hello to map to services registered as "/hello"
                String q = requri.getQuery();
                String fake = "/app" + reqPath + (q != null ? "?" + q : "");
                outputLine = invokeService(new URI(fake));
            } else if (reqPath.equals("/hello") || reqPath.equals("/hellopost")) {
                // simple internal handlers kept for the demo (returns JSON)
                outputLine = helloService(requri);
            } else {
                // Serve static resources from classpath /webroot
                outputLine = staticResponse(requri);
            }
            out.println(outputLine);

            out.close();
            in.close();
            clientSocket.close();
        }
        serverSocket.close();
    }

    private static String helloService(URI requesturi) {
        //Encabezado con content-type adaptado para retornar un JSON        
        String response = "HTTP/1.1 200 OK\n\r"
                + "content-type: application/json\n\r"
                + "\n\r";
        //Extrae el valor de "name" desde el query.
        String name = requesturi.getQuery().split("=")[1]; //name=jhon

        //Crea la respuesta completa                
        response = response + "{\"mensaje\": \"Hola " + name + "\"}";
        return response;
    }

    private static String staticResponse(URI requri) {
        String path = requri.getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        // normalize path and build resource path
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String resourcePath = "webroot" + path;
        System.out.println("Requesting static resource: " + resourcePath);
        try {
            try (InputStream is = HttpServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is == null) {
                    System.out.println("Static resource not found: " + resourcePath);
                    return "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found";
                }
                byte[] bytes = is.readAllBytes();
                String contentType = guessContentType(resourcePath);
                String header = "HTTP/1.1 200 OK\r\n" + "content-type: " + contentType + "\r\n" + "\r\n";
                if (contentType.startsWith("text/") || contentType.startsWith("application/javascript")) {
                    return header + new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                } else {
                    // for binary, return header and base64 body (simple approach)
                    String body = java.util.Base64.getEncoder().encodeToString(bytes);
                    return header + body;
                }
            }
        } catch (IOException e) {
            return "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + e.getMessage();
        }
    }

    private static String guessContentType(String resourcePath) {
        if (resourcePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (resourcePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (resourcePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (resourcePath.endsWith(".png")) return "image/png";
        if (resourcePath.endsWith(".jpg") || resourcePath.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }


    public static String invokeService(URI requri) throws IllegalAccessException, InvocationTargetException {
        HttpRequest req = new HttpRequest(requri);
        HttpResponse res = new HttpResponse();
        String service = requri.getPath().substring(4);
        Method s = services.get(service);

        String header = "HTTP/1.1 200 OK\n\r"
                + "content-type: text/html\n\r"
                + "\n\r";

        if (s == null) {
            return header + "Not Found";
        }

        // resolve parameters
        Parameter[] params = s.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Parameter p = params[i];
            if (p.isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = p.getAnnotation(RequestParam.class);
                String name = rp.value();
                String defaultVal = rp.defaultValue();
                String val = req.getValue(name);
                if (val == null) val = defaultVal;
                args[i] = val;
            } else {
                args[i] = null; // unsupported param
            }
        }

        Object instance = instances.get(s);
        Object result = s.invoke(instance, args);
        return header + (result != null ? result.toString() : "");

    }

    public static void staticfiles(String localFilesPath) {
    }

    public static void start(String[] args) throws IOException, URISyntaxException, IllegalAccessException, InvocationTargetException {
        runServer(args);
    }

    public static String defaultResponse() {
        String resourcePath = "webroot/index.html";
        try (InputStream is = HttpServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String header = "HTTP/1.1 200 OK\r\n" + "content-type: text/html; charset=UTF-8\r\n" + "\r\n";
                return header + new String(bytes);
            }
        } catch (IOException e) {
            // fall through to embedded fallback
        }

        // Fallback embedded HTML if resource not found
        return "HTTP/1.1 200 OK\r\n"
                + "content-type: text/html\r\n"
                + "\r\n"
                + "<!DOCTYPE html>\n"
                + "<html>\n"
                + "<head>\n"
                + "<title>Form Example</title>\n"
                + "<meta charset=\"UTF-8\">\n"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "</head>\n"
                + "<body>\n"
                + "<h1>Form with GET</h1>\n"
                + "<form action=\"/hello\">\n"
                + "<label for=\"name\">Name:</label><br>\n"
                + "<input type=\"text\" id=\"name\" name=\"name\" value=\"John\"><br><br>\n"
                + "<input type=\"button\" value=\"Submit\" onclick=\"loadGetMsg()\">\n"
                + "</form>\n"
                + "<div id=\"getrespmsg\"></div>\n"
                + " \n"
                + "<script>\n"
                + "function loadGetMsg() {\n"
                + "let nameVar = document.getElementById(\"name\").value;\n"
                + "const xhttp = new XMLHttpRequest();\n"
                + "xhttp.onload = function() {\n"
                + "document.getElementById(\"getrespmsg\").innerHTML =\n"
                + "this.responseText;\n"
                + "}\n"
                + "xhttp.open(\"GET\", \"/app/hello?name=\"+nameVar);\n"
                + "xhttp.send();\n"
                + "}\n"
                + "</script>\n"
                + " \n"
                + "<h1>Form with POST</h1>\n"
                + "<form action=\"/hellopost\">\n"
                + "<label for=\"postname\">Name:</label><br>\n"
                + "<input type=\"text\" id=\"postname\" name=\"name\" value=\"John\"><br><br>\n"
                + "<input type=\"button\" value=\"Submit\" onclick=\"loadPostMsg(postname)\">\n"
                + "</form>\n"
                + " \n"
                + "<div id=\"postrespmsg\"></div>\n"
                + " \n"
                + "<script>\n"
                + "function loadPostMsg(name){\n"
                + "let url = \"/hellopost?name=\" + name.value;\n"
                + " \n"
                + "fetch (url, {method: 'POST'})\n"
                + ".then(x => x.text())\n"
                + ".then(y => document.getElementById(\"postrespmsg\").innerHTML = y);\n"
                + "}\n"
                + "</script>\n"
                + "</body>\n"
                + "</html>";
    }

}
