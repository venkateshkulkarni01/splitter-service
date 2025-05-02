package com.example.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.service.SplitterService;

@RestController
@RequestMapping("/split")
@RequiredArgsConstructor
public class SplitterController {

    private final SplitterService splitterService;

    @GetMapping
    public String splitFile() {
        splitterService.splitAndTriggerJobs();
        return "Splitting Started!";
    }
}

