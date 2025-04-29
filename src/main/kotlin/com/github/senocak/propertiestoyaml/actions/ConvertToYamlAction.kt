package com.github.senocak.propertiestoyaml.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.github.senocak.propertiestoyaml.MyBundle
import com.github.senocak.propertiestoyaml.services.MyProjectService
import java.io.File

/**
 * Action to convert a .properties file to YAML format
 * This action appears in the context menu for .properties files
 */
class ConvertToYamlAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        if (virtualFile.extension != "properties") {
            return
        }

        val service = project.service<MyProjectService>()

        try {
            // Convert the properties file to YAML
            val yamlContent = service.convertPropertiesToYaml(File(virtualFile.path))

            // Use the same directory as the properties file
            val fileName = virtualFile.nameWithoutExtension + ".yml"
            val outputFile = File(virtualFile.parent.path, fileName)

            service.saveYamlToFile(yamlContent, outputFile)

            Messages.showInfoMessage(
                project,
                MyBundle.message("conversionToYamlSuccess", outputFile.absolutePath),
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
        // Only enable this action for .properties files
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = virtualFile != null && virtualFile.extension == "properties"
    }
}
