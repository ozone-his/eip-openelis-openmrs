/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsTaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) {

        return producerTemplate.requestBody("direct:openmrs-create-resource-route", task, Task.class);
    }

    public Task updateTask(ProducerTemplate producerTemplate, Task task, String taskID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, taskID);

        return producerTemplate.requestBodyAndHeaders("direct:openmrs-update-task-route", task, headers, Task.class);
    }

    public Task markTaskRejected(Task task) {
        Task rejectTask = new Task();
        rejectTask.setId(task.getId());
        rejectTask.setStatus(Task.TaskStatus.REJECTED);
        rejectTask.setIntent(Task.TaskIntent.ORDER);
        return rejectTask;
    }

    public Task markTaskCompleted(Task task) {
        Task updateTask = new Task();
        updateTask.setId(task.getIdPart());
        updateTask.setIntent(Task.TaskIntent.ORDER);
        updateTask.setStatus(Task.TaskStatus.COMPLETED);
        return updateTask;
    }

    public boolean doesTaskExists(Task task) {
        return task != null && task.getId() != null && !task.getId().isEmpty() && task.getStatus() != null;
    }

    public Task getTaskByServiceRequestID(ProducerTemplate producerTemplate, String serviceRequestID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestID);
        Bundle bundle =
                producerTemplate.requestBodyAndHeaders("direct:openmrs-get-task-route", null, headers, Bundle.class);

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Task.class::isInstance)
                .map(Task.class::cast)
                .findFirst()
                .orElse(null);
    }

    public void rejectTaskByServiceRequestID(ProducerTemplate producerTemplate, String serviceRequestID) {
        Task task = getTaskByServiceRequestID(producerTemplate, serviceRequestID);
        if (doesTaskExists(task)) {
            updateTask(producerTemplate, markTaskRejected(task), task.getIdPart());
        }
    }

    public Task buildTask(ServiceRequest serviceRequest) {
        Task openelisTask = new Task();
        String taskUuid = UUID.randomUUID().toString();
        openelisTask.setId(taskUuid);

        Identifier orderUuid = new Identifier();
        orderUuid.setSystem("http://openelis-global.org/order_uuid");
        orderUuid.setValue(taskUuid);

        openelisTask.setIdentifier(Collections.singletonList(orderUuid));

        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(orderUuid);

        openelisTask.setIdentifier(identifierList);

        openelisTask.addBasedOn().setReference(serviceRequest.getIdPart()).setType("ServiceRequest");

        openelisTask.setStatus(Task.TaskStatus.REQUESTED);
        openelisTask.setIntent(Task.TaskIntent.ORDER);
        openelisTask.setPriority(Task.TaskPriority.ROUTINE);

        openelisTask.setFor(
                new Reference().setReference(serviceRequest.getSubject().getReference()));
        openelisTask.setAuthoredOn(serviceRequest.getAuthoredOn());

        Reference ownerReference = new Reference();
        ownerReference.setReference(serviceRequest.getRequester().getReference());
        ownerReference.setType("Practitioner");

        openelisTask.setOwner(ownerReference);

        return openelisTask;
    }
}
