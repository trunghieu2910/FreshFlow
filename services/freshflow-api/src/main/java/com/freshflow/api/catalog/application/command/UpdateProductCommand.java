package com.freshflow.api.catalog.application.command;

public record UpdateProductCommand(
    String name, String description, String imageUrl, Boolean active) {}
