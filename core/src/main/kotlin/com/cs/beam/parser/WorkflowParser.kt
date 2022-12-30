package com.cs.beam.parser

import com.cs.beam.domain.WorkflowGraph
import java.io.InputStream

interface WorkflowParser {

    fun parseWorkflow(file: InputStream): WorkflowGraph
}