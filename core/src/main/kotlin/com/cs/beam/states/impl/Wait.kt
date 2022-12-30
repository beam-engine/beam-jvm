package com.cs.beam.states.impl

import com.cs.beam.domain.StateType
import com.cs.beam.states.WorkflowState

data class Wait(
    override val name: String,
    override val type: StateType = StateType.Wait,
    override val previous: String?,
    override val next: String?,
    override val isEnd: Boolean = false
) : WorkflowState