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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Enumeration;

public class HttpServer {

    public static Map<String, Method> services = new HashMap<>();
    public static Map<Method, Object> instances = new HashMap<>();
    
    private static volatile boolean running = true;
    private static ExecutorService threadPool;
    private static ServerSocket serverSocket;

    public static void loadServices(String[] args) {
        try {
            if (args != null && args.length > 0) {
                for (String className : args) {
                    try {
                        Class<?> c = Class.forName(className);
                        registerControllerClass(c);
                    } catch (ClassNotFoundException e) {
                        System.err.println("Class not found: " + className);
                    }
                }
            } else {
                for (String cpEntry : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
                    Path p = Paths.get(cpEntry);
                    if (Files.isDirectory(p)) {
                        Files.walk(p)
                                .filter(fp -> fp.toString().endsWith(".class"))
                                .forEach(fp -> {
                                    String rel = p.relativize(fp).toString();
                                    String className = rel.replace(FileSystems.getDefault().getSeparator(), ".").replaceAll("\\.class$", "");
                                    try {
                                        Class<?> c = Class.forName(className);
                                        registerControllerClass(c);
                                    } catch (Throwable ex) {
                                    }
                                });
                    } else if (cpEntry.endsWith(".jar")) {
                        try (JarFile jf = new JarFile(cpEntry)) {
                            Enumeration<JarEntry> en = jf.entries();
                            while (en.hasMoreElements()) {
                                JarEntry je = en.nextElement();
                                String name = je.getName();
                                if (name.endsWith(".class")) {
                                    String className = name;
                                    if (className.startsWith("BOOT-INF/classes/")) {
                                        className = className.substring("BOOT-INF/classes/".length());
                                    }
                                    className = className.replace('/', '.').replaceAll("\\.class$", "");
                                    try {
                                        Class<?> c = Class.forName(className);
                                        registerControllerClass(c);
                                    } catch (Throwable ex) {
                                    }
                                }
                            }
                        } catch (IOException e) {
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
            }
        }
    }

    public static void runServer(String[] args) throws IOException, URISyntaxException, IllegalAccessException, InvocationTargetException {
        loadServices(args);

        threadPool = Executors.newFixedThreadPool(10); 
        
        try {
            serverSocket = new ServerSocket(35000);
            System.out.println("Concurrent server started on port 35000");
            System.out.println("Ready to receive connections...");
            System.out.println("http://localhost:35000/");
            
            services.put("/shutdown", null);
            
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(() -> handleClientRequest(clientSocket));
                } catch (IOException e) {
                    if (!running) {
                        break;
                    }
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on port: 35000.");
            System.exit(1);
        } finally {
            shutdown();
        }
    }

    /**
     * Handle a single client request in its own thread
     */
    private static void handleClientRequest(Socket clientSocket) {
        try (Socket socket = clientSocket;
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            
            String inputLine, outputLine = null;
            boolean firstline = true;
            URI requri = null;

            while ((inputLine = in.readLine()) != null) {
                if (firstline) {
                    requri = new URI(inputLine.split(" ")[1]);
                    System.out.println("Thread-" + Thread.currentThread().threadId() + " Path: " + requri.getPath());
                    firstline = false;
                }
                if (!in.ready()) {
                    break;
                }
            }

            if (requri == null) {
                outputLine = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/plain\r\n\r\nBad Request";
                out.println(outputLine);
                return;
            }

            String reqPath = requri.getPath();
            boolean alreadySent = false;
            
            if (reqPath.equals("/shutdown")) {
                outputLine = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nServer shutting down...";
                out.println(outputLine);
                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        stop();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                return;
            }
            
            if (reqPath.startsWith("/app/")) {
                outputLine = invokeService(requri);
            } else if (services.containsKey(reqPath)) {
                String q = requri.getQuery();
                String fake = "/app" + reqPath + (q != null ? "?" + q : "");
                outputLine = invokeService(new URI(fake));
            } else if (reqPath.equals("/hello") || reqPath.equals("/hellopost")) {
                outputLine = helloService(requri);
            } else {
                try {
                    writeStaticResponse(requri, socket.getOutputStream());
                    alreadySent = true;
                } catch (IOException e) {
                    outputLine = "HTTP/1.1 500 Internal Server Error\r\nContent-Type: text/plain\r\n\r\n" + e.getMessage();
                }
            }
 
            if (!alreadySent) {
                out.println(outputLine);
            }
            
        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String helloService(URI requesturi) {
        String response = "HTTP/1.1 200 OK\n\r"
                + "content-type: application/json\n\r"
                + "\n\r";
        String name = requesturi.getQuery().split("=")[1];

        response = response + "{\"mensaje\": \"Hola " + name + "\"}";
        return response;
    }

    /**
     * Write a static resource response (header + body) directly to the provided OutputStream.
     * Serves files from classpath "webroot/...". Binary files are written raw.
     */
    private static void writeStaticResponse(URI requri, OutputStream os) throws IOException {
        String path = requri.getPath();
        if (path.equals("/")) {
            path = "/index.html";
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        String resourcePath = "webroot" + path;
        System.out.println("Requesting static resource: " + resourcePath);

        try (InputStream is = HttpServer.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                String notFound = "HTTP/1.1 404 Not Found\r\nContent-Type: text/plain\r\n\r\nNot Found";
                os.write(notFound.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                os.flush();
                return;
            }
            byte[] bytes = is.readAllBytes();
            String contentType = guessContentType(resourcePath);
            String header = "HTTP/1.1 200 OK\r\n" + "Content-Type: " + contentType + "\r\n" + "\r\n";

            os.write(header.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            os.write(bytes);
            os.flush();
        }
    }

    private static String guessContentType(String resourcePath) {
        if (resourcePath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (resourcePath.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (resourcePath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (resourcePath.endsWith(".png")) return "image/png";
        if (resourcePath.endsWith(".jpg") || resourcePath.endsWith(".jpeg")) return "image/jpeg";
    if (resourcePath.endsWith(".ico")) return "image/x-icon";
    if (resourcePath.endsWith(".gif")) return "image/gif";
    if (resourcePath.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }


    public static String invokeService(URI requri) throws IllegalAccessException, InvocationTargetException {
    HttpRequest req = new HttpRequest(requri);
        String service = requri.getPath().substring(4);
        Method s = services.get(service);

        String header = "HTTP/1.1 200 OK\n\r"
                + "content-type: text/html\n\r"
                + "\n\r";

        if (s == null) {
            return header + "Not Found";
        }

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
                args[i] = null; //
            }
        }

        Object instance = instances.get(s);
        Object result = s.invoke(instance, args);
        return header + (result != null ? result.toString() : "");

    }

    public static void staticfiles(String localFilesPath) {
    }

    /**
     * Stop the server gracefully
     */
    public static void stop() {
        System.out.println("Stopping server...");
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    /**
     * Shutdown thread pool and cleanup resources
     */
    private static void shutdown() {
        System.out.println("Shutting down thread pool...");
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(30, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                    if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                        System.err.println("Thread pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        System.out.println("Server shutdown complete.");
    }

    public static void start(String[] args) throws IOException, URISyntaxException, IllegalAccessException, InvocationTargetException {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nReceived shutdown signal, stopping server...");
            stop();
        }));
        
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
        }

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
