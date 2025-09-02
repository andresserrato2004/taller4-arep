# WebApp Framework — Minimal HTTP Server in Java

A small sequential HTTP server implemented from scratch in Java 21. It supports serving static files from the classpath, discovering controllers by annotations, and basic routing via a tiny IoC-like micro-framework using annotations `@RestController`, `@GetMapping` and `@RequestParam`.

## Features

🚀 Lambda-based route registration: define handlers with small, focused functions

🔍 Query parameter extraction: easy access to URL query parameters from handler code

📁 Unified static files: all static assets served from `webroot` (packaged under resources)

📡 Request/Response objects: lightweight wrappers to access path, headers, query params, and body

🏗️ Modular architecture: components are separated for clarity and easier extension



### Architecture (files)

- `RouteHandler.java` — functional interface for route handlers
- `Request.java` — wrapper for an incoming HTTP request (query params, headers, body)
- `Response.java` — wrapper for response construction
- `WebFramework.java` — static API for registering routes and configuring static files
- `HttpServer.java` — the low-level HTTP accept loop and dispatcher
- `App.java` — example application that demonstrates usage


## Static files configuration

```java
WebFramework.staticfiles("webroot"); // sets the directory for static resources
```

Where the framework looks for static files (in order):
1. `target/classes/webroot` (after Maven build)
2. `src/main/resources/webroot` (source)
3. `webroot` (project root / direct path)

Static files in this repository live under `src/main/resources/webroot/`.

## Example endpoints

- `http://localhost:35000/hello?name=Pedro` → Personalized greeting

Static file examples:

- `http://localhost:35000/` or `/index.html` → demo page
- `http://localhost:35000/styles.css` → stylesheet
- `http://localhost:35000/app.js` → JavaScript

## Requirements

- Git
- Java 21 or higher (project uses Java 21 in this repository)
- Maven 3.9.x

## Clone the project

```bash
git clone https://github.com/andresserrato2004/taller3-arep.git
cd taller3-arep
```

## Building and running

### With Maven (recommended)

```bash
mvn clean install

mvn package

java -jar target/demo-0.0.1-SNAPSHOT.jar 
```

### Run tests / run application in another terminal

```bash
mvn test
```

Open `http://localhost:35000/` in your browser. Stop the server with Ctrl+C.

## Project structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── RouteHandler.java    # functional interface (example)
│   │   ├── Request.java         # request wrapper
+│   │   ├── Response.java        # response wrapper
│   │   ├── WebFramework.java    # convenience API for registering handlers
│   │   ├── HttpServer.java      # HTTP server core
│   │   └── App.java             # example application
│   └── resources/webroot/
│       ├── index.html
│       ├── styles.css
│       ├── app.js
│       └── img/
└── test/
    └── java/com/example/demo/
        └── HttpServerTest.java

pom.xml
```

## Fast test with curl

```bash
# GET request
curl "http://localhost:35000/hello?name=Pedro"

# Test static files
curl "http://localhost:35000/"
```

## REST API Endpoints

- GET `/hello?name=Pedro` → `Hello Pedro`

## Static File Endpoints

- GET `/` → Demo HTML page
- GET `/index.html` → Demo HTML page
- GET `/styles.css` → CSS stylesheet
- GET `/app.js` → JavaScript file


## Author

- Andrés Serrato Camero
