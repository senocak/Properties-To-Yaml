package com.github.senocak.propertiestoyaml.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.senocak.propertiestoyaml.MyBundle
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileWriter
import java.io.StringReader

@Service(Service.Level.PROJECT)
class MyProjectService(project: Project) {

    init {
        thisLogger().info(MyBundle.message("projectService", project.name))
    }

    /**
     * Converts a properties file to YAML format, preserving comments.
     * Full-line comments are attached to the shallowest node their entry creates;
     * trailing ` # ...` comments are kept as inline comments on the generated entry.
     *
     * @param propertiesFile The properties file to convert
     * @return The YAML content as a string
     */
    fun convertPropertiesToYaml(propertiesFile: File): String {
        val root = YamlNode()
        var pendingComments: MutableList<String>? = null

        for (logical in parsePropertiesLines(propertiesFile.readText())) {
            if (logical.isComment) {
                if (pendingComments == null) pendingComments = mutableListOf()
                pendingComments.add(logical.text)
                continue
            }
            val parts = logical.key.split(".").filter { it.isNotEmpty() }
            if (parts.isEmpty()) {
                pendingComments = null
                continue
            }
            insertEntry(root, parts, logical.value, logical.comment, pendingComments)
            pendingComments = null
        }

        val dumper = Yaml(
            DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                isPrettyFlow = true
            }
        )
        val out = StringBuilder()
        renderChildren(out, dumper, root, 0)
        pendingComments?.forEach { out.append(it).append('\n') }
        return out.toString()
    }

    /**
     * Saves YAML content to a file
     *
     * @param yamlContent The YAML content to save
     * @param outputFile The file to save to
     */
    fun saveYamlToFile(yamlContent: String, outputFile: File) =
        writeTextToFile(yamlContent, outputFile)

    /**
     * Converts a YAML file to Properties format, preserving comments.
     * Supports multi-document YAML files (documents separated by ---);
     * documents after the first one are separated by a #--- comment line.
     *
     * @param yamlFile The YAML file to convert
     * @return The properties content as a string
     */
    fun convertYamlToProperties(yamlFile: File): String {
        val output = StringBuilder()

        for (segment in splitYamlSegments(yamlFile.readText())) {
            val document = Yaml().load(StringReader(segment)) as? Map<*, *> ?: continue
            if (document.isEmpty()) continue

            val entries = flattenYamlMap(document)
            if (entries.isEmpty()) continue
            if (output.isNotEmpty()) {
                output.append("#---\n")
            }
            emitYamlSegment(segment, entries, output)
        }

        return output.toString()
    }

    /**
     * Saves Properties content to a file
     *
     * @param propertiesContent The properties content to save
     * @param outputFile The file to save to
     */
    fun savePropertiesToFile(propertiesContent: String, outputFile: File) =
        writeTextToFile(propertiesContent, outputFile)

    /**
     * Writes text content to a file and refreshes the IntelliJ VFS
     */
    private fun writeTextToFile(content: String, outputFile: File) {
        // Ensure parent directory exists
        outputFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
        // Write to disk
        FileWriter(outputFile).use { it.write(content) }
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

    // ---------------------------------------------------------------------
    // Properties -> YAML
    // ---------------------------------------------------------------------

    private class LogicalLine(
        val isComment: Boolean,
        val text: String = "",
        val key: String = "",
        val value: String = "",
        val comment: String? = null
    )

    private class YamlNode {
        var before: MutableList<String>? = null
        var value: String? = null
        var inline: String? = null
        val children = LinkedHashMap<String, YamlNode>()
    }

    /**
     * Parses properties text preserving order, full-line comments, inline
     * comments, line continuations and standard properties unescaping.
     */
    private fun parsePropertiesLines(text: String): List<LogicalLine> {
        val result = mutableListOf<LogicalLine>()
        val physical = text.lines()
        var i = 0
        while (i < physical.size) {
            val line = physical[i]
            i++
            val trimmed = line.trimStart()
            if (trimmed.isEmpty()) continue
            if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
                result.add(LogicalLine(isComment = true, text = trimmed))
                continue
            }
            var logical = line
            while (endsWithContinuation(logical) && i < physical.size) {
                logical = logical.dropLast(1) + physical[i].trimStart()
                i++
            }
            result.add(parseKeyValue(logical))
        }
        return result
    }

    /** A trailing run of backslashes continues the line only when the count is odd. */
    private fun endsWithContinuation(line: String): Boolean {
        var backslashes = 0
        var i = line.length - 1
        while (i >= 0 && line[i] == '\\') {
            backslashes++
            i--
        }
        return backslashes % 2 == 1
    }

    private fun parseKeyValue(line: String): LogicalLine {
        var i = 0
        while (i < line.length && line[i].isWhitespace()) i++
        val keyStart = i
        while (i < line.length && line[i] != '=' && line[i] != ':' && !line[i].isWhitespace()) i++
        val rawKey = line.substring(keyStart, i)
        while (i < line.length && line[i].isWhitespace()) i++
        if (i < line.length && (line[i] == '=' || line[i] == ':')) i++
        val valuePortion = line.substring(i)

        val commentMatch = propInlineComment.find(valuePortion)
        val rawValue: String
        val comment: String?
        if (commentMatch != null) {
            rawValue = commentMatch.groupValues[1]
            comment = commentMatch.groupValues[2].trim().ifEmpty { null }
        } else {
            rawValue = valuePortion
            comment = null
        }
        return LogicalLine(
            isComment = false,
            key = unescapeProperties(rawKey),
            value = unescapeProperties(rawValue.trim()),
            comment = comment
        )
    }

    /** Standard java.util.Properties unescaping (\t \n \r \f \uXXXX and \<other> -> \<other>). */
    private fun unescapeProperties(raw: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c != '\\') {
                out.append(c)
                i++
                continue
            }
            i++
            if (i >= raw.length) break
            when (val e = raw[i]) {
                't' -> out.append('\t')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                'f' -> out.append(0x0C.toChar())
                'u' -> {
                    if (i + 4 < raw.length) {
                        val hex = raw.substring(i + 1, i + 5)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            out.append(code.toChar())
                            i += 4
                        } else {
                            out.append('u')
                        }
                    } else {
                        out.append('u')
                    }
                }
                else -> out.append(e)
            }
            i++
        }
        return out.toString()
    }

    /**
     * Inserts a dotted-key entry into the ordered tree. The pending comment
     * block attaches to the shallowest node the entry creates; scalar-vs-branch
     * conflicts keep the nested structure (structure wins).
     */
    private fun insertEntry(
        root: YamlNode,
        parts: List<String>,
        value: String,
        comment: String?,
        pendingComments: List<String>?
    ) {
        var current = root
        var commentAttached = false
        val fullPath = parts.joinToString(".")

        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            var child = current.children[part]
            if (child == null) {
                child = YamlNode()
                if (!commentAttached && pendingComments != null) {
                    child.before = pendingComments.toMutableList()
                    commentAttached = true
                }
                current.children[part] = child
            } else if (child.value != null) {
                // e.g. logging.file=x together with logging.file.max-history=0:
                // YAML cannot hold a value and a nested section under the same key,
                // so the nested structure wins
                thisLogger().warn("Property '$fullPath' conflicts with '$part=${child.value}'; keeping nested structure")
                child.value = null
                child.inline = null
            }
            current = child
        }

        val last = parts.last()
        var leaf = current.children[last]
        if (leaf == null) {
            leaf = YamlNode()
            if (!commentAttached && pendingComments != null) {
                leaf.before = pendingComments.toMutableList()
            }
            current.children[last] = leaf
        } else if (leaf.children.isNotEmpty()) {
            thisLogger().warn("Property '$fullPath' conflicts with nested keys under '$last'; keeping nested structure")
            return
        }
        leaf.value = value
        leaf.inline = comment
    }

    /**
     * Renders the ordered tree as YAML. Keys and values are rendered with
     * SnakeYAML so quoting/escaping stays correct; multi-line dumps keep their
     * relative indentation and inline comments go on the header line.
     */
    private fun renderChildren(out: StringBuilder, dumper: Yaml, node: YamlNode, indent: Int) {
        val pad = " ".repeat(indent)
        for ((key, child) in node.children) {
            child.before?.forEach { line ->
                out.append(pad).append(line).append('\n')
            }
            if (child.children.isEmpty()) {
                val dumped = dumper.dump(mapOf(key to (child.value ?: ""))).trimEnd()
                val lines = dumped.lines()
                out.append(pad).append(lines[0])
                child.inline?.let { out.append(" # ").append(it) }
                out.append('\n')
                for (i in 1 until lines.size) {
                    out.append(pad).append(lines[i]).append('\n')
                }
            } else {
                // Render the key with SnakeYAML scalar quoting, then open a block section.
                // (Dumping an empty map would emit flow-style `key: {}` under prettyFlow.)
                out.append(pad).append(dumper.dump(key).trimEnd()).append(":\n")
                renderChildren(out, dumper, child, indent + 2)
            }
        }
    }

    // ---------------------------------------------------------------------
    // YAML -> Properties
    // ---------------------------------------------------------------------

    private class SegmentComments {
        val before = LinkedHashMap<String, MutableList<String>>()
        val inline = LinkedHashMap<String, String>()
        val leftover = mutableListOf<String>()
    }

    /** Splits raw YAML text on `---` document-marker lines. */
    private fun splitYamlSegments(text: String): List<String> {
        val segments = mutableListOf<String>()
        val current = StringBuilder()
        for (line in text.lineSequence()) {
            if (line == "---" || line.startsWith("--- ")) {
                segments.add(current.toString())
                current.setLength(0)
            } else {
                current.appendLine(line)
            }
        }
        segments.add(current.toString())
        return segments
    }

    /**
     * Scans one YAML segment's raw lines, mapping full-line comments and inline
     * comments to the dotted key paths they document. Block scalars and list
     * items are skipped so their inner `#` characters are not misread.
     */
    private fun scanSegmentComments(segment: String): SegmentComments {
        val comments = SegmentComments()
        val stack = mutableListOf<Pair<Int, String>>()
        val pending = mutableListOf<String>()
        var blockScalarIndent = -1

        for (line in segment.lines()) {
            if (line.isBlank()) continue
            val indent = line.length - line.trimStart().length
            val trimmed = line.trimStart()

            if (blockScalarIndent >= 0 && indent > blockScalarIndent) continue
            blockScalarIndent = -1

            if (trimmed.startsWith("#")) {
                pending.add(trimmed)
                continue
            }
            if (trimmed.startsWith("-")) continue

            val match = yamlKeyLine.matchEntire(line) ?: continue
            val rawKey = match.groupValues[2]
            val key = if (rawKey.length >= 2 &&
                ((rawKey.startsWith("\"") && rawKey.endsWith("\"")) ||
                    (rawKey.startsWith("'") && rawKey.endsWith("'")))
            ) {
                rawKey.substring(1, rawKey.length - 1)
            } else {
                rawKey
            }
            val valuePart = match.groupValues[3]
            val inlineText = extractYamlInlineComment(valuePart)

            while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeAt(stack.lastIndex)
            val path = (stack.map { it.second } + key).joinToString(".")
            stack.add(indent to key)

            if (pending.isNotEmpty()) {
                comments.before.getOrPut(path) { mutableListOf() }.addAll(pending)
                pending.clear()
            }
            if (inlineText != null) {
                if (valuePart.trim().isEmpty()) {
                    // comment after a branch key: emit before the branch's first entry
                    comments.before.getOrPut(path) { mutableListOf() }.add("# $inlineText")
                } else {
                    comments.inline[path] = inlineText
                }
            }
            if (valuePart.trim().matches(blockScalarMarker)) {
                blockScalarIndent = indent
            }
        }

        comments.leftover.addAll(pending)
        return comments
    }

    /**
     * Finds a `# ...` comment in a raw YAML value part. The `#` must be
     * preceded by whitespace and must not appear inside quotes.
     */
    private fun extractYamlInlineComment(valuePart: String): String? {
        var inSingle = false
        var inDouble = false
        for (i in valuePart.indices) {
            when (valuePart[i]) {
                '\'' -> if (!inDouble) inSingle = !inSingle
                '"' -> if (!inSingle) inDouble = !inDouble
                '#' -> if (!inSingle && !inDouble && i > 0 && valuePart[i - 1].isWhitespace()) {
                    return valuePart.substring(i + 1).trim().ifEmpty { null }
                }
            }
        }
        return null
    }

    /** Emits one document segment: comments flushed before the entries they annotate. */
    private fun emitYamlSegment(segment: String, entries: Map<String, Any?>, out: StringBuilder) {
        val comments = scanSegmentComments(segment)
        val flushed = mutableSetOf<String>()

        for ((path, value) in entries) {
            val parts = path.split(".")
            for (i in parts.indices) {
                val prefix = parts.subList(0, i + 1).joinToString(".")
                if (flushed.add(prefix)) {
                    comments.before[prefix]?.forEach { out.append(it).append('\n') }
                }
            }
            out.append(path).append('=').append(value?.toString() ?: "")
            comments.inline[path]?.let { out.append(" # ").append(it) }
            out.append('\n')
        }

        for ((path, lines) in comments.before) {
            if (path !in flushed) lines.forEach { out.append(it).append('\n') }
        }
        comments.leftover.forEach { out.append(it).append('\n') }
    }

    /**
     * Flattens a hierarchical YAML map to a flat map with dot-separated keys,
     * preserving document order and stringifying non-string keys.
     */
    private fun flattenYamlMap(yamlMap: Map<*, *>, prefix: String = ""): LinkedHashMap<String, Any?> {
        val result = LinkedHashMap<String, Any?>()

        for ((key, value) in yamlMap) {
            val keyStr = key?.toString() ?: continue
            val newKey = if (prefix.isEmpty()) keyStr else "$prefix.$keyStr"
            when (value) {
                is Map<*, *> -> result.putAll(flattenYamlMap(value, newKey))
                else -> result[newKey] = value
            }
        }

        return result
    }

    private companion object {
        /** Trailing ` # comment` inside a properties value (whitespace before # required). */
        val propInlineComment = Regex("^(.*?)\\s+#(.*)$")
        /** A YAML mapping entry line: indent, key, then everything after the colon. */
        val yamlKeyLine = Regex("^( *)([^#\\s][^:]*?):(.*)$")
        /** Block scalar indicators such as `|`, `|-`, `>2`. */
        val blockScalarMarker = Regex("^[|>][+-]?\\d*$")
    }
}
