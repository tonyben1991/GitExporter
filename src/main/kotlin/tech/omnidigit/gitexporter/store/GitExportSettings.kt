package tech.omnidigit.gitexporter.store

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

/*
 * Copyright (c) 2025 Tony Ben
 */

@Service(Service.Level.PROJECT)
@State(
    name = "GitExporterSettings",
    storages = [Storage(value = "gitExporterSettings.xml", roamingType = RoamingType.PER_OS)]
)
class GitExportSettings : PersistentStateComponent<GitExportSettings.State> {

    data class State(
        var targetDir: String = "",
        var startDate: String = "",
        var endDate: String = "",
        var author: String = "",
        var message: String = ""
    )

    private var state = State()

    @Nullable
    override fun getState(): State = state

    override fun loadState(@NotNull state: State) {
        this.state = state
    }

    var targetDir: String
        get() = state.targetDir
        set(value) {
            state.targetDir = value
        }

    var startDate: String
        get() = state.startDate
        set(value) {
            state.startDate = value
        }

    var endDate: String
        get() = state.endDate
        set(value) {
            state.endDate = value
        }

    var author: String
        get() = state.author
        set(value) {
            state.author = value
        }

    var message: String
        get() = state.message
        set(value) {
            state.message = value
        }

    companion object {
        fun getInstance(project: Project): GitExportSettings {
            return project.getService(GitExportSettings::class.java)
        }
    }
}