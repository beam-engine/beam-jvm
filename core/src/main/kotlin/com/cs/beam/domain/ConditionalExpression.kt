package com.cs.beam.domain

data class ConditionalExpression(
    val conditionType: ConditionType,
    val expressionList: List<Expression>,
    val nexState: String,
)