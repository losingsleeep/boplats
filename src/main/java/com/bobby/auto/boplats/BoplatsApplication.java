package com.bobby.auto.boplats;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoplatsApplication {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public static void main(String[] args) {
		SpringApplication.run(BoplatsApplication.class, args);
	}

	@Bean
	@Scope("prototype")
	public WebDriver webDriverChrome(@Value("${driver.chrome.path}") String exePath){
		log.info("Creating ChromeDriver bean");
		System.setProperty("webdriver.chrome.driver", exePath);
		return new ChromeDriver();
	}

	@ConditionalOnMissingBean(WebDriver.class)
	@Bean
	@Scope("prototype")
	public WebDriver webDriverFirefox(@Value("${driver.firefox.path}") String firefoxPath){
		log.info("Creating FirefoxDriver bean");
		System.setProperty("webdriver.gecko.driver", firefoxPath);
		return new FirefoxDriver();
	}

	public static Integer f(){
		return 1;
	}

}
