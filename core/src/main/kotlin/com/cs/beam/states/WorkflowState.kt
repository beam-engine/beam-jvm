package com.cs.beam.states

import com.cs.beam.domain.StateType

interface WorkflowState {

    val name: String
    val type: StateType
    val previous: String?
    val next: String?
    val isEnd: Boolean
}