package com.github.senocak.propertiestoyaml.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.github.senocak.propertiestoyaml.MyBundle
import com.github.senocak.propertiestoyaml.services.MyProjectService
import java.awt.BorderLayout
import java.awt.GridLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.border.EmptyBorder
import javax.swing.DefaultComboBoxModel

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), MyBundle.message("toolWindowTitle"), false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(private val toolWindow: ToolWindow) {

        private val service = toolWindow.project.service<MyProjectService>()
        private var selectedFile: VirtualFile? = null
        private var conversionMode: ConversionMode = ConversionMode.PROPERTIES_TO_YAML

        private val previewTextArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
        }
        private lateinit var statusLabel: JBLabel
        private lateinit var convertButton: JButton
        private lateinit var selectFileButton: JButton
        private lateinit var modeComboBox: ComboBox<String>

        // Enum to represent conversion modes
        private enum class ConversionMode {
            PROPERTIES_TO_YAML,
            YAML_TO_PROPERTIES
        }

        fun getContent(): JPanel {
            val mainPanel = JBPanel<JBPanel<*>>().apply {
                layout = BorderLayout()
                border = EmptyBorder(10, 10, 10, 10)
            }

            // Create top panel with controls
            val topPanel = JPanel().apply {
                layout = BorderLayout(10, 10)
            }

            // Create mode selection combo box
            val modePanel = JPanel().apply {
                layout = BorderLayout(5, 0)
            }

            modeComboBox = ComboBox<String>().apply {
                model = DefaultComboBoxModel(arrayOf(
                    "Properties to YAML",
                    "YAML to Properties"
                ))
                addActionListener {
                    conversionMode = if (selectedIndex == 0) {
                        ConversionMode.PROPERTIES_TO_YAML
                    } else {
                        ConversionMode.YAML_TO_PROPERTIES
                    }
                    updateFileButtonLabel()
                    selectedFile = null
                    updateUI()
                }
            }
            modePanel.add(JBLabel("Conversion Mode:"), BorderLayout.WEST)
            modePanel.add(modeComboBox, BorderLayout.CENTER)
            topPanel.add(modePanel, BorderLayout.NORTH)

            // Create buttons panel
            val buttonsPanel = JPanel().apply {
                layout = GridLayout(1, 2, 10, 0)
            }

            // Select file button
            selectFileButton = JButton(MyBundle.message("selectPropertiesFile")).apply {
                addActionListener {
                    selectFile()
                }
            }
            buttonsPanel.add(selectFileButton)

            // Convert button
            convertButton = JButton(MyBundle.message("convertToYamlButton")).apply {
                addActionListener {
                    convert()
                }
                isEnabled = false
            }
            buttonsPanel.add(convertButton)
            topPanel.add(buttonsPanel, BorderLayout.CENTER)

            // Add top panel to the main panel
            mainPanel.add(topPanel, BorderLayout.NORTH)

            // Add preview area
            val scrollPane = JBScrollPane(previewTextArea)
            mainPanel.add(scrollPane, BorderLayout.CENTER)

            // Status label
            statusLabel = JBLabel()
            mainPanel.add(statusLabel, BorderLayout.SOUTH)

            return mainPanel
        }

        // Update the file selection button label based on the conversion mode
        private fun updateFileButtonLabel() {
            // Update select file button text
            selectFileButton.text = if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) {
                MyBundle.message("selectPropertiesFile")
            } else {
                MyBundle.message("selectYamlFile")
            }

            // Update convert button text
            convertButton.text = if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) {
                MyBundle.message("convertToYamlButton")
            } else {
                MyBundle.message("convertToPropertiesButton")
            }
        }

        // Update UI when a file is selected
        private fun updateUI() {
            if (selectedFile != null) {
                statusLabel.text = "Selected: ${selectedFile!!.name}"
                convertButton.isEnabled = true

                try {
                    // Show preview based on conversion mode
                    previewTextArea.text = if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) {
                        service.convertPropertiesToYaml(File(selectedFile!!.path))
                    } else {
                        service.convertYamlToProperties(File(selectedFile!!.path)).toString()
                    }
                } catch (e: Exception) {
                    previewTextArea.text = "Error generating preview: ${e.message}"
                    thisLogger().warn("Error generating preview", e)
                }
            } else {
                statusLabel.text = ""
                convertButton.isEnabled = false
                previewTextArea.text = ""
            }
        }

        // File selection function
        private fun selectFile() {
            val fileExtension = if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) "properties" else "yml"
            val title = if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) {
                MyBundle.message("selectPropertiesFile")
            } else {
                MyBundle.message("selectYamlFile")
            }

            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .withTitle(title)
                .withFileFilter { it.extension == fileExtension }

            FileChooser.chooseFile(descriptor, toolWindow.project, null) { file ->
                selectedFile = file
                updateUI()
            }
        }

        // Conversion function
        private fun convert() {
            selectedFile?.let { file: VirtualFile ->
                try {
                    if (conversionMode == ConversionMode.PROPERTIES_TO_YAML) {
                        // Convert properties to YAML
                        val yamlContent = service.convertPropertiesToYaml(File(file.path))
                        val outputFile = File(file.parent.path, "${file.nameWithoutExtension}.yml")
                        service.saveYamlToFile(yamlContent, outputFile)

                        Messages.showInfoMessage(
                            toolWindow.project,
                            MyBundle.message("conversionToYamlSuccess", outputFile.absolutePath),
                            MyBundle.message("toolWindowTitle")
                        )
                    } else {
                        // Convert YAML to properties
                        val properties = service.convertYamlToProperties(File(file.path))
                        val outputFile = File(file.parent.path, "${file.nameWithoutExtension}.properties")
                        service.savePropertiesToFile(properties, outputFile)

                        Messages.showInfoMessage(
                            toolWindow.project,
                            MyBundle.message("conversionToPropertiesSuccess", outputFile.absolutePath),
                            MyBundle.message("toolWindowTitle")
                        )
                    }
                } catch (e: Exception) {
                    Messages.showErrorDialog(
                        toolWindow.project,
                        MyBundle.message("conversionError", e.message ?: "Unknown error"),
                        MyBundle.message("toolWindowTitle")
                    )
                    thisLogger().warn("Error during conversion", e)
                }
            }
        }
    }
}
