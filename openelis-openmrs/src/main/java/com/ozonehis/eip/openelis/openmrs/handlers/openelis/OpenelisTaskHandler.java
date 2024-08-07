/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import ca.uhn.fhir.context.FhirContext;
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
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisTaskHandler {

    public Task sendTask(ProducerTemplate producerTemplate, Task task) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, task.getIdPart());
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-create-task-route", task, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Task savedTask = ctx.newJsonParser().parseResource(Task.class, response);
        return savedTask;
    }

    public Task getTaskByServiceRequestID(ProducerTemplate producerTemplate, String serviceRequestID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_SERVICE_REQUEST_ID, serviceRequestID);
        String response =
                producerTemplate.requestBodyAndHeaders("direct:openelis-get-task-route", null, headers, String.class);
        FhirContext ctx = FhirContext.forR4();
        Bundle bundle = ctx.newJsonParser().parseResource(Bundle.class, response);
        List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

        Task task = null;
        for (Bundle.BundleEntryComponent entry : entries) {
            Resource resource = entry.getResource();
            if (resource instanceof Task) {
                task = (Task) resource;
                if (task.getStatus() == Task.TaskStatus.COMPLETED) { // TODO: Fix this hack
                    break;
                }
            }
        }
        return task;
    }

    public void deleteTask(ProducerTemplate producerTemplate, String taskID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_TASK_ID, taskID);
        String response = producerTemplate.requestBodyAndHeaders(
                "direct:openelis-delete-task-route", null, headers, String.class);
        log.info("Openelis: deleteTask response {}", response);
    }

    public boolean doesTaskExists(Task task) {
        return task != null && task.getId() != null && !task.getId().isEmpty() && task.getStatus() != null;
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

        openelisTask.setBasedOn(Collections.singletonList(
                new Reference().setReference("ServiceRequest/" + serviceRequest.getIdPart())));
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
