package com.github.senocak.propertiestoyaml

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.components.service
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import com.github.senocak.propertiestoyaml.services.MyProjectService
import java.io.File
import java.nio.file.Files

@TestDataPath("\$CONTENT_ROOT/src/test/testData")
class MyPluginTest : BasePlatformTestCase() {

    fun testXMLFile() {
        val psiFile = myFixture.configureByText(XmlFileType.INSTANCE, "<foo>bar</foo>")
        val xmlFile = assertInstanceOf(psiFile, XmlFile::class.java)

        assertFalse(PsiErrorElementUtil.hasErrors(project, xmlFile.virtualFile))

        assertNotNull(xmlFile.rootTag)

        xmlFile.rootTag?.let {
            assertEquals("foo", it.name)
            assertEquals("bar", it.value.text)
        }
    }

    fun testRename() {
        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
    }

    fun testProjectServiceConvertsPropertiesToYaml() {
        val projectService = project.service<MyProjectService>()

        val propertiesFile = File(Files.createTempDirectory("props").toFile(), "application.properties")
        propertiesFile.writeText("server.port=8080\napp.name=demo\n")

        val yaml = projectService.convertPropertiesToYaml(propertiesFile)

        assertTrue(yaml.contains("server:"))
        assertTrue(yaml.contains("port: '8080'"))
        assertTrue(yaml.contains("app:"))
        assertTrue(yaml.contains("name: demo"))
    }

    fun testProjectServiceConvertsPropertiesWithScalarAndNestedKeyConflict() {
        val projectService = project.service<MyProjectService>()

        val propertiesFile = File(Files.createTempDirectory("props").toFile(), "application.properties")
        propertiesFile.writeText(
            """
            logging.file=
            logging.file.max-history=0
            logging.file.max-size=10MB
            server.port=8080
            """.trimIndent()
        )

        val yaml = projectService.convertPropertiesToYaml(propertiesFile)

        // YAML cannot hold a value and nested keys under the same name; the nested structure wins
        assertTrue(yaml.contains("logging:"))
        assertTrue(yaml.contains("max-history: '0'"))
        assertTrue(yaml.contains("max-size: 10MB"))
        assertTrue(yaml.contains("server:"))
        assertTrue(yaml.contains("port: '8080'"))
    }

    fun testProjectServiceConvertsYamlToProperties() {
        val projectService = project.service<MyProjectService>()

        val yamlFile = File(Files.createTempDirectory("yaml").toFile(), "application.yml")
        yamlFile.writeText("server:\n  port: 8080\napp:\n  name: demo\n")

        val properties = projectService.convertYamlToProperties(yamlFile)

        assertEquals("server.port=8080\napp.name=demo\n", properties)
    }

    fun testProjectServiceRendersNestedBranchesWithoutFlowStyleBraces() {
        val projectService = project.service<MyProjectService>()

        val propertiesFile = File(Files.createTempDirectory("props").toFile(), "application.properties")
        propertiesFile.writeText("spring.rabbitmq.password=guest # Login to authenticate against the broker.\n")

        val yaml = projectService.convertPropertiesToYaml(propertiesFile)

        assertEquals(
            """
            spring:
              rabbitmq:
                password: guest # Login to authenticate against the broker.

            """.trimIndent(),
            yaml
        )
    }

    fun testProjectServicePreservesCommentsWhenConvertingPropertiesToYaml() {
        val projectService = project.service<MyProjectService>()

        val propertiesFile = File(Files.createTempDirectory("props").toFile(), "application.properties")
        propertiesFile.writeText(
            """
            # CORE SECTION
            server.port=8080 # HTTP port
            logging.config= # Location of the config
            """.trimIndent()
        )

        val yaml = projectService.convertPropertiesToYaml(propertiesFile)

        assertTrue(yaml.contains("# CORE SECTION"))
        assertTrue(yaml.indexOf("# CORE SECTION") < yaml.indexOf("server:"))
        assertTrue(yaml.contains("port: '8080' # HTTP port"))
        assertTrue(yaml.contains("config: '' # Location of the config"))
    }

    fun testProjectServicePreservesCommentsWhenConvertingYamlToProperties() {
        val projectService = project.service<MyProjectService>()

        val yamlFile = File(Files.createTempDirectory("yaml").toFile(), "application.yml")
        yamlFile.writeText(
            """
            # global header
            server:
              # port comment
              port: 8080 # HTTP port
            app:
              name: demo
            """.trimIndent()
        )

        val properties = projectService.convertYamlToProperties(yamlFile)

        assertEquals(
            """
            # global header
            # port comment
            server.port=8080 # HTTP port
            app.name=demo

            """.trimIndent(),
            properties
        )
    }

    fun testProjectServiceConvertsMultiDocumentYamlToProperties() {
        val projectService = project.service<MyProjectService>()

        val yamlFile = File(Files.createTempDirectory("yaml").toFile(), "application.yml")
        yamlFile.writeText(
            """
            server:
              port: 9000
            ---
            spring:
              config:
                activate:
                  on-profile: "development"
            server:
              port: 9001
            ---
            spring:
              config:
                activate:
                  on-profile: "production"
            server:
              port: 0
            """.trimIndent()
        )

        val properties = projectService.convertYamlToProperties(yamlFile)

        assertEquals(
            """
            server.port=9000
            #---
            spring.config.activate.on-profile=development
            server.port=9001
            #---
            spring.config.activate.on-profile=production
            server.port=0

            """.trimIndent(),
            properties
        )
    }

    override fun getTestDataPath() = "src/test/testData/rename"
}
