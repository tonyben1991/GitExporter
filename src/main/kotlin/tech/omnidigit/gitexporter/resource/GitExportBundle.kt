package tech.omnidigit.gitexporter.resource

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/*
 * Copyright (c) 2025 Tony Ben
 */

@NonNls
private const val BUNDLE_NAME = "messages.GitExportBundle"

object GitExportBundle : DynamicBundle(BUNDLE_NAME) {
    fun message(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String, vararg params: Any): String {
        return getMessage(key, *params)
    }
}