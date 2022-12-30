package com.cs.beam.parser.json

import com.cs.beam.domain.*
import com.cs.beam.domain.Constants.ERR_INVALID_JSON_FILE
import com.cs.beam.domain.Constants.ERR_INVALID_WORKFLOW
import com.cs.beam.domain.Constants.ERR_REQUIRED_INFO_MISSING
import com.cs.beam.domain.Constants.LABEL_ASYNC
import com.cs.beam.domain.Constants.LABEL_BRANCHES
import com.cs.beam.domain.Constants.LABEL_CONDITIONS
import com.cs.beam.domain.Constants.LABEL_CONDITION_MATCH_TYPE
import com.cs.beam.domain.Constants.LABEL_CONDITION_MATCH_VALUE
import com.cs.beam.domain.Constants.LABEL_CONDITION_VARIABLE
import com.cs.beam.domain.Constants.LABEL_DESCRIPTION
import com.cs.beam.domain.Constants.LABEL_END
import com.cs.beam.domain.Constants.LABEL_INIT_STATE
import com.cs.beam.domain.Constants.LABEL_NAME
import com.cs.beam.domain.Constants.LABEL_NEXT
import com.cs.beam.domain.Constants.LABEL_RESULT_VARIABLE
import com.cs.beam.domain.Constants.LABEL_STATES
import com.cs.beam.domain.Constants.LABEL_TYPE
import com.cs.beam.json.JsonException
import com.cs.beam.json.JsonUtils
import com.cs.beam.parser.WorkflowParser
import com.cs.beam.parser.WorkflowParserError
import com.cs.beam.states.WorkflowState
import com.cs.beam.states.impl.Choice
import com.cs.beam.states.impl.Parallel
import com.cs.beam.states.impl.Task
import com.cs.beam.states.impl.Wait
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStream
import java.util.*

class JsonWorkflowParser : WorkflowParser {
    private val logger = LoggerFactory.getLogger(JsonWorkflowParser::class.java)

    /**
     * Creates the [WorkflowGraph] state machine instance from the given JSON file
     *
     * @param file workflow json resource
     */
    override fun parseWorkflow(file: InputStream): WorkflowGraph {
        logger.info("In parseWorkflow")

        val workflowGraph: WorkflowGraph
        val workflowJson = file.bufferedReader().use(BufferedReader::readText)
        val workflowStatesDictionary: MutableMap<String, WorkflowState> = LinkedHashMap()
        val workflowName: String
        val startState: String
        val isAsync: Boolean
        var resultVariable = ""
        try {
            val jsonNode = JsonUtils.fromJson(workflowJson, JsonNode::class.java)
            logger.info("Checking for all required fields in workflow JSON")
            validateFields(jsonNode)

            // Setting the workflow name
            workflowName = jsonNode.get(LABEL_NAME).asText()

            // Identifying start state
            startState = jsonNode.get(LABEL_INIT_STATE).asText()

            // Setting Async or Sync
            isAsync = jsonNode.get(LABEL_ASYNC).asBoolean()

            // Setting the result variable
            if (!isAsync) {
                resultVariable = jsonNode.get(LABEL_RESULT_VARIABLE).asText()
            }

            val iterator: Iterator<Map.Entry<String, JsonNode>> = jsonNode.get(LABEL_STATES).fields()
            val componentNameSet = mutableSetOf<String>()
            var previousState: String? = null
            while (iterator.hasNext()) {
                val stateEntry = iterator.next()
                val componentName = stateEntry.key
                val stateNode = stateEntry.value

                val stateInstance: WorkflowState =
                    createWorkflowState(stateNode, componentName, previousState, componentNameSet)
                previousState = componentName

                componentNameSet.add(componentName)
                workflowStatesDictionary[componentName] = stateInstance
            }

            // Setting all workflow states
            workflowGraph = WorkflowGraph(
                workflowName,
                isAsync,
                startState,
                resultVariable,
                workflowStatesDictionary
            )
        } catch (ex: JsonException) {
            logger.error(
                "Error: Problem while parsing the workflow, cannot able to create workflow graph -{}",
                ex.message
            )
            throw WorkflowParserError("$ERR_INVALID_JSON_FILE $file", ex)
        } finally {
            file.close()
        }

        return workflowGraph
    }

    private fun createWorkflowState(
        stateNode: JsonNode,
        componentName: String,
        previousState: String?,
        componentNameSet: MutableSet<String>
    ): WorkflowState {
        if (!stateNode.has(LABEL_TYPE)) {
            logger.error("Error: Every state should have $LABEL_TYPE field")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_TYPE")
        }

        if (!stateNode.has(LABEL_END)) {
            logger.error("Error: Every state should have $LABEL_END field")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_END")
        }

        val isEnd = stateNode.get(LABEL_END).asBoolean()
        val stateType = stateNode.get(LABEL_TYPE).asText()

        if (StateType.Choice.name != stateType) {
            val node = stateNode.get(LABEL_NEXT)
            if (isEnd && !node.isNull) {
                logger.error("Error: Cannot have end state as true because next state is not null $node")
                throw WorkflowParserError("$ERR_INVALID_WORKFLOW = $componentName ")
            }
        }

        val stateInstance: WorkflowState = when (stateType) {
            StateType.Task.name -> createTaskState(
                componentName,
                previousState,
                stateNode,
                isEnd,
                componentNameSet
            )

            StateType.Choice.name -> createConditionsState(
                componentName,
                stateNode,
                previousState,
                isEnd,
                componentNameSet
            )

            StateType.Wait.name -> createWaitState(
                componentName,
                previousState,
                stateNode,
                isEnd,
                componentNameSet
            )

            StateType.Parallel.name -> createParallelState(
                componentName,
                previousState,
                stateNode,
                isEnd,
                componentNameSet
            )

            else -> {
                logger.error("Error: Invalid or Unknown task name found ${stateNode.get(LABEL_TYPE).asText()}")
                throw WorkflowParserError(
                    "$ERR_INVALID_WORKFLOW, Invalid task name found ${
                        stateNode.get(
                            LABEL_TYPE
                        ).asText()
                    }"
                )
            }
        }
        return stateInstance
    }

    private fun createParallelState(
        parallelFieldName: String,
        previousState: String?,
        stateNode: JsonNode,
        isEnd: Boolean,
        componentNameSet: MutableSet<String>
    ): WorkflowState {
        logger.info("In createParallelState")

        if (!stateNode.has(LABEL_BRANCHES)) {
            logger.error("Error: Required $LABEL_BRANCHES label does not exist in  ${StateType.Parallel.name} state")
            throw WorkflowParserError("$ERR_INVALID_WORKFLOW, $LABEL_BRANCHES label does not exist")
        }

        val branchList: MutableList<Branch> = ArrayList(5)
        for (node in stateNode.get(LABEL_BRANCHES)) {
            val parallelStateDict: MutableMap<String, WorkflowState> = HashMap(5)
            val startState = node.get(LABEL_INIT_STATE).asText()
            val parallelNodeName = node.get(LABEL_NAME).asText()
            val iterator: Iterator<Map.Entry<String, JsonNode>> = node.get(LABEL_STATES).fields()
            val statesList: MutableList<WorkflowState> = LinkedList()
            var branchPreviousState = ""
            while (iterator.hasNext()) {
                val branchEntry = iterator.next()
                val componentName = branchEntry.key
                val branchStateNode = branchEntry.value
                val stateInstance: WorkflowState =
                    createWorkflowState(branchStateNode, componentName, branchPreviousState, componentNameSet)
                statesList.add(stateInstance)
                branchPreviousState = componentName
                componentNameSet.add(componentName)
                parallelStateDict[componentName] = stateInstance
            }
            branchList.add(Branch(name = parallelNodeName, startAt = startState, stateMap = parallelStateDict))
        }

        logger.info("Return from createParallelState")
        return Parallel(
            name = parallelFieldName, previous = previousState, next = stateNode.get(LABEL_NEXT).asText(),
            branches = branchList, isEnd = isEnd
        )
    }

    private fun createConditionsState(
        componentName: String,
        stateNode: JsonNode,
        previousState: String?,
        isEnd: Boolean,
        componentNameSet: MutableSet<String>
    ): WorkflowState {
        logger.info("In createConditionsState")

        if (isEnd) {
            logger.error("Error: We cannot end the workflow with ${StateType.Choice.name} state")
            throw WorkflowParserError("$ERR_INVALID_WORKFLOW, Workflow cannot end with ${StateType.Choice.name} state")
        }

        if (!stateNode.has(LABEL_CONDITIONS)) {
            logger.error("Error: Required $LABEL_CONDITIONS label does not exist in  ${StateType.Choice.name} state")
            throw WorkflowParserError("$ERR_INVALID_WORKFLOW, $LABEL_CONDITIONS label does not exist")
        }

        logger.info("Return from createConditionsState")
        return Choice(
            name = componentName,
            previous = previousState,
            conditionList = extractConditions(stateNode.get(LABEL_CONDITIONS), componentNameSet)
        )
    }

    private fun createTaskState(
        componentName: String,
        previousState: String?,
        stateNode: JsonNode,
        isEnd: Boolean,
        componentNameSet: MutableSet<String>
    ): WorkflowState {
        logger.info("In createTaskState")

        val taskState = Task(
            name = componentName,
            type = StateType.Task,
            previous = previousState,
            next = stateNode.get(LABEL_NEXT).asText(),
            isEnd = isEnd
        )
        validateState(taskState.next!!, componentNameSet)

        logger.info("Return from createTaskState")
        return taskState
    }

    private fun createWaitState(
        componentName: String,
        previousState: String?,
        stateNode: JsonNode,
        isEnd: Boolean,
        componentNameSet: MutableSet<String>
    ): WorkflowState {
        logger.info("In createWaitState")

        val stateInstance = Wait(
            name = componentName,
            previous = previousState,
            next = stateNode.get(LABEL_NEXT).asText(),
            isEnd = isEnd
        )
        validateState(stateInstance.next!!, componentNameSet)

        logger.info("Return from createWaitState")
        return stateInstance
    }

    private fun validateState(nextState: String, componentSet: Set<String>) {
        if (nextState.isBlank() && componentSet.contains(nextState)) {
            logger.error("Error: Cannot connect to $nextState, Workflow is wrong !!")
            throw WorkflowParserError("$ERR_INVALID_WORKFLOW, We can't connect to $nextState")
        }
    }

    private fun extractConditions(
        jsonNode: JsonNode,
        componentSet: Set<String>
    ): Set<ConditionalExpression> {
        logger.info("In extractConditions")
        val result = mutableSetOf<ConditionalExpression>()
        val iterator = jsonNode.iterator()
        while (iterator.hasNext()) {
            val node = iterator.next()
            val key = node.fields().next().key!!
            val conditionType = when (key) {
                "And" -> ConditionType.AND
                "Simple" -> ConditionType.SIMPLE
                "Or" -> ConditionType.OR
                else -> {
                    logger.error("Unknown condition type found in the workflow Json {}", key)
                    throw WorkflowParserError("$ERR_INVALID_WORKFLOW, Invalid match type in conditions $key")
                }
            }

            val conditionalExpression = node.get(key)
            val expressionList: MutableList<Expression> = ArrayList(5)
            for (expressionNode in conditionalExpression.iterator()) {
                val variable = expressionNode.get(LABEL_CONDITION_VARIABLE).asText()
                val matchTypeText = expressionNode.get(LABEL_CONDITION_MATCH_TYPE).asText()
                val matchType: MatchType = when (matchTypeText!!) {
                    "StringEquals" -> MatchType.STRING_EQUALS
                    "StringNotEquals" -> MatchType.STRING_NOT_EQUALS
                    else -> {
                        logger.error("Unknown condition type found in the workflow Json - {}", matchTypeText)
                        throw WorkflowParserError("$ERR_INVALID_WORKFLOW, Invalid match type in conditions $matchTypeText")
                    }
                }
                val matchValue = expressionNode.get(LABEL_CONDITION_MATCH_VALUE).asText()
                expressionList.add(Expression(variable, matchType, matchValue))
            }
            val nextState = node.get(LABEL_NEXT).asText()
            if (nextState.isBlank() && componentSet.contains(nextState)) {
                logger.error("Error: Cannot connect to $nextState, Workflow is wrong !!")
                throw WorkflowParserError("$ERR_INVALID_WORKFLOW, We can't connect to $nextState")
            }
            result.add(ConditionalExpression(conditionType, expressionList, nextState))
        }

        logger.info("Return from extractConditions")
        return result
    }

    /**
     * Function to validate required fields
     *
     * @param jsonNode - Top container node
     */
    private fun validateFields(jsonNode: JsonNode) {
        logger.info("In validateFields")
        logger.info("Workflow Json = {}", jsonNode)

        if (!jsonNode.has(LABEL_NAME)) {
            logger.error("Error: Required label $LABEL_NAME not found in json")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_NAME")
        }

        if (!jsonNode.has(LABEL_DESCRIPTION)) {
            logger.error("Error: Required label $LABEL_DESCRIPTION not found in json")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_DESCRIPTION")
        }

        if (!jsonNode.has(LABEL_ASYNC)) {
            logger.error("Error: Required label $LABEL_ASYNC not found in json")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_ASYNC")
        }

        if (!jsonNode.has(LABEL_STATES)) {
            logger.error("Error: Required label $LABEL_STATES not found in json")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_STATES")
        }

        if (!jsonNode.has(LABEL_INIT_STATE)) {
            logger.error("Error: Required label $LABEL_INIT_STATE not found in json")
            throw WorkflowParserError("$ERR_REQUIRED_INFO_MISSING = $LABEL_INIT_STATE")
        }

        logger.info("Return from validateFields")
    }
}