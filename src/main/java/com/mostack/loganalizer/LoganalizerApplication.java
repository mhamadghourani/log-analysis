package com.mostack.loganalizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LoganalizerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoganalizerApplication.class, args);
		System.out.println("hello its MoStack!");
	}

}
