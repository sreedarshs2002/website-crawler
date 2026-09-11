package com.example.demo;

import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CrawlerController {

    private final CrawlerService crawlerService;
    private final AiService aiService;

    public CrawlerController(
            CrawlerService crawlerService,
            AiService aiService) {

        this.crawlerService = crawlerService;
        this.aiService = aiService;
    }

    @GetMapping(value = "/crawl", produces = "text/plain")
    public String crawl(
            @RequestParam String url,
            @RequestParam(defaultValue = "50") int maxPages) {

        Document knowledgeDocument =
                crawlerService.crawlWebsite(url, maxPages);

        return knowledgeDocument.getText();
    }

    @GetMapping(value = "/ask", produces = "text/plain")
    public String ask(
            @RequestParam String url,
            @RequestParam String question,
            @RequestParam(defaultValue = "50") int maxPages) {

        Document knowledgeDocument =
                crawlerService.crawlWebsite(url, maxPages);

        return aiService.ask(
                knowledgeDocument.getText(),
                question
        );
    }
}