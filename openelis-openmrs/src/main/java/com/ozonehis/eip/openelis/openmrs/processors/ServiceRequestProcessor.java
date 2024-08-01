/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPatientHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPractitionerHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisTaskHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsTaskHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ServiceRequestProcessor implements Processor {

    @Autowired
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

    @Autowired
    private OpenelisTaskHandler openelisTaskHandler;

    @Autowired
    private OpenelisPatientHandler openelisPatientHandler;

    @Autowired
    private OpenelisPractitionerHandler openelisPractitionerHandler;

    @Autowired
    private OpenmrsTaskHandler openmrsTaskHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            ServiceRequest serviceRequest = null;
            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof ServiceRequest) {
                    serviceRequest = (ServiceRequest) resource;
                }
            }

            if (patient == null || encounter == null || serviceRequest == null) {
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient, Encounter and ServiceRequest", exchange);
            } else {
                log.debug("Processing ServiceRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage()
                        .getHeader(org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                            && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {

                        String[] nameSplit =
                                serviceRequest.getRequester().getDisplay().split(" ");

                        Reference requesterReference = serviceRequest.getRequester();
                        requesterReference.setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed");
                        serviceRequest.setRequester(requesterReference);

                        Practitioner practitioner = new Practitioner();
                        practitioner.setActive(true);
                        practitioner.setId(
                                serviceRequest.getRequester().getReference().split("/")[1]);
                        practitioner.setName(Collections.singletonList(new HumanName()
                                .setFamily(nameSplit[1])
                                .setGiven(Collections.singletonList(new StringType(nameSplit[0])))));

                        openelisPractitionerHandler.sendPractitioner(producerTemplate, practitioner);
                        openelisPatientHandler.sendPatient(
                                producerTemplate, openelisPatientHandler.buildPatient(patient));
                        openelisServiceRequestHandler.sendServiceRequest(producerTemplate, serviceRequest);
                        Task savedOpenelisTask =
                                openelisTaskHandler.sendTask(producerTemplate, buildTask(serviceRequest));
                        openmrsTaskHandler.sendTask(producerTemplate, buildTask(serviceRequest));

                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing ServiceRequest", exchange, e);
        }
    }

    private Task buildTask(ServiceRequest serviceRequest) {
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

        log.info("buildTask: Patient reference {}", serviceRequest.getSubject().getReference());

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
