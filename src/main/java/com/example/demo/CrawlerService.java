package com.example.demo;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class CrawlerService {


    public Document crawlWebsite(String startUrl, int maxPages) {

        List<Document> documents = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        queue.add(startUrl);

        String domain = getDomain(startUrl);

        while (!queue.isEmpty() && visited.size() < maxPages) {

            String url = queue.poll();

            if (visited.contains(url)) {
                continue;
            }

            visited.add(url);

            try {

                System.out.println("Crawling: " + url);

                // Fetch webpage
                org.jsoup.nodes.Document html =
                        org.jsoup.Jsoup.connect(url)
                                .userAgent("Mozilla/5.0")
                                .timeout(10000)
                                .get();

             // Remove unwanted HTML
                html.select(
                        "script, style, noscript, nav, footer, header, aside, form, " +
                        "iframe, canvas, svg, " +
                        ".ad, .ads, .advertisement, .advert, " +
                        ".cookie, .cookies, .cookie-banner, " +
                        ".popup, .modal, " +
                        ".sidebar, .social-share"
                ).remove();

                // Structure the content
                String structuredContent =
                        extractStructuredContent(html, url);

                // Create Spring AI Document
                Document document = new Document(structuredContent);

                document.getMetadata().put("title", html.title());
                document.getMetadata().put("url", url);

                documents.add(document);

                // Find links for crawling
                for (org.jsoup.nodes.Element link : html.select("a[href]")) {

                    String nextUrl = link.absUrl("href");

                    if (nextUrl.isEmpty()) {
                        continue;
                    }

                    // Remove #section
                    nextUrl = nextUrl.split("#")[0];

                    // Only crawl same-domain links
                    if (nextUrl.startsWith("http")
                            && getDomain(nextUrl).equals(domain)
                            && !visited.contains(nextUrl)
                            && !queue.contains(nextUrl)) {

                        queue.add(nextUrl);
                    }
                }

            } catch (Exception e) {

                System.out.println("Failed to crawl: " + url);
                System.out.println("Reason: " + e.getMessage());
            }
        }

        System.out.println("Crawling completed.");
        System.out.println("Pages visited: " + visited.size());

        return createKnowledgeDocument(documents, startUrl);
    }


    private String extractStructuredContent(
            org.jsoup.nodes.Document html,
            String url) {

        StringBuilder content = new StringBuilder();

        // Page information
        content.append("Page: ")
                .append(html.title())
                .append("\n");

        content.append("URL: ")
                .append(url)
                .append("\n\n");

        

        // Headings, paragraphs, lists and tables
        for (org.jsoup.nodes.Element element :
            html.body().select("h1, h2, h3, h4, h5, h6, p, ul, ol, table")) {

            String tag = element.tagName();

            // Headings
            if (tag.matches("h[1-6]")) {

                int level =
                        Integer.parseInt(tag.substring(1));

                content.append("#".repeat(level))
                        .append(" ")
                        .append(element.text())
                        .append("\n\n");
            }

            // Paragraphs
            else if (tag.equals("p")) {

                String text = element.text().trim();

                if (!text.isEmpty()) {
                    content.append(text)
                            .append("\n\n");
                }
            }

            // Lists
            else if (tag.equals("ul") || tag.equals("ol")) {

                int number = 1;

                for (org.jsoup.nodes.Element li :
                        element.children()) {

                    if (li.tagName().equals("li")) {

                        if (tag.equals("ol")) {
                            content.append(number++)
                                    .append(". ");
                        } else {
                            content.append("- ");
                        }

                        content.append(li.text())
                                .append("\n");
                    }
                }

                content.append("\n");
            }

            // Tables
            else if (tag.equals("table")) {

                for (org.jsoup.nodes.Element row :
                        element.select("tr")) {

                    content.append(
                            String.join(
                                    " | ",
                                    row.select("th, td").eachText()
                            )
                    );

                    content.append("\n");
                }

                content.append("\n");
            }
            
        }
            content.append("Media:\n");

            Set<String> imageUrls = new LinkedHashSet<>();

            for (org.jsoup.nodes.Element img : html.select("img")) {

                String imageUrl = img.absUrl("src");

                if (imageUrl.isEmpty()) {
                    imageUrl = img.absUrl("data-src");
                }

                if (imageUrl.isEmpty()) {
                    imageUrl = img.absUrl("data-lazy-src");
                }

                if (!imageUrl.isEmpty() && imageUrl.startsWith("http")) {

                    imageUrls.add(imageUrl);
                }
            }

            for (String imageUrl : imageUrls) {

                content.append("Image: ")
                        .append(imageUrl)
                        .append("\n");
            }

         // Videos
            Set<String> videoUrls = new LinkedHashSet<>();

            for (org.jsoup.nodes.Element video :
                    html.select("video[src], video source[src]")) {

                String videoUrl = video.absUrl("src");

                if (!videoUrl.isEmpty() && videoUrl.startsWith("http")) {

                    videoUrls.add(videoUrl);
                }
            }

            for (String videoUrl : videoUrls) {

                content.append("Video: ")
                        .append(videoUrl)
                        .append("\n");
            }
            
            
         // Documents
            Set<String> documentUrls = new LinkedHashSet<>();

            for (org.jsoup.nodes.Element link :
                    html.select("a[href]")) {

                String documentUrl = link.absUrl("href");

                if (documentUrl.endsWith(".pdf")
                        || documentUrl.endsWith(".doc")
                        || documentUrl.endsWith(".docx")
                        || documentUrl.endsWith(".xls")
                        || documentUrl.endsWith(".xlsx")
                        || documentUrl.endsWith(".ppt")
                        || documentUrl.endsWith(".pptx")) {

                    documentUrls.add(documentUrl);
                }
            }

            for (String documentUrl : documentUrls) {

                content.append("Document: ")
                        .append(documentUrl)
                        .append("\n");
            }
            
            
            content.append("\n");
            
        
            
         // Links
            content.append("Related Links:\n\n");

            Set<String> internalLinks = new LinkedHashSet<>();
            Set<String> externalLinks = new LinkedHashSet<>();

            for (org.jsoup.nodes.Element link : html.select("a[href]")) {

                String linkUrl = link.absUrl("href");
                String linkText = link.text().trim();
                
                if (linkText.length() > 100) {
                    linkText = linkText.substring(0, 100) + "...";
                }

                // Ignore invalid links
                if (linkUrl.isEmpty()
                        || !linkUrl.startsWith("http")
                        || linkUrl.startsWith("javascript:")
                        || linkUrl.startsWith("mailto:")
                        || linkUrl.startsWith("tel:")) {
                    continue;
                }

                // Remove #section
                linkUrl = linkUrl.split("#")[0];

                if (linkUrl.isEmpty()) {
                    continue;
                }

                // Ignore links without useful text
                if (linkText.isEmpty()) {
                    linkText = linkUrl;
                }

                String linkData = linkText + " → " + linkUrl;

                // Separate internal and external links
                if (getDomain(linkUrl).equals(getDomain(url))) {

                    internalLinks.add(linkData);

                } else {

                    externalLinks.add(linkData);
                }
            }

            // Internal links
            if (!internalLinks.isEmpty()) {

                content.append("Internal Links:\n");

                for (String link : internalLinks) {

                    content.append("- ")
                            .append(link)
                            .append("\n");
                }

                content.append("\n");
            }

            // External links
            if (!externalLinks.isEmpty()) {

                content.append("External Links:\n");

                for (String link : externalLinks) {

                    content.append("- ")
                            .append(link)
                            .append("\n");
                }

                content.append("\n");
            }

            return content.toString();
            

       
    }


    
    private Document createKnowledgeDocument(
            List<Document> documents,
            String startUrl) {

        StringBuilder knowledge = new StringBuilder();

        knowledge.append("========================================\n");
        knowledge.append("          WEBSITE INFORMATION\n");
        knowledge.append("========================================\n\n");

        knowledge.append("Website: ")
                .append(startUrl)
                .append("\n\n");

        knowledge.append("Total Pages Crawled: ")
                .append(documents.size())
                .append("\n\n");

        for (int i = 0; i < documents.size(); i++) {

            Document document = documents.get(i);

            knowledge.append("----------------------------------------\n");
            knowledge.append("Page ")
                    .append(i + 1)
                    .append("\n");
            knowledge.append("----------------------------------------\n\n");

            knowledge.append(document.getText())
                    .append("\n\n");
        }

        return new Document(knowledge.toString());
    }
    
    
    private String getDomain(String url) {

        try {

            URI uri = new URI(url);

            return uri.getHost();

        } catch (Exception e) {

            return "";
        }
    }
}