package com.cs.beam.core.parser.json

import com.cs.beam.parser.json.JsonWorkflowParser
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.test.assertEquals

internal class JsonWorkflowParserTest {

    private val resourceFileList: MutableList<Path> = ArrayList(5)

    private val genericWorkflowParser = JsonWorkflowParser()

    @BeforeEach
    fun setUp() {
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val resourcesPath = Paths.get(projectDirAbsolutePath, "/src/test/resources")
        val pathMutableList = Files.list(resourcesPath)
            .filter { Files.isRegularFile(it) }
            .filter {
                println(it.toString())
                it.toString().endsWith(".json")
            }
            .collect(Collectors.toList())
        if (pathMutableList.isNotEmpty()) {
            resourceFileList.addAll(pathMutableList)
        }
    }

    @Test
    fun parseWorkflow() {
        assertEquals(5, resourceFileList.size)
        resourceFileList.forEach {
            val workflowGraph = genericWorkflowParser.parseWorkflow(Files.newInputStream(it))
            workflowGraph.getWorkflowState("")
            assertNotNull(workflowGraph)
        }
    }
}