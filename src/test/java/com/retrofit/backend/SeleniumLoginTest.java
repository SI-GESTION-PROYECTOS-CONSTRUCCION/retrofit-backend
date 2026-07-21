package com.retrofit.backend;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SeleniumLoginTest {

    public static void main(String[] args) {
        WebDriver driver = new SafariDriver();

        try {
            driver.manage().window().maximize();

            driver.get("https://retrofit-nxaez.ondigitalocean.app/login");

            System.out.println("Pantalla de login vacía");
            Thread.sleep(3000);

            WebElement emailInput = driver.findElement(By.id("username"));
            emailInput.sendKeys("SuperAdmin@retrofit");

            WebElement passwordInput = driver.findElement(By.id("password"));
            passwordInput.sendKeys("SuperAdmin2026@retrofit");

            System.out.println("Credenciales ingresadas");
            Thread.sleep(3000);

            WebElement btnLogin = driver.findElement(By.xpath("//button[@type='submit']"));
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", btnLogin);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dashboard-container")));

            System.out.println("✅ PRUEBA EXITOSA: El login funcionó y cargó el dashboard.");
            System.out.println("Dashboard cargado");
            Thread.sleep(5000);

        } catch (Exception e) {
            System.out.println("❌ LA PRUEBA FALLÓ: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
