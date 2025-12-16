package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IpTvApplication {

	public static void main(String[] args) {
		SpringApplication.run(IpTvApplication.class, args);
		System.out.println("🚀 IPTV Backend démarré avec MongoDB !");
	}

}
