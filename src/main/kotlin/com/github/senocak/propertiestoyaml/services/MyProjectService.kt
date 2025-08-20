package com.github.senocak.propertiestoyaml.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.senocak.propertiestoyaml.MyBundle
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.util.Properties

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    /**
     * Converts a properties file to YAML format
     * 
     * @param propertiesFile The properties file to convert
     * @return The YAML content as a string
     */
    fun convertPropertiesToYaml(propertiesFile: File): String {
        val properties = Properties()
        FileInputStream(propertiesFile).use { properties.load(it) }

        // Convert flat properties to hierarchical map
        val map = propertiesToHierarchicalMap(properties)

        // Configure YAML output options
        val options = DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            isPrettyFlow = true
        }

        // Convert to YAML
        val yaml = Yaml(options)
        return yaml.dump(map)
    }

    /**
     * Saves YAML content to a file
     * 
     * @param yamlContent The YAML content to save
     * @param outputFile The file to save to
     */
    fun saveYamlToFile(yamlContent: String, outputFile: File) {
        // Ensure parent directory exists
        outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        // Write to disk
        FileWriter(outputFile).use { it.write(yamlContent) }
        // Refresh IntelliJ VFS so the new file appears immediately
        try {
            val localFs = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            val vf = localFs.refreshAndFindFileByIoFile(outputFile)
            if (vf != null) {
                vf.refresh(false, false)
            } else {
                localFs.refreshAndFindFileByIoFile(outputFile.parentFile)?.refresh(false, true)
            }
        } catch (_: Throwable) {
            // ignore refresh errors
        }
    }

    /**
     * Converts a YAML file to Properties format
     * 
     * @param yamlFile The YAML file to convert
     * @return The Properties object
     */
    fun convertYamlToProperties(yamlFile: File): Properties {
        val yaml = Yaml()
        val properties = Properties()

        FileReader(yamlFile).use { reader ->
            val yamlMap = yaml.load(reader) as? Map<String, Any> ?: emptyMap()
            val flattenedMap = flattenYamlMap(yamlMap)

            for ((key, value) in flattenedMap) {
                properties[key] = value.toString()
            }
        }

        return properties
    }

    /**
     * Saves Properties to a file
     * 
     * @param properties The Properties to save
     * @param outputFile The file to save to
     */
    fun savePropertiesToFile(properties: Properties, outputFile: File) {
        // Ensure parent directory exists
        outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        // Write to disk
        FileOutputStream(outputFile).use {
            properties.store(it, "Generated from YAML")
        }
        // Refresh IntelliJ VFS so the new file appears immediately
        try {
            val localFs = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
            val vf = localFs.refreshAndFindFileByIoFile(outputFile)
            if (vf != null) {
                vf.refresh(false, false)
            } else {
                localFs.refreshAndFindFileByIoFile(outputFile.parentFile)?.refresh(false, true)
            }
        } catch (_: Throwable) {
            // ignore refresh errors
        }
    }

    /**
     * Converts flat properties to a hierarchical map
     * 
     * @param properties The properties to convert
     * @return A hierarchical map
     */
    private fun propertiesToHierarchicalMap(properties: Properties): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        for ((key, value) in properties) {
            val keyStr = key.toString()
            val parts = keyStr.split(".")

            var current = result
            for (i in 0 until parts.size - 1) {
                val part = parts[i]
                if (!current.containsKey(part)) {
                    current[part] = mutableMapOf<String, Any>()
                }

                @Suppress("UNCHECKED_CAST")
                current = current[part] as MutableMap<String, Any>
            }

            current[parts.last()] = value
        }

        return result
    }

    /**
     * Flattens a hierarchical YAML map to a flat map with dot-separated keys
     * 
     * @param yamlMap The hierarchical map from YAML
     * @param prefix The current key prefix (used for recursion)
     * @return A flattened map with dot-separated keys
     */
    private fun flattenYamlMap(yamlMap: Map<String, Any>, prefix: String = ""): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        for ((key, value) in yamlMap) {
            val newKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    result.putAll(flattenYamlMap(value as Map<String, Any>, newKey))
                }
                else -> result[newKey] = value
            }
        }

        return result
    }
}
