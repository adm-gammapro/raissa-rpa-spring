package com.raissa.rpa.service.impl.commons;

import com.raissa.rpa.service.commons.BankingService;
import lombok.RequiredArgsConstructor;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BankingServiceImpl implements BankingService {
    private final WebDriver driver;

    public String testConnection() {
        driver.get("https://www.google.com");
        return "Title: " + driver.getTitle();
    }
}
