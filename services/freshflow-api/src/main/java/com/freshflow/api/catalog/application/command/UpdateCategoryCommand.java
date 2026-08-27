package com.freshflow.api.catalog.application.command;

public record UpdateCategoryCommand(String name, String description, Boolean active) {}
