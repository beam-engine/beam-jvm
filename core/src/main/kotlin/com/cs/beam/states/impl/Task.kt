package com.cs.beam.states.impl

import com.cs.beam.domain.StateType
import com.cs.beam.states.WorkflowState

data class Task(
    override val name: String,
    override val type: StateType = StateType.Task,
    override val previous: String?,
    override val next: String?,
    override val isEnd: Boolean = false
) : WorkflowState
