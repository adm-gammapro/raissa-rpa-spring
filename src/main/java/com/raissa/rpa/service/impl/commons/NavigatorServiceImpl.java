package com.raissa.rpa.service.impl.commons;

import com.raissa.rpa.service.commons.NavigatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NavigatorServiceImpl implements NavigatorService {
    @Value("${app.production:false}")
    private boolean isProduction;

    public WebDriver iniciarNavegador() {
        ChromeDriver driver = null;
        try {
            ChromeOptions options = new ChromeOptions();

            // ── Antidetección ───────────────────────────────────────────────────
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
            options.setExperimentalOption("useAutomationExtension", false);

            // ── Idioma ──────────────────────────────────────────────────────────
            options.addArguments("--lang=es-PE");
            options.addArguments("--accept-lang=es-PE,es;q=0.9");

            // ── User-Agent exacto según Chrome instalado ────────────────────────
            String userAgent = isProduction
                    ? "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.7444.175 Safari/537.36"
                    : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.7632.76 Safari/537.36";
            options.addArguments("user-agent=" + userAgent);

            // ── Perfil persistente (cookies, historial, localStorage) ───────────
            String userDataDir = isProduction
                    ? "/opt/rpa/chrome-profile"
                    : System.getProperty("user.home") + "/rpa-chrome-profile";
            options.addArguments("--user-data-dir=" + userDataDir);
            options.addArguments("--profile-directory=Default");

            // ── Configuración según entorno ─────────────────────────────────────
            if (isProduction) {
                options.addArguments("--window-size=1920,1080");
                options.addArguments("--headless=new");
                options.addArguments("--disable-gpu");
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--font-render-hinting=medium");
                options.addArguments("--disable-features=IsolateOrigins,site-per-process");
            } else {
                options.addArguments("--window-size=1400,1000");
            }

            driver = new ChromeDriver(options);

            // ── Inyectar script ANTES de que cargue cualquier página ────────────
            // Más efectivo que executeScript porque se ejecuta en la creación del documento
            Map<String, Object> cdpParams = new HashMap<>();
            cdpParams.put("source",
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                            "Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3]});" +
                            "Object.defineProperty(navigator, 'languages', {get: () => ['es-PE', 'es']});"
            );
            driver.executeCdpCommand("Page.addScriptToEvaluateOnNewDocument", cdpParams);

            // ── Timeouts ────────────────────────────────────────────────────────
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));

            log.info("Navegador iniciado correctamente [produccion={}]", isProduction);
            return driver;
        } catch (Exception e) {
            log.error("Error al iniciar el navegador: {}", e.getMessage());
            // ── Screenshot de diagnóstico ───────────────────────────────────────
            if (driver != null) {
                try {
                    File screenshot = driver.getScreenshotAs(OutputType.FILE);
                    String screenshotPath = isProduction
                            ? "/opt/rpa/chrome-profile/evidencia_error_init.png"
                            : System.getProperty("user.home") + "/rpa-chrome-profile" + "/rpa-evidencia_error_init.png";
                    java.nio.file.Files.copy(
                            screenshot.toPath(),
                            java.nio.file.Paths.get(screenshotPath),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    );
                    log.error("Evidencia guardada en: {}", screenshotPath);
                } catch (IOException ioEx) {
                    log.error("No se pudo guardar la evidencia: {}", ioEx.getMessage());
                }
                driver.quit();
            }
            throw new RuntimeException("No se pudo iniciar el navegador: " + e.getMessage(), e);
        }
    }
}