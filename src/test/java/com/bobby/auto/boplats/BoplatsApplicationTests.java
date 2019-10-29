package com.bobby.auto.boplats;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RunWith(SpringRunner.class)
@SpringBootTest
public class BoplatsApplicationTests {

	@Test
	public void f() {
		driver.get("https://nya.boplats.se/objekt/1hand/5C5CBACEFD0C1E1463AA9A99");
		notTooManyApplicantsAlreadyRegistered();
	}

	@Autowired
	WebDriver driver;

	int MAX_APPLICANTS = 40;

	private boolean notTooManyApplicantsAlreadyRegistered() {

		String path = "//*[@id=\"sortingDetails\"]";
		WebElement applicants = getByXPath(path).get();
		String value = applicants.getText().trim();
		int applicantsCount = Integer.parseInt(value.split(" ")[0]);
		//log.info("Applicants: {}",applicantsCount);
		if (applicantsCount <= MAX_APPLICANTS){
			return true;
		}else {
			return false;
		}

	}

	private Optional<WebElement> getByXPath(String xpath){
		Optional<WebElement> container;
		try {
			return Optional.of(driver.findElement(By.xpath(xpath)));
		}catch (NoSuchElementException e){
			return Optional.empty();
		}
	}

	@Test
	public void publishDateLooksGood() {
		//DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		// if publish date is not too old, for example yesterday, then we can apply, otherwise so many applicants will be before us
		String publishDateString = "2019-02-09";
		LocalDate publishDate = LocalDate.parse(publishDateString).plusDays(1);
		LocalDate notBefore = LocalDate.now();
		boolean isOK = !publishDate.isBefore(notBefore);
		System.out.printf("OK = "+isOK);
	}

}
