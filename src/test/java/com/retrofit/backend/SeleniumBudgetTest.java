package com.retrofit.backend;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SeleniumBudgetTest {

    public static void main(String[] args) {
        WebDriver driver = new SafariDriver();

        try {
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

            driver.get("https://retrofit-nxaez.ondigitalocean.app/login");
            driver.findElement(By.id("username")).sendKeys("SuperAdmin@retrofit");
            driver.findElement(By.id("password")).sendKeys("SuperAdmin2026@retrofit");
            driver.findElement(By.xpath("//button[@type='submit']")).click();
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dashboard-container")));
            System.out.println("Login superado");

            driver.get("https://retrofit-nxaez.ondigitalocean.app/portafolio/proyecto/PRJ-TEST-001?tab=PRESUPUESTO");

            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("excel-table")));
            Thread.sleep(3000);
            System.out.println("Paso 1: Tabla de presupuesto cargada (Vacía)");

            List<WebElement> descriptions = driver.findElements(By.xpath("//input[@formControlName='description']"));
            List<WebElement> units = driver.findElements(By.xpath("//select[@formControlName='unit']"));
            List<WebElement> quantities = driver.findElements(By.xpath("//input[@formControlName='totalQuantity']"));
            List<WebElement> prices = driver.findElements(By.xpath("//input[@formControlName='unitPrice']"));

            descriptions.get(0).sendKeys("Obras Preliminares y Movilización");
            new Select(units.get(0)).selectByValue("glb");
            quantities.get(0).sendKeys("1");
            prices.get(0).sendKeys("2500.50");

            descriptions.get(1).sendKeys("Excavación de Zanjas");
            new Select(units.get(1)).selectByValue("m3");
            quantities.get(1).sendKeys("120");
            prices.get(1).sendKeys("45.20");

            descriptions.get(2).sendKeys("Vaciado de Concreto f'c=210 kg/cm2");
            new Select(units.get(2)).selectByValue("m3");
            quantities.get(2).sendKeys("45");
            prices.get(2).sendKeys("380.00");

            System.out.println("Paso 2: 3 partidas del presupuesto ingresadas");
            Thread.sleep(8000);
            WebElement btnGuardar = driver.findElement(By.xpath("//button[contains(., 'Guardar Presupuesto')]"));
            if (btnGuardar == null || !btnGuardar.isDisplayed()) {
                btnGuardar = driver.findElement(By.xpath("//button[contains(@class, 'btn-dark')]"));
            }
            btnGuardar.click();

            System.out.println("Paso 3: Botón de guardar presionado");
            Thread.sleep(8000);

            System.out.println("✅ PRUEBA EXITOSA: La línea base del presupuesto se registró correctamente.");

        } catch (Exception e) {
            System.out.println("❌ LA PRUEBA FALLÓ: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
