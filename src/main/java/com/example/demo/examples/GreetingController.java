package com.example.demo.examples;

import com.example.demo.annotations.*;
import java.util.concurrent.atomic.AtomicLong;

@RestController
public class GreetingController {

	private static final String template = "Hello, %s!";
	private final AtomicLong counter = new AtomicLong();

	@GetMapping("/greeting")
	public static String greeting() {
		return "Hola world";
	}
}
