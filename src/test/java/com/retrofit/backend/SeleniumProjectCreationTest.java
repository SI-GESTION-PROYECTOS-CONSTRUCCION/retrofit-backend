package com.retrofit.backend;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SeleniumProjectCreationTest {

    public static void main(String[] args) {
        WebDriver driver = new SafariDriver();

        try {
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            driver.get("https://retrofit-nxaez.ondigitalocean.app/login");
            driver.findElement(By.id("username")).sendKeys("SuperAdmin@retrofit");
            driver.findElement(By.id("password")).sendKeys("SuperAdmin2026@retrofit");
            WebElement btnLogin = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnLogin);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dashboard-container")));
            System.out.println("Login superado");

            driver.get("https://retrofit-nxaez.ondigitalocean.app/portafolio");
            Thread.sleep(3000);
            System.out.println("Paso 1: Portafolio de Proyectos cargado");
            WebElement btnNuevo = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(., 'Nuevo Proyecto')]")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnNuevo);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("modal-container")));
            System.out.println("Modal de creación abierto");

            driver.findElement(By.xpath("//input[@formControlName='code']")).sendKeys("PRJ-TEST-002");
            driver.findElement(By.xpath("//input[@formControlName='name']")).sendKeys("Proyecto Automatizado Selenium");
            driver.findElement(By.xpath("//input[@formControlName='client']")).sendKeys("Cliente Test SA");
            driver.findElement(By.xpath("//input[@formControlName='location']")).sendKeys("Lima Peru");

            WebElement dateInput = driver.findElement(By.xpath("//input[@formControlName='startDate']"));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='2026-07-21';" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    dateInput);

            driver.findElement(By.xpath("//textarea[@formControlName='description']")).sendKeys(
                    "Este es un proyecto creado automáticamente por el robot de Selenium para probar el E2E.");

            Select statusSelect = new Select(driver.findElement(By.xpath("//select[@formControlName='status']")));
            statusSelect.selectByValue("PLANNING");

            Select prioritySelect = new Select(driver.findElement(By.xpath("//select[@formControlName='priority']")));
            prioritySelect.selectByValue("HIGH");
            Select managerSelect = new Select(driver.findElement(By.xpath("//select[@formControlName='managerId']")));
            managerSelect.selectByIndex(2);

            System.out.println("Paso 2: Formulario llenado");
            Thread.sleep(3000);

            WebElement btnSubmit = driver.findElement(By.className("btn-submit"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnSubmit);
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("modal-container")));
            System.out.println("✅ PRUEBA EXITOSA: El proyecto se guardó correctamente en la base de datos.");
            Thread.sleep(4000);

        } catch (Exception e) {
            System.out.println("❌ LA PRUEBA FALLÓ: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
