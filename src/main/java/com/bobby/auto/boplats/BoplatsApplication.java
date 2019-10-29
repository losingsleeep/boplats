package com.bobby.auto.boplats;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoplatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoplatsApplication.class, args);
	}

	@Bean
	@Scope("prototype")
	public WebDriver webDriver(@Value("${driver.path}") String exePath){
		System.setProperty("webdriver.chrome.driver", exePath);
		return new ChromeDriver();
	}

	public static Integer f(){
		return 1;
	}

}
