package com.freshflow.api.catalog.application.command;

public record CreateCategoryCommand(String name, String description, Boolean active) {}
