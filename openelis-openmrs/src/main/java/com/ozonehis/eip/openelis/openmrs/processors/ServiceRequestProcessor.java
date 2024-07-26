/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.PatientHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.ServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.TaskHandler;
import java.util.Collections;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.lang3.RandomStringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ServiceRequestProcessor implements Processor {

    @Autowired
    private ServiceRequestHandler serviceRequestHandler;

    @Autowired
    private TaskHandler taskHandler;

    @Autowired
    private PatientHandler patientHandler;

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
                String serviceRequestUuid = serviceRequest.getIdPart();
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (serviceRequest.getStatus().equals(ServiceRequest.ServiceRequestStatus.ACTIVE)
                            && serviceRequest.getIntent().equals(ServiceRequest.ServiceRequestIntent.ORDER)) {

                        String taskID = "671ee2f8-ced1-411f-aadf-"
                                + RandomStringUtils.random(12, true, true).toLowerCase();
                        Task task = new Task();
                        task.setId(taskID);
                        task.setIdentifier(Collections.singletonList(new Identifier()
                                .setSystem("http://fhir.openmrs.org/ext/task/identifier")
                                .setValue(taskID)));

                        Reference serviceRequestReference = new Reference();
                        serviceRequestReference.setReference("ServiceRequest/" + serviceRequestUuid);
                        serviceRequestReference.setType("ServiceRequest");

                        task.setBasedOn(Collections.singletonList(serviceRequestReference));
                        task.setStatus(Task.TaskStatus.REQUESTED);
                        task.setIntent(Task.TaskIntent.ORDER);
                        task.setFor(serviceRequest.getSubject());
                        task.setEncounter(serviceRequest.getEncounter());

                        Reference ownerReference = new Reference();
                        ownerReference.setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed");
                        ownerReference.setType("Practitioner");

                        task.setOwner(ownerReference);

                        patientHandler.sendPatient(producerTemplate, patient);
                        serviceRequestHandler.sendServiceRequest(producerTemplate, serviceRequest);
                        taskHandler.sendTask(producerTemplate, task);

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
}
