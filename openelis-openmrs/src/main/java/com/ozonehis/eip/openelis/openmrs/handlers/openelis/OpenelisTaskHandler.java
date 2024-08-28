/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
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
public class OpenelisTaskHandler {

    @Autowired
    @Qualifier("openelisFhirClient") private IGenericClient openelisFhirClient;

    public Task sendTask(Task task) {
        MethodOutcome methodOutcome =
                openelisFhirClient.update().resource(task).encodedJson().execute();

        log.debug("OpenelisTaskHandler: Task created {}", methodOutcome.getCreated());

        return (Task) methodOutcome.getResource();
    }

    public Task getTaskByServiceRequestID(String serviceRequestID) {
        Bundle bundle = openelisFhirClient
                .search()
                .forResource(Task.class)
                .returnBundle(Bundle.class)
                .execute();

        log.info("OpenelisTaskHandler: Task getTaskByServiceRequestID {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Task.class::isInstance)
                .map(Task.class::cast)
                .filter(task -> task.getBasedOn()
                        .get(0)
                        .getReference()
                        .equals("ServiceRequest/"
                                + serviceRequestID)) // TODO: Use client impl and don't fetch all Tasks
                .findFirst()
                .orElse(null);
    }

    public void deleteTask(String taskID) {
        MethodOutcome methodOutcome = openelisFhirClient
                .delete()
                .resourceById(new IdType("Task", taskID))
                .execute();

        log.debug("OpenelisServiceRequestHandler: deleteTask {}", methodOutcome.getCreated());
    }

    public boolean doesTaskExists(Task task) {
        return task != null && task.getId() != null && !task.getId().isEmpty() && task.getStatus() != null;
    }

    public Task buildTask(ServiceRequest serviceRequest, Encounter encounter) {
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

        openelisTask.setLocation(encounter.getLocationFirstRep().getLocation());

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
