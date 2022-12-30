# Beam

State Machine based workflow orchestration engine. Based on the inspiration from BPMN and Step functions. Use this engine for realtime and near realtime workflows. 

- **Realtime**: Workflow that executes within 15 Seconds window.
- **Near Realtime**: Workflow that takes more than 15 Seconds but completes within 120 Seconds

There are two types of Workflows that you can choose with beam engine, Asynchronous Workflows and Synchronous Workflows.

### Asynchronous Workflows
It returns confirmation that the workflow was started, but do not wait for the workflow to complete. To get the result, you need to implement callback in the workflow. Prefer when your workflow executes more than 15 seconds.

#### Supported states

|   State   | Status  |
|:---------:|:-------:|
|   Task    | &check; |
| Condition | &check; |
|   Wait    | &check; |
| Parallel  | &check; |



### Synchronous Workflows 
It starts a workflow, wait until it completes, then return the result. Prefer when your workflow completes in less than 15 seconds.

#### Supported states

|   State   | Status  |
|:---------:|:-------:|
|   Task    | &check; |
| Condition | &check; |
|   Wait    | &cross; |
| Parallel  | &check; |

### Frameworks and Language:

This library is written in Kotlin, and It uses spring reactive stack.

- [Java 17](https://docs.aws.amazon.com/corretto/latest/corretto-11-ug/downloads-list.html)
- [Netty web container](https://projectreactor.io/docs/netty/release/reference/index.html)
- [R2DBC database driver](https://spring.io/projects/spring-data-r2dbc)
- [Kotlin Coroutines](https://coding2fun.wordpress.com/2022/07/10/kotlin-coroutines-and-jvm-writing-non-blocking-code-with-ease/)
- [Spring](https://spring.io/)

### How to build the library:

- This is a multi maven project and it consists of two modules
  - core
  - express-engine
- To build the application, we can create the jar file using the following command :

  `mvn clean package`

- The above command will create a jar file in targets folder of **core** and **express-engine** module

### Code Stats
![](Stats.png)
