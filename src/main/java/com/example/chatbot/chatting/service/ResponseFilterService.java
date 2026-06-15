package com.example.chatbot.chatting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResponseFilterService {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFilterService.class);
    private static final Pattern IMAGE_TAG_PATTERN = Pattern.compile("<IMG>(.*?)<IMG>", Pattern.CASE_INSENSITIVE);

    private final ResourceLoader resourceLoader;

    public ResponseFilterService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String sanitizeImageTags(String text) {
        text = text.replace("\r\n", "\n").replace("\r", "\n");
        Matcher matcher = IMAGE_TAG_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fileName = matcher.group(1).trim();
            if (!fileName.isEmpty() && imageExists(fileName)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("<IMG>" + fileName + "<IMG>"));
            } else {
                logger.info("이미지 없음, 제거 처리: {}", fileName);
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private boolean imageExists(String filename) {
        try {
            Resource resource = resourceLoader.getResource("classpath:/static/assets/images/" + filename);
            return resource.exists();
        } catch (Exception e) {
            return false;
        }
    }
}
