package com.github.senocak.propertiestoyaml.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.github.senocak.propertiestoyaml.MyBundle
import com.github.senocak.propertiestoyaml.services.MyProjectService
import java.io.File

/**
 * Action to convert a .yml file to Properties format
 * This action appears in the context menu for .yml files
 */
class ConvertToPropertiesAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (virtualFile.extension != "yml" && virtualFile.extension != "yaml") {
            return
        }

        val service = project.service<MyProjectService>()

        try {
            // Convert the YAML file to properties
            val properties = service.convertYamlToProperties(File(virtualFile.path))

            // Use the same directory as the YAML file
            val fileName = virtualFile.nameWithoutExtension + ".properties"
            val outputFile = File(virtualFile.parent.path, fileName)

            service.savePropertiesToFile(properties, outputFile)

            Messages.showInfoMessage(
                project,
                MyBundle.message("conversionToPropertiesSuccess", outputFile.absolutePath),
                MyBundle.message("toolWindowTitle")
            )
        } catch (ex: Exception) {
            Messages.showErrorDialog(
                project,
                MyBundle.message("conversionError", ex.message ?: "Unknown error"),
                MyBundle.message("toolWindowTitle")
            )
        }
    }

    override fun update(e: AnActionEvent) {
        // Only enable this action for .yml and .yaml files
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && 
                (virtualFile.extension == "yml" || virtualFile.extension == "yaml")
    }
}