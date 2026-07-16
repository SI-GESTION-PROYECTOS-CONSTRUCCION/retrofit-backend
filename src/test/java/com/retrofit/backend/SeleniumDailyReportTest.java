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
import java.util.List;

public class SeleniumDailyReportTest {

    public static void main(String[] args) {
        WebDriver driver = new SafariDriver();

        try {
            driver.manage().window().maximize();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get("https://retrofit-nxaez.ondigitalocean.app/login");
            Thread.sleep(2000);
            driver.findElement(By.id("username")).sendKeys("SuperAdmin@retrofit");
            driver.findElement(By.id("password")).sendKeys("SuperAdmin2026@retrofit");
            Thread.sleep(1000);
            WebElement btnLogin = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnLogin);
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("dashboard-container")));
            System.out.println("Login superado");

            driver.get("https://retrofit-nxaez.ondigitalocean.app/portafolio/proyecto/PRJ-TEST-001?tab=PRESUPUESTO");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("excel-table")));
            Thread.sleep(3000);

            List<WebElement> apuBtns = driver.findElements(By.xpath("//button[@title='Configurar APU']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", apuBtns.get(1));
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("modal-container")));
            Thread.sleep(8000);

            WebElement laborYield = driver.findElement(
                    By.xpath("//label[contains(text(), 'Rendimiento Mano de Obra')]/following-sibling::div/input"));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='40'; arguments[0].dispatchEvent(new Event('input'));", laborYield);

            WebElement resourceSelect = driver.findElement(
                    By.xpath("//label[contains(text(), 'Seleccionar del Catálogo')]/following-sibling::select"));
            new Select(resourceSelect).selectByIndex(1);

            WebElement btnAgregar = driver.findElement(By.xpath("//button[contains(., 'Agregar')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnAgregar);
            Thread.sleep(8000);

            WebElement squadInput = driver
                    .findElement(By.xpath("//td/input[contains(@class, 'cell-input') and not(@disabled)]"));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='2'; arguments[0].dispatchEvent(new Event('input'));", squadInput);

            WebElement btnGuardarApu = driver.findElement(By.xpath("//button[contains(., 'Guardar APU')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnGuardarApu);
            Thread.sleep(8000);
            WebElement btnGuardarPto = driver.findElement(By.xpath("//button[contains(., 'Guardar Presupuesto')]"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnGuardarPto);
            Thread.sleep(8000);
            System.out.println("Paso 1: APU configurado exitosamente");

            driver.get("https://retrofit-nxaez.ondigitalocean.app/portafolio/proyecto/106/daily-report");
            wait.until(ExpectedConditions.presenceOfElementLocated(By.className("report-layout")));
            Thread.sleep(8000);
            System.out.println("Paso 2: Pantalla de Registro de Avance cargada");

            WebElement dateInput = driver.findElement(By.xpath("//input[@formControlName='reportDate']"));
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].value='2026-07-16';" +
                            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                            "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                    dateInput);

            WebElement selectElement = driver.findElement(By.xpath("//select[@formControlName='projectItemId']"));
            Select itemSelect = new Select(selectElement);
            itemSelect.selectByIndex(1);
            Thread.sleep(8000);
            WebElement quantityInput = driver.findElement(By.xpath("//input[@formControlName='executedQuantity']"));
            quantityInput.sendKeys("20.5");
            WebElement obsInput = driver.findElement(By.xpath("//textarea[@formControlName='observations']"));
            obsInput.sendKeys(
                    "El equipo de terreno avanzó con la excavación de zanjas. Clima favorable y sin contratiempos reportados por el capataz.");

            System.out.println("Paso 3: Formulario de avance diario llenado");
            Thread.sleep(8000);

            WebElement btnEnviar = driver.findElement(By.xpath("//button[@type='submit']"));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btnEnviar);

            System.out.println("Paso 4: Botón de enviar presionado");
            Thread.sleep(8000);

            System.out.println("✅ PRUEBA EXITOSA: El avance físico diario se registró en la obra correctamente.");

        } catch (Exception e) {
            System.out.println("❌ LA PRUEBA FALLÓ: " + e.getMessage());
        } finally {
            driver.quit();
        }
    }
}
