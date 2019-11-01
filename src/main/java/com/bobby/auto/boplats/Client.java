package com.bobby.auto.boplats;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author Babak Eghbali (Bob)
 * @since 2018/11/21
 */
@Component
public class Client {


    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private String emailRequestBodyAsString;

    @Autowired
    WebDriver driver;

    @Value("${notification.email}")
    private Boolean emailNotificationEnabled;

    @Value("${login.user}")
    private String user;

    @Value("${login.pass}")
    private String pass;

    @Value("${moveinDate}")
    private String moveinDateStringFromConfig;

    @Value("${minSize}")
    private Integer minSize;

    @Value("${maxApplicants}")
    private Integer maxApplicants;

    @Value("${maxPrice}")
    private Integer maxPrice;

    private Set<String> skipAds = new HashSet<>();

    @Autowired
    private ApplicationContext applicationContext;

    private void refreshDriver(){
        log.info("Going to refresh the web driver");
        driver.quit();
        driver = applicationContext.getBean(WebDriver.class);
    }

    @Scheduled(fixedRate = 1000*60*5) // 5 minutes, 1000*60*1 = 1 minute
    public void run() throws Exception {
        log.info("==================== Started ====================");
        tryLogin(5);
        search();
        processResults();
//        driver.close();
//        log.info("====================closed all windows====================");
    }

    private void getHomePage(){
        driver.get("http://www.boplats.se/");
    }

    private void tryLogin(int attemptsCount) throws Exception {
        boolean failed = true;
        for (int i = 1; i <= attemptsCount && failed; i++) {
            try {
                getHomePage();
                wait(2);
                log.info("login attempt {}",i);
                login();
                failed = false;
            }
//            catch (Exception e){
//                log.error("NoSuchWindowException occurred while trying to login.",e);
//                refreshDriver();
//                i = 1;
//                failed = true;
//            }
            catch (Exception e){
                log.error("error occurred while trying to login",e);
                refreshDriver();
                i = 1;
                failed = true;
            }
        }
        if (failed){
            throw new Exception("exceeded login attempts");
        }
    }

    private void processResults_TEST_Apply_Without_Condition_Check() {
        final Integer NOT_MATCH = 1;
        final Integer MATCH = 2;
        final Integer NEED_LOGIN = 0;
        int row = 0;
        while (row < 7){ // only first 7 rows
            row++;
            String rowPath = "//*[@id=\"search-result-items\"]/tr["+row+"]/td";
            String adUrl = getByXPath(rowPath + "/a").get().getAttribute("href");
            if (skipAds.contains(adUrl)){
                log.info("Ad is checked before. Skipped!");
                continue;
            }
            skipAds.add(adUrl);
            Integer price = getPrice(rowPath);
            WebElement condition = getByXPath(rowPath+"/a/div[2]/div[6]").get();
            Integer termsAndConditionFieldValue = Integer.parseInt(condition.getAttribute("data-value"));
            log.info(" ---------- [ AD ] price:  " + price + " , condition type: "+ termsAndConditionFieldValue+" ----------");
            if (NEED_LOGIN.equals(termsAndConditionFieldValue)){
                log.warn("Terms & conditions not visible! Login required!");
                return;
            }else{
                if (openAndApplyForAd(adUrl)){

                        /* Break after 1st apply, so the entire process will start at next scheduled interval,
                        otherwise current While loop will encounter errors for all next tries as current page is not the search results anymore.
                        When process starts again at next scheduled interval, it will skip all applied records
                        */
                    //break;
                    goBack(2); // should back twice after successful register
                }else {
                    goBack(1); // only once after doing nothing in Ads page
                }
            }

        }
    }

    private void processResults() {
        final Integer NOT_MATCH = 1;
        final Integer MATCH = 2;
        final Integer NEED_LOGIN = 0;
        int row = 0;
        while (row < 7){ // only first 7 rows
            row++;
            String rowPath = "//*[@id=\"search-result-items\"]/tr["+row+"]/td";
            String adUrl = getByXPath(rowPath + "/a").get().getAttribute("href");
            if (skipAds.contains(adUrl)){
                log.info("Ad is checked before. Skipped!");
                continue;
            }
            skipAds.add(adUrl);
            Integer price = getPrice(rowPath);
            WebElement condition = getByXPath(rowPath+"/a/div[2]/div[6]").get();
            Integer termsAndConditionFieldValue = Integer.parseInt(condition.getAttribute("data-value"));
            log.info(" ---------- [ AD ] price:  " + price + " , condition type: "+ termsAndConditionFieldValue+" ----------");
            if (NEED_LOGIN.equals(termsAndConditionFieldValue)){
                log.warn("Terms & conditions not visible! Login required!");
                return;
            }else if (MATCH.equals(termsAndConditionFieldValue)){
                log.info("applicable found, checking conditions");
                // if move-in-date is good (e.g. later than 3 month)
                if (priceLooksGood(price) && moveInDateLooksGood(rowPath) && publishDateLooksGood(rowPath) && sizeLooksGood(rowPath)){
                    // good, go for it
                    log.info("pre-conditions passed. opening ad for check & interest registration");
                    if (openAndApplyForAd(adUrl)){
                        /* Break after 1st apply, so the entire process will start at next scheduled interval,
                        otherwise current While loop will encounter errors for all next tries as current page is not the search results anymore.
                        When process starts again at next scheduled interval, it will skip all applied records
                        */
                        //break;
                        sendNotification(adUrl);
                        goBack(2); // should back twice after successful register
                    }else {
                        goBack(1); // only once after doing nothing in Ads page
                    }
                }else {
                    log.info("Conditions not met. Skipped!");
                }
            }else {
                log.info("Not applicable! Terms & conditions wise.");
            }

        }
    }

    private void sendNotification(String adUrl) {
        if (emailNotificationEnabled){
            sendEmailNotification(adUrl);
        }
    }

    private void sendEmailNotification(String adUrl) {
        try {
            // prepare request body
            String requestBody = emailRequestBodyAsString.replace("${adUrl}",adUrl);
            // send the email
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("https://api.sendgrid.com/v3/mail/send");

            StringEntity requestBodyEntity = new StringEntity(requestBody);
            httpPost.setEntity(requestBodyEntity);
            httpPost.setHeader("Authorization", "Bearer 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");
            httpPost.setHeader("Content-type", "application/json");

            CloseableHttpResponse response = client.execute(httpPost);
            Integer responseCode = response.getStatusLine().getStatusCode();
            if (!responseCode.toString().startsWith("2")){ // if HTTP response status code is not 2xx
                log.error("Email notification sent, but was received status code {} with response body: {}",
                        responseCode, EntityUtils.toString(response.getEntity(), "UTF-8"));
            } else {
                log.info("Email notification sent successfully.");
            }
            response.close();
            client.close();
        } catch (IOException e) {
            log.error("Failed to send email notification", e);
        }
    }

//    public String emailRequestAsString() throws IOException {
//        Reader reader = new InputStreamReader(emailResource.getInputStream(), UTF_8);
//        return FileCopyUtils.copyToString(reader);
//    }

    private Integer getPrice(String rowPath){
        String priceString;
        Optional<WebElement> priceElement = getByXPath(rowPath+"/a/div[2]/div[1]");
        priceString = priceElement.get().getAttribute("data-value");
        Integer price = Integer.parseInt(priceString);
        return price;
    }

    private boolean priceLooksGood(Integer price) {
//        String priceString = priceElement.getText().split(" ")[0].replace(",","");
//        Integer price = Integer.parseInt(priceString);
        log.info("Price: {} , Max Price: {}",price, maxPrice);
        return price <= maxPrice;
    }

    private void goBack(int count) {
        for (int i = 0; i <count ; i++) {
            driver.navigate().back();
        }
    }

    private boolean moveInDateLooksGood(String rowPath) {
        WebElement moveInDateElement = getByXPath(rowPath+"/a/div[2]/div[4]").get();
        String moveInDateString = moveInDateElement.getAttribute("data-value").substring(0,10);
        LocalDate moveInDate = LocalDate.parse(moveInDateString);
        LocalDate targetDate = LocalDate.parse(moveinDateStringFromConfig);
        log.info("target date: {} , move-in date: {}",targetDate,moveInDate);
        return moveInDate.isAfter(targetDate);
    }

    private boolean publishDateLooksGood(String rowPath) {
        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        // if publish date is not too old, for example yesterday, then we can apply, otherwise so many applicants will be before us
//        WebElement publishDateElement = getByXPath(rowPath+"/a/div[2]/div[5]").get();
//        String publishDateString = publishDateElement.getAttribute("data-value").substring(0,10);
//        LocalDate publishDate = LocalDate.parse(publishDateString);

        WebElement dayAndMonth = getByXPath(rowPath+"/a/div[2]/div[5]/p[1]").get(); // "11 Feb"
        String dayString = dayAndMonth.getText().substring(0,2).trim(); // "11"
        int day = Integer.parseInt(dayString);

        LocalDate notBefore = LocalDate.now();
        log.info("today : {} , publish date: {}",notBefore,dayAndMonth.getText());

//        return publishDate.isEqual(notBefore); // must be on the same day

        return notBefore.getDayOfMonth() == day;

    }

    private boolean sizeLooksGood(String rowPath){
        WebElement sizeElement = getByXPath(rowPath+"/a/div[2]/div[3]/p[1]/b").get();
        String value = sizeElement.getText().trim();
        String sizeString = value.split(" ")[0]; // "47.5 m2" => "47.5"
        float size = Float.parseFloat(sizeString);
        log.info("Size: {} , minSize: {}",size, minSize);
        if (size >= minSize){
            return true;
        }else {
            return false;
        }
    }

    private boolean openAndApplyForAd(String adUrl) {
        driver.get(adUrl);
        try {
            if (noRanking(adUrl) && notTooManyApplicantsAlreadyRegistered(adUrl)) {
                log.info("Registering interest...");
                Optional<WebElement> submit = getByXPath("//*[@id=\"apply\"]");
                submit.ifPresent(WebElement::click);
                log.info("-------------------------------------------------------------------------");
                log.info("********** REGISTERED INTEREST **********");
                log.info("Ad URL: {}", adUrl);
                log.info("-------------------------------------------------------------------------");
                return true;
            } else {
                log.info("Not registered.");
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to open and apply", e);
            return false;
        }
    }

    private boolean noRanking(String adUrl) {
        // Click on terms button and open the popup div
        String termsButtonPath = "//*[@id=\"baseCriteriaDetails\"]";
        new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(termsButtonPath)));
        WebElement termsElement = getByXPath(termsButtonPath).get();
        termsElement.click();
        wait(1);

        // get criteria text element
        String criteriaTextPath = "//*[@id=\"searchCriteria\"]/div[1]/div/div";
        WebElement criteriaTextElement = getByXPath(criteriaTextPath).get();
        String criteria = criteriaTextElement.getText();
        log.info("Criteria: {}",criteria);

        // check if criteria needs no ranking
        if (criteria.toLowerCase().contains("no ranking".toLowerCase())){
            log.info("Ranking is NOT needed.");
            return true;
        } else {
            log.info("Ranking is needed.");
            return false;
        }

    }

    private boolean notTooManyApplicantsAlreadyRegistered(String adUrl) {

        String path = "//*[@id=\"sortingDetails\"]";
        WebElement applicantsElement = getByXPath(path).get();
        String value = applicantsElement.getText().trim();
        int applicantsCount = Integer.parseInt(value.split(" ")[0]);
        log.info("Applicants: {}",applicantsCount);
        if (applicantsCount <= maxApplicants){
            log.info("Not so many applicants.");
            return true;
        }else {
            log.info("Too many applicants.");
            return false;
        }

    }

    private void search() {

        selectCity();
        selectMinRooms();
        clickOnSearch();

    }

    private void clickOnSearch() {
        // open rooms combo box
        WebElement searchButton =
                getByXPath("//*[@id=\"objectsearchform\"]/div/table[2]/tbody/tr[3]/td[2]/button").get();
        searchButton.click();
    }

    private void selectMinRooms(){
        wait(2);
        // open rooms combo box
        WebElement roomsCombo =
                getByXPath("//*[@id=\"objectsearchform\"]/div/table[1]/tbody/tr[3]/td[1]/div/div/div/div[1]/label").get();
        roomsCombo.click();
        // click on gothenburg
        wait(2);
        WebElement twoRoomsItem =
                getByXPath("//*[@id=\"objectsearchform\"]/div/table[1]/tbody/tr[3]/td[1]/div/div/div/div[2]/ul/li[4]").get();
        twoRoomsItem.click();
    }

    private void selectCity(){
        // open city combo box
        String cityXPath = "//*[@id=\"objectsearchform\"]/div/table[1]/tbody/tr[2]/td[3]/div/div/div/div[1]/label";
        new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(cityXPath)));
        WebElement city = getByXPath(cityXPath).get();
        city.click();
        // click on gothenburg
        String gothenburgXPath = "//*[@id=\"objectsearchform\"]/div/table[1]/tbody/tr[2]/td[3]/div/div/div/div[2]/ul/li[2]";
        new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(gothenburgXPath)));
        WebElement goteborg = getByXPath(gothenburgXPath).get();
        goteborg.click();
    }

    private void login() {
        String logoutPath = "//*[@id=\"pagehead\"]/div/div[1]/p[2]/a";
        if (getByXPath(logoutPath).isPresent()){
            log.info("no need to log in");
            return;
        }

        WebElement loginEl = driver.findElement(By.xpath("//*[@id=\"login\"]/a/span"));
        loginEl.click();
        String loginFrameXPath = "//*[@id=\"login-frame\"]";
        new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(loginFrameXPath)));
        driver.switchTo().frame(getByXPath(loginFrameXPath).get());
        String usernameXPath = "//*[@id=\"username\"]";
        new WebDriverWait(driver, 10).until(ExpectedConditions.visibilityOfElementLocated(By.xpath(usernameXPath)));
        WebElement usernameEl = getByXPath(usernameXPath).get();
        usernameEl.sendKeys(user);
        WebElement passwordEl = getByXPath("//*[@id=\"password\"]").get();
        passwordEl.sendKeys(pass);
        WebElement loginBtn = driver.findElement(By.xpath("//*[@id=\"loginform\"]/button"));
        loginBtn.click();
    }

    private Optional<WebElement> getByXPath(String xpath){
        try {
            return Optional.of(driver.findElement(By.xpath(xpath)));
        }catch (NoSuchElementException e){
            return Optional.empty();
        }
    }

    private void wait(int sec){
        try {
            Thread.sleep(sec*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
