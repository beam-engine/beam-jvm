package com.cs.beam.domain

import com.cs.beam.states.WorkflowState

data class Branch(val name: String, val startAt: String, val stateMap: Map<String, WorkflowState>)