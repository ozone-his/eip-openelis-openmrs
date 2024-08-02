/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsTaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) {
        String response = producerTemplate.requestBody("direct:openmrs-create-task-route", task, String.class);
        FhirContext ctx = FhirContext.forR4();
        Task savedTask = ctx.newJsonParser().parseResource(Task.class, response);
        return savedTask;
    }

    public Task updateTask(ProducerTemplate producerTemplate, Task task, String taskID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, taskID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:openmrs-update-task-route", task, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Task updatedTask = ctx.newJsonParser().parseResource(Task.class, response);
        return updatedTask;
    }

    public Task markTaskRejected(Task task) {
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        return rejectTask;
    }

    public Task updateTaskStatus(Task task, String analysisRequestTaskStatus) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.fromCode(analysisRequestTaskStatus));
        return updateTask;
    }

    public boolean doesTaskExists(Task task) {
        return task != null && task.getId() != null && !task.getId().isEmpty() && task.getStatus() != null;
    }
}
