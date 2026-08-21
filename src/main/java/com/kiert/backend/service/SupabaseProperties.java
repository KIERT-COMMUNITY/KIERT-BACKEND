package com.kiert.backend.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kiert.supabase")
public record SupabaseProperties(String url, String bucket, String serviceRoleKey) {}
