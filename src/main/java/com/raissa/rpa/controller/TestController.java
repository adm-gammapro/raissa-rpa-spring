package com.raissa.rpa.controller;

import com.raissa.rpa.service.BankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final BankingService bankingService;

    @GetMapping("/test")
    public String test() {
        return bankingService.testConnection();
    }
}
