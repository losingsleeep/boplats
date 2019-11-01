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
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static java.nio.charset.StandardCharsets.UTF_8;

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

	@Value("classpath:email_request.json")
	Resource emailResource;

	@Bean
	public String emailRequestBodyAsString() throws IOException {
		Reader reader = new InputStreamReader(emailResource.getInputStream(), UTF_8);
		return FileCopyUtils.copyToString(reader);
	}

	public static Integer f(){
		return 1;
	}

}
