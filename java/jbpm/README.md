SLAMon jBPM WorkItemHandler
===========================

This module provides the SLAMon integration to JBoss Business Process Management Suite (jBPM). Every SLAMon task is represented by a custom work item in jBPM.

SLAMon provides 2 jBPM custom work items:

  * SLAMonJupsHandler - for sending push notifications via JBoss Unified Push Server
  * SLAMonWorkItemHandler - for executing SLAMon task handlers 

Installation
------------

When creating custom work items, you need to create both a Work Item Definition and its corresponding Work Item Handler.

### Work Item Definition ###

To create a work item definition in Business Central:

Click Authoring -> Project Authoring

In the Project Authoring page, click New Item -> Work Item definition

Add the entries of SLAMon.

Use the following for SLAMonJupsHandler:

    import org.drools.core.process.core.datatype.impl.type.StringDataType;
    import org.drools.core.process.core.datatype.impl.type.IntegerDataType;

    [
        [
            "name" : "slamon-push",
            "parameters" : [
                "variant_id" : new StringDataType(),
                "alert" : new StringDataType(),
                "sound" : new StringDataType()
            ],
            "results" : [
                "result" : new StringDataType()
            ],
            "displayName" : "SLAMon Push Notification",
            "icon" : "defaultservicenodeicon.png"
        ]
    ]

Please refer to the Usage section of this readme for the description of the parameters and results.

You can use the following as template for each SLAMonWorkItemHandler to be used:

    import org.drools.core.process.core.datatype.impl.type.IntegerDataType;

    [
        [
            "name" : "TASK_NAME",
            "parameters" : [
                "INPUT_PARAMETER1" : new StringDataType(),
                "INPUT_PARAMETER2" : new IntegerDataType()
            ],
            "results" : [
                "OUTPUT_PARAMETER1" : new StringDataType(),
                "OUTPUT_PARAMETER2" : new IntegerDataType()
            ],
            "displayName" : "DISPLAY_NAME",
            "icon" : "defaultservicenodeicon.png"
        ]
    ]

Where:

  * TASK_NAME - the jBPM custom work item name assigned to the SLAMon task type
  * INPUT_PARAMETER - the input parameters of the SLAMon task type
  * OUTPUT_PARAMETER - the output parameters of the SLAMon task type
  * DISPLAY_NAME - the name that will appear in the Process Designer Canvas Object Library palette in Business Central

Please refer to the readme of the corresponding SLAMon task type that you are going to use for the actual parameter names and descriptions.

### Work Item Handler ###

A work item handler is a Java class that implement how the work will be executed by jBPM.

##### Build the SLAMon jbpm module #####

The module can be built with the following maven command:

    mvn package

The generated package will be located in the target folder.

##### Upload the generated SLAMon jbpm jar package to jBPM via Business Central #####

Click Authoring -> Artifact repository.

In the Artifact repository page, click Upload.

##### Add the SLAMon jbpm jar package to project dependencies #####

Click Authoring -> Project Authoring.

In the Project Authoring page, click Tools -> Project Editor then select Dependencies.

Click add from repository then select the SLAMon jbpm jar artifact that was uploaded earlier

##### Add the dependencies of the SLAMon jbpm module to project dependencies #####

Add the following Maven Group ID - Artifact ID - Version ID:

  * org.kie - kie-api - 6.0.1.Final
  * com.google.http-client - google-http-client - 1.19.0
  * com.google.http-client - google-http-client-gson - 1.19.0
  * com.google.code.gson - gson - 2.3.1

##### Register the work item handler #####

Click Authoring -> Administration

In the File Explorer, open the following:

    PROJECT_NAME/src/main/resources/META-INF/kmodule.xml

You can use the following as your kmodule.xml template:

    <kmodule xmlns="http://jboss.org/kie/6.0.0/kmodule" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <kbase name="defaultKieBase" default="true" eventProcessingMode="cloud" equalsBehavior="identity" declarativeAgenda="disabled" scope="javax.enterprise.context.ApplicationScoped" packages="*">
        <ksession name="defaultKieSession" type="stateful" default="true" clockType="realtime" scope="javax.enterprise.context.ApplicationScoped">
          <workItemHandlers>
            <!-- add your entries here -->
          </workItemHandlers>
        </ksession>
      </kbase>
    </kmodule>

Add the following for SLAMonJupsHandler:

    <workItemHandler type="new org.slamon.SLAMonJupsHandler(JBOSS_UNIFIED_PUSH_SERVER_URL, APPLICATION_ID, MASTER_SECRET, VARIANT_ID)" name="slamon-push"/>

Where:

  * JBOSS_UNIFIED_PUSH_SERVER_URL - the JBoss Unified Push Server that is to be used
  * APPLICATION_ID - the user name to the JBoss Unified Push Server
  * MASTER_SECRET - the password to the JBoss Unified Push Server
  * VARIANT_ID - the variant ID of the devices where SLAMon will send a copy of every notification (e.g. variant ID used by the service operation center)

You can use the following as template for each SLAMonWorkItemHandler to be used:

    <workItemHandler type="new org.slamon.SLAMonWorkItemHandler(AGENT_FLEET_MANAGER_URL, TASK_HANDLER_NAME, TASK_HANDLER_VERSION)" name=TASK_NAME/>

Where:

  * AGENT_FLEET_MANAGER_URL - the SLAMon Agent Fleet Manager that is to be used
  * TASK_HANDLER_NAME - the SLAMon task type that is to be included to the jBPM project
  * TASK_HANDLER_VERSION - the SLAMon task type version that is to be used
  * TASK_NAME - the jBPM custom work item name assigned to the SLAMon task type (Note: this should match the work item definition)

Usage
-----

Custom work items are located under Service Tasks of the Process Designer Canvas Object Library palette in Business Central.

### SLAMonJupsHandler ###

The SLAMon Push Notification custom work item can be used in a test process to send a notification to a mobile device.

SLAMon Push Notification accepts the following DataInput:

  * variant_id - the id of the group of devices where the notification will be sent
  * alert - the message to be sent
  * sound - the sound that will be played on the receiving devices

Take note that a copy of the notification will always be sent to the variant id defined in the Work Item Handler registration. For example, this can be the variant ID used by the service operation center.

A notification can also be sent to all devices. To do this, use "all" instead of specifying a specific variant_id.

If you do not have a specific sound to play, please use "default".

SLAMon Push Notification returns the following DataOutput:

  * result - either "successful" or "failed"

### SLAMonWorkItemHandler ###

Each SLAMon task type is represented by a separate custom work item in jBPM.

The DataInput of each custom work item corresponds to the input data parameter of the equivalent SLAMon task handler.

Likewise, the DataOutput of each custom work item corresponds to the response data return by the equivalent SLAMon task handler.

Please refer the readme files of the SLAMon Agent and the corresponding task types that you are going to use for more details.

