package com.cs.beam.states.impl

import com.cs.beam.domain.Branch
import com.cs.beam.domain.StateType
import com.cs.beam.states.WorkflowState

data class Parallel(
    override val name: String,
    override val type: StateType = StateType.Parallel,
    override val previous: String?,
    override val next: String? = "",
    override val isEnd: Boolean = false,
    val branches: List<Branch>
) : WorkflowState
