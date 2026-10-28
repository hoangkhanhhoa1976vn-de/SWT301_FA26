package tests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import utils.DriverFactory;
import utils.ScreenshotExtension;

@ExtendWith(ScreenshotExtension.class)
public abstract class BaseTest {
    protected static WebDriver driver;

    @BeforeAll
    public static void setUpBase() {
        driver = DriverFactory.createDriver();
        driver.manage().window().maximize();
    }

    public static WebDriver getDriver() {
        return driver;
    }

    @AfterAll
    public static void tearDownBase() {
        if (driver != null) {
            driver.quit();
        }
    }
}
