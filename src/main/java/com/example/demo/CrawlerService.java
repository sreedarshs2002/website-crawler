package com.example.demo;

import java.time.Duration;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.ai.document.Document;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;

@Service
public class CrawlerService {

	
	
	
	
	private void discoverButtonLinks(
	        String url,
	        Queue<String> queue,
	        Set<String> visited,
	        String domain) {

		ChromeOptions options = new ChromeOptions();

		options.setBinary("/usr/bin/chromium-browser");

		options.addArguments("--headless=new");
		options.addArguments("--no-sandbox");
		options.addArguments("--disable-dev-shm-usage");
		options.addArguments("--disable-gpu");
		options.addArguments("--remote-allow-origins=*");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-software-rasterizer");

		// Add these
		options.addArguments("--disable-setuid-sandbox");
		options.addArguments("--no-zygote");
		options.addArguments("--enable-logging");
		options.addArguments("--log-level=0");

		System.setProperty(
		        "webdriver.chrome.driver",
		        "/usr/bin/chromedriver"
		);

		// Debug information
		System.out.println("===== SELENIUM DEBUG START =====");

		System.out.println("STEP 1");

		System.out.println("Chromium path check starting");

		java.io.File chromium =
		        new java.io.File("/usr/bin/chromium-browser");

		System.out.println("Chromium exists: " + chromium.exists());

		System.out.println("ChromeDriver path check starting");

		java.io.File chromedriver =
		        new java.io.File("/usr/bin/chromedriver");

		System.out.println("ChromeDriver exists: " + chromedriver.exists());

		System.out.println("STEP 2");

		System.out.println("Creating ChromeDriver...");

		WebDriver driver = new ChromeDriver(options);

		System.out.println("ChromeDriver CREATED SUCCESSFULLY");

		System.out.println("===== SELENIUM DEBUG END =====");

	    try {

	        System.out.println("================================");
	        System.out.println("DISCOVERING BUTTON LINKS");
	        System.out.println("URL: " + url);
	        System.out.println("================================");

	        driver.get(url);

	        WebDriverWait wait =
	                new WebDriverWait(driver, Duration.ofSeconds(10));

	        wait.until(
	                ExpectedConditions.presenceOfElementLocated(
	                        By.tagName("body")
	                )
	        );

	        Thread.sleep(2000);

	        /*
	         * Find:
	         * 1. Real <button> elements
	         * 2. Elements with role="button"
	         * 3. <a> elements that may behave like buttons
	         */
	        List<WebElement> elements = driver.findElements(
	                By.cssSelector(
	                        "button, [role='button'], a"
	                )
	        );

	        System.out.println(
	                "Potential clickable elements found: "
	                        + elements.size()
	        );

	        for (int i = 0; i < elements.size(); i++) {

	            try {

	                /*
	                 * Reload the original page before checking
	                 * the next element.
	                 */
	                driver.get(url);

	                wait.until(
	                        ExpectedConditions.presenceOfElementLocated(
	                                By.tagName("body")
	                        )
	                );

	                Thread.sleep(1000);

	                /*
	                 * Get the elements again because after
	                 * driver.get(), old WebElement objects are
	                 * no longer reliable.
	                 */
	                List<WebElement> currentElements =
	                        driver.findElements(
	                                By.cssSelector(
	                                        "button, [role='button'], a"
	                                )
	                        );

	                if (i >= currentElements.size()) {
	                    continue;
	                }

	                WebElement element = currentElements.get(i);

	                String text = element.getText();

	                if (text == null || text.trim().isEmpty()) {
	                    continue;
	                }

	                text = text.trim();

	                String lowerText = text.toLowerCase();

	                /*
	                 * Only click navigation-type elements.
	                 */
	                boolean navigationElement =
	                        lowerText.contains("get started")
	                        || lowerText.contains("login")
	                        || lowerText.contains("log in")
	                        || lowerText.contains("sign in")
	                        || lowerText.contains("sign up")
	                        || lowerText.contains("register");

	                if (!navigationElement) {
	                    continue;
	                }

	                System.out.println(
	                        "Trying clickable element: " + text
	                );

	                /*
	                 * Remember the original browser window.
	                 */
	                String originalWindow =
	                        driver.getWindowHandle();

	                Set<String> windowsBefore =
	                        driver.getWindowHandles();

	                /*
	                 * Click the element.
	                 */
	                element.click();

	                Thread.sleep(2000);

	                /*
	                 * Check whether a new tab/window was opened.
	                 */
	                Set<String> windowsAfter =
	                        driver.getWindowHandles();

	                String targetUrl = driver.getCurrentUrl();

	                /*
	                 * If a new tab/window appeared,
	                 * switch to it.
	                 */
	                if (windowsAfter.size() > windowsBefore.size()) {

	                    for (String window : windowsAfter) {

	                        if (!windowsBefore.contains(window)) {

	                            driver.switchTo().window(window);

	                            targetUrl =
	                                    driver.getCurrentUrl();

	                            break;
	                        }
	                    }
	                }

	                /*
	                 * Remove URL fragments.
	                 *
	                 * Example:
	                 * https://example.com/page#section
	                 *
	                 * becomes:
	                 * https://example.com/page
	                 */
	                if (targetUrl != null) {

	                    targetUrl =
	                            targetUrl.split("#")[0];

	                    System.out.println(
	                            "Button destination: "
	                                    + targetUrl
	                    );

	                    /*
	                     * Only add valid HTTP/HTTPS URLs.
	                     */
	                    if (targetUrl.startsWith("http://")
	                            || targetUrl.startsWith("https://")) {

	                        /*
	                         * Only allow same-domain pages.
	                         */
	                        if (getDomain(targetUrl)
	                                .equals(domain)) {

	                            if (!visited.contains(targetUrl)
	                                    && !queue.contains(targetUrl)) {

	                                queue.add(targetUrl);

	                                System.out.println(
	                                        "Added button URL: "
	                                                + targetUrl
	                                );
	                            }
	                        }
	                    }
	                }

	                /*
	                 * Close the newly opened tab/window
	                 * if one was created.
	                 */
	                if (windowsAfter.size() > windowsBefore.size()) {

	                    driver.close();

	                    /*
	                     * Switch back to original window.
	                     */
	                    driver.switchTo()
	                            .window(originalWindow);
	                }

	            } catch (Exception e) {

	                System.out.println(
	                        "Could not process clickable element: "
	                                + e.getMessage()
	                );
	            }
	        }

	        System.out.println(
	                "Queue after button discovery: "
	                        + queue.size()
	        );

	    } catch (Exception e) {

	        System.out.println(
	                "Button discovery failed: "
	                        + e.getMessage()
	        );

	    } finally {

	        driver.quit();

	        System.out.println(
	                "Button discovery browser closed."
	        );
	    }
	}
	
	
	

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
	                            .userAgent(
	                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
	                                "AppleWebKit/537.36 (KHTML, like Gecko) " +
	                                "Chrome/140.0.0.0 Safari/537.36"
	                            )
	                            .referrer("https://www.google.com/")
	                            .header(
	                                "Accept",
	                                "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
	                            )
	                            .header(
	                                "Accept-Language",
	                                "en-US,en;q=0.9"
	                            )
	                            .timeout(20000)
	                            .followRedirects(true)
	                            .get();
	            
	            
	            


	         // ==========================================
	         // FIND LINKS USING JSOUP
	         // ==========================================

	         System.out.println("================================");
	         System.out.println("LINK DISCOVERY FOR: " + url);

	         for (org.jsoup.nodes.Element link : html.select("a[href]")) {

	             String nextUrl = link.absUrl("href");

	             if (nextUrl.isEmpty()) {
	                 continue;
	             }

	             // Remove #section
	             nextUrl = nextUrl.split("#")[0];

	             if (nextUrl.isEmpty()) {
	                 continue;
	             }

	             // Only HTTP/HTTPS
	             if (!nextUrl.startsWith("http")) {
	                 continue;
	             }

	             // Only same-domain links
	             if (!getDomain(nextUrl).equals(domain)) {
	                 continue;
	             }

	             // Don't crawl already visited pages
	             if (visited.contains(nextUrl)) {
	                 continue;
	             }

	             // Don't add duplicate queue entries
	             if (queue.contains(nextUrl)) {
	                 continue;
	             }

	             System.out.println("ADDING TO QUEUE: " + nextUrl);
	             queue.add(nextUrl);
	         }

	         int pagesNeeded = maxPages - visited.size();

	         System.out.println("Pages already visited: " + visited.size());
	         System.out.println("Pages waiting in queue: " + queue.size());
	         System.out.println("Pages still needed: " + pagesNeeded);
	         System.out.println("Maximum pages requested: " + maxPages);
	         System.out.println("================================");

	         // ==========================================
	         // USE SELENIUM IF JSOUP DOES NOT FIND ENOUGH
	         // ==========================================

	         if (queue.size() < pagesNeeded) {

	             System.out.println("Jsoup did not find enough links.");
	             System.out.println("Pages still needed: " + pagesNeeded);
	             System.out.println("Pages available in queue: " + queue.size());
	             System.out.println("Trying Selenium...");

	             String renderedHtml = getRenderedHtml(url);

	             if (!renderedHtml.isEmpty()) {

	                 html = org.jsoup.Jsoup.parse(renderedHtml, url);

	                 System.out.println("Selenium HTML parsed by Jsoup.");

	                 System.out.println(
	                     "Links after Selenium rendering: "
	                     + html.select("a[href]").size()
	                 );

	                 // ==========================================
	                 // DISCOVER LINKS FROM SELENIUM HTML
	                 // ==========================================

	                 for (org.jsoup.nodes.Element link :
	                         html.select("a[href]")) {

	                     String nextUrl = link.absUrl("href");

	                     System.out.println("SELENIUM FOUND LINK: " + nextUrl);

	                     if (nextUrl.isEmpty()) {
	                         continue;
	                     }

	                     // Remove #section
	                     nextUrl = nextUrl.split("#")[0];

	                     if (nextUrl.isEmpty()) {
	                         continue;
	                     }

	                     // Only HTTP/HTTPS
	                     if (!nextUrl.startsWith("http")) {
	                         continue;
	                     }

	                     // Only same-domain links
	                     if (!getDomain(nextUrl).equals(domain)) {
	                         continue;
	                     }

	                     // Already visited
	                     if (visited.contains(nextUrl)) {
	                         continue;
	                     }

	                     // Already in queue
	                     if (queue.contains(nextUrl)) {
	                         continue;
	                     }

	                     System.out.println(
	                         "SELENIUM ADDING TO QUEUE: " + nextUrl
	                     );

	                     queue.add(nextUrl);
	                 }

	                 System.out.println(
	                     "Queue after Selenium: " + queue.size()
	                 );
	                 
	                 
	              // ==========================================
	              // CHECK NAVIGATION BUTTONS
	              // ==========================================
	              if (queue.size() < pagesNeeded) {

	                  discoverButtonLinks(
	                          url,
	                          queue,
	                          visited,
	                          domain
	                  );

	                  System.out.println(
	                          "Queue after button discovery: " + queue.size()
	                  );
	              }
	                 
	             }
	         }
	         

	         
	        

	            // ==========================================
	            // NOW REMOVE UNWANTED HTML
	            // ==========================================

	            html.select(
	                    "script, style, noscript, nav, footer, header, aside, form, " +
	                    "iframe, canvas, svg, " +
	                    ".ad, .ads, .advertisement, .advert, " +
	                    ".cookie, .cookies, .cookie-banner, " +
	                    ".popup, .modal, " +
	                    ".sidebar, .social-share"
	            ).remove();

	            // ==========================================
	            // EXTRACT CLEAN CONTENT
	            // ==========================================

	            String structuredContent =
	                    extractStructuredContent(html, url);


	            // Create Spring AI Document
	            Document document = new Document(structuredContent);

	            document.getMetadata().put("title", html.title());
	            document.getMetadata().put("url", url);

	            documents.add(document);

	        } catch (Exception e) {

	            System.out.println("================================");
	            System.out.println("FAILED TO CRAWL");
	            System.out.println("URL: " + url);
	            System.out.println("Error Type: " + e.getClass().getName());
	            System.out.println("Error Message: " + e.getMessage());
	            System.out.println("================================");
	        }
	    }

	    System.out.println("Crawling completed.");
	    System.out.println("Pages visited: " + visited.size());

	    return createKnowledgeDocument(documents, startUrl);
	}


	
	private String getRenderedHtml(String url) {

	    ChromeOptions options = new ChromeOptions();

	    options.setBinary("/usr/bin/chromium-browser");

	    options.addArguments("--headless=new");
	    options.addArguments("--no-sandbox");
	    options.addArguments("--disable-dev-shm-usage");
	    options.addArguments("--disable-gpu");
	    options.addArguments("--remote-allow-origins=*");
	    options.addArguments("--disable-extensions");
	    options.addArguments("--disable-software-rasterizer");
	    options.addArguments("--disable-setuid-sandbox");
	    options.addArguments("--no-zygote");

	    options.addArguments("--enable-logging");
	    options.addArguments("--log-level=0");

	    System.setProperty(
	            "webdriver.chrome.driver",
	            "/usr/bin/chromedriver"
	    );

	    WebDriver driver = null;

	    try {

	        System.out.println("===== SELENIUM DEBUG =====");

	        System.out.println("Chromium exists: " +
	                new java.io.File(
	                        "/usr/bin/chromium-browser"
	                ).exists());

	        System.out.println("ChromeDriver exists: " +
	                new java.io.File(
	                        "/usr/bin/chromedriver"
	                ).exists());

	        System.out.println("==========================");

	        System.out.println("Creating ChromeDriver...");

	        driver = new ChromeDriver(options);

	        System.out.println("ChromeDriver CREATED SUCCESSFULLY");

	        System.out.println("================================");
	        System.out.println("USING SELENIUM");
	        System.out.println("URL: " + url);
	        System.out.println("================================");

	        driver.get(url);

	        WebDriverWait wait =
	                new WebDriverWait(
	                        driver,
	                        Duration.ofSeconds(10)
	                );

	        wait.until(
	                ExpectedConditions.presenceOfElementLocated(
	                        By.tagName("body")
	                )
	        );

	        // Give JavaScript time to finish rendering
	        Thread.sleep(2000);

	        String renderedHtml =
	                driver.getPageSource();

	        System.out.println(
	                "Rendered HTML size: "
	                + renderedHtml.length()
	        );

	        System.out.println(
	                "Rendered links: "
	                + driver.findElements(
	                        By.cssSelector("a[href]")
	                ).size()
	        );

	        return renderedHtml;

	    } catch (Exception e) {

	        System.out.println(
	                "================================"
	        );

	        System.out.println(
	                "SELENIUM FAILED"
	        );

	        System.out.println(
	                "Error Type: "
	                + e.getClass().getName()
	        );

	        System.out.println(
	                "Error Message: "
	                + e.getMessage()
	        );

	        System.out.println(
	                "================================"
	        );

	        e.printStackTrace();

	        return "";

	    } finally {

	        if (driver != null) {
	            driver.quit();

	            System.out.println(
	                    "Selenium browser closed."
	            );
	        }
	    }
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

            String host = uri.getHost();

            if (host == null) {
                return "";
            }

            host = host.toLowerCase();

            // Treat www.example.com and example.com as the same domain
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }

            return host;

        } catch (Exception e) {

            return "";
        }
    }
}