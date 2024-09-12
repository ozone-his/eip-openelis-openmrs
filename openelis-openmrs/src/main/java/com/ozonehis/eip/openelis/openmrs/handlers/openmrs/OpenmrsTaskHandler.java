/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsTaskHandler {

    @Autowired
    @Qualifier("openmrsFhirClient") private IGenericClient openmrsFhirClient;

    public void sendTask(Task task) {
        MethodOutcome methodOutcome =
                openmrsFhirClient.create().resource(task).encodedJson().execute();

        log.debug("OpenmrsTaskHandler: Task created {}", methodOutcome.getCreated());
    }

    public Task updateTask(Task task, String taskID) {
        MethodOutcome methodOutcome =
                openmrsFhirClient.update().resource(task).encodedJson().execute();

        log.debug("OpenmrsTaskHandler: Task updateTask {}", methodOutcome.getCreated());

        return (Task) methodOutcome.getResource();
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

    public Task getTaskByServiceRequestID(String serviceRequestID) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Task.class)
                .returnBundle(Bundle.class)
                .execute();

        log.debug("OpenmrsTaskHandler: Task getTaskByServiceRequestID {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Task.class::isInstance)
                .map(Task.class::cast)
                .filter(task -> task.getBasedOn()
                        .get(0)
                        .getReference()
                        .equals(serviceRequestID)) // TODO: Use client impl and don't fetch all Tasks
                .findFirst()
                .orElse(null);
    }

    public void rejectTaskByServiceRequestID(String serviceRequestID) {
        Task task = getTaskByServiceRequestID(serviceRequestID);
        if (doesTaskExists(task)) {
            updateTask(markTaskRejected(task), task.getIdPart());
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
