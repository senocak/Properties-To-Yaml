package com.github.senocak.propertiestoyaml.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager

class MyProjectActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        // Ensure the tool window is registered and available
        ToolWindowManager.getInstance(project).getToolWindow("PropertiesToYaml")
    }
}
