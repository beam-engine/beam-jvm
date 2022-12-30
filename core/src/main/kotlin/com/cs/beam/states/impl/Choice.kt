package com.cs.beam.states.impl

import com.cs.beam.domain.StateType
import com.cs.beam.states.WorkflowState
import com.cs.beam.domain.ConditionalExpression

data class Choice(
    override val name: String = "Condition_No_Name",
    override val type: StateType = StateType.Condition,
    override val previous: String?,
    override val next: String? = "",
    override val isEnd: Boolean = false,
    private val conditionList: Set<ConditionalExpression>
) : WorkflowState