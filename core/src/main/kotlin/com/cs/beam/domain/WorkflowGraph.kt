package com.cs.beam.domain

import com.cs.beam.states.WorkflowState

data class WorkflowGraph(
    val workflowName: String = "",
    val isAsync: Boolean = false,
    val startState: String = "",
    val resultVariable: String = "",
    val statesMap: Map<String, WorkflowState> = emptyMap(),
    val parallelStatesMap: Map<String, Map<String, WorkflowState>> = emptyMap()
) {
    fun getWorkflowState(component: String) = statesMap[component]
}