# Web Framework for REST Services and Static File Management

A powerful concurrent web framework implemented from scratch in Java 21 that allows developers to register REST endpoints with annotation-based controllers and serve static files. Features include thread pool concurrency, graceful shutdown, and IoC-style dependency injection.

> Academic exercise for the course: Arquitecturas Empresariales (AREP) — Workshop 3.

## Features

🚀 **Annotation-based Controllers**: Define REST endpoints using `@RestController`, `@GetMapping`, and `@RequestParam`

🔍 **Query Parameter Injection**: Automatic parameter binding with default value support

📁 **Static File Serving**: Unified static assets served from `webroot` (packaged under resources)

📡 **Concurrent Request Handling**: Thread pool executor for handling multiple simultaneous requests

🛑 **Graceful Shutdown**: Proper cleanup of resources and thread pools

🏗️ **Custom Framework**: Built from scratch without Spring dependencies

⚡ **Auto-Discovery**: Automatic scanning and registration of controller classes


## Quick Start

### Example Controller

```java
import com.example.demo.annotations.GetMapping;
import com.example.demo.annotations.RequestParam;
import com.example.demo.annotations.RestController;

@RestController
public class GreetingController {
    @GetMapping("/greeting")
    public String greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        return "Hello " + name;
    }
}
```

### Architecture (core components)

- `HttpServer.java` — concurrent HTTP server with thread pool management
- `annotations/*` — custom annotations for IoC framework (`@RestController`, `@GetMapping`, `@RequestParam`)
- `examples/GreetingController.java` — example REST controller
- `httpserver/HttpRequest.java` — HTTP request parsing and parameter extraction
- `DemoApplication.java` — main application entry point

## Concurrency Features

The server uses a thread pool executor to handle multiple concurrent requests:

- **Thread Pool Size**: 10 concurrent connections (configurable)
- **Request Isolation**: Each request is handled in its own thread
- **Graceful Shutdown**: Proper cleanup when server stops
- **Thread Safety**: Controller instances are shared safely across threads

## Shutdown Options

1. **Ctrl+C**: Graceful shutdown via shutdown hook
2. **HTTP Endpoint**: `GET /shutdown` to stop server remotely
3. **Programmatic**: Call `HttpServer.stop()` from code

## Example endpoints

- `http://localhost:35000/app/greeting?name=Pedro` → Personalized greeting
- `http://localhost:35000/shutdown` → Shutdown the server gracefully

Static file examples:

- `http://localhost:35000/` or `/index.html` → demo page
- `http://localhost:35000/styles.css` → stylesheet
- `http://localhost:35000/app.js` → JavaScript
- `http://localhost:35000/img/favico.ico` → favicon
- `http://localhost:35000/img/logo.jpg` → logo image

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

### para correrlo con docker debes usar  el comando 

```bash
docker compose up -d
```

Open `http://localhost:35000/` in your browser. Stop the server with Ctrl+C.

## Project structure

```
src/
├── main/
│   ├── java/com/example/demo/
│   │   ├── DemoApplication.java
│   │   ├── annotations/{GetMapping.java, RequestParam.java, RestController.java}
│   │   ├── examples/{GreetingController.java}
│   │   └── httpserver/{HttpRequest.java, HttpResponse.java, HttpServer.java}
│   └── resources/webroot/
│       ├── index.html
│       ├── styles.css
│       ├── app.js
│       └── img/
│           ├── favico.ico
│           └── logo.jpg
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

- GET `/app/greeting?name=Pedro` → `Hello Pedro`
- GET `/shutdown` → Graceful server shutdown

## Static File Endpoints

* GET `/` → Demo HTML page
* GET `/index.html` → Demo HTML page
* GET `/styles.css` → CSS stylesheet
* GET `/app.js` → JavaScript file
* GET `/img/logo.jpg` → Example image

## Development
You can add or update static frontend files directly in `src/main/resources/webroot/`.


## Author

- Andrés Serrato Camero
