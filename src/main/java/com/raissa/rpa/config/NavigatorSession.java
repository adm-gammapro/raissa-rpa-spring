package com.raissa.rpa.config;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

public record NavigatorSession(Playwright playwright,
                               BrowserContext context,
                               Page page) implements AutoCloseable {
    @Override
    public void close() {
        if (context != null) {
            context.close();
        }
    }
}