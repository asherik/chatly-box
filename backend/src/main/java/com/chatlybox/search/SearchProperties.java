package com.chatlybox.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatly.search")
public record SearchProperties(
    String engine,
    String index,
    String meiliUrl,
    String meiliMasterKey,
    String opensearchUrl,
    String opensearchUsername,
    String opensearchPassword) {}
