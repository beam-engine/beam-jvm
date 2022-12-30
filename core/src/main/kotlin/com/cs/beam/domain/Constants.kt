package com.cs.beam.domain

object Constants {

    // Name of the workflow
    const val LABEL_NAME = "Name"

    // Description in workflow JSON
    const val LABEL_DESCRIPTION = "Description"

    // The First State in the workflow
    const val LABEL_INIT_STATE = "InitState"

    // Is workflow needs to executed in async mode
    const val LABEL_ASYNC = "Async"

    // All workflow states
    const val LABEL_STATES = "States"

    // Result Variable
    const val LABEL_RESULT_VARIABLE = "ResultVariable"

    // Workflow Type
    const val LABEL_TYPE = "Type"

    // Workflow next state
    const val LABEL_NEXT = "Next"

    const val LABEL_BRANCHES = "Branches"

    // Is End of the workflow
    const val LABEL_END = "End"

    // Workflow conditions and it's related labels
    const val LABEL_CONDITIONS = "Conditions"
    const val LABEL_CONDITION_VARIABLE = "Variable"
    const val LABEL_CONDITION_MATCH_TYPE = "MatchType"
    const val LABEL_CONDITION_MATCH_VALUE = "MatchValue"


    // Error Messages
    const val ERR_INVALID_JSON_FILE = "Cannot able to create workflow graph from Json"
    const val ERR_REQUIRED_INFO_MISSING = "Required information in workflow Json is missing"
    const val ERR_INVALID_WORKFLOW = "The provided workflow json is wrong"
}