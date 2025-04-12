package tech.omnidigit.gitexporter.config

/*
 * Copyright (c) 2025 Tony Ben
 */

data class GitExportConfig(
    val targetDir: String,
    val author: String,
    val message: String,
    val startDate: String,
    val endDate: String
)