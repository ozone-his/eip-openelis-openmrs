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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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

                        String labNumber = "DEV" + Math.round(Math.random() * 1000000);
                        patientHandler.sendPatient(producerTemplate, patientHandler.buildPatient(patient));
                        serviceRequestHandler.sendServiceRequest(
                                producerTemplate, buildServiceRequest(serviceRequest, labNumber));
                        taskHandler.sendTask(producerTemplate, buildTask(serviceRequest, labNumber));

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

    private ServiceRequest buildServiceRequest(ServiceRequest serviceRequest, String labNumber) {
        ServiceRequest openelisServiceRequest = new ServiceRequest();
        openelisServiceRequest.setId(serviceRequest.getIdPart());

        Identifier analysisUuid = new Identifier();
        analysisUuid.setSystem("http://openelis-global.org/analysis_uuid");
        analysisUuid.setValue(serviceRequest.getIdPart());

        Identifier sampleLabNumber = new Identifier();
        sampleLabNumber.setSystem("http://openelis-global.org/samp_labNo");
        sampleLabNumber.setValue(labNumber);

        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(analysisUuid);
        identifierList.add(sampleLabNumber);

        openelisServiceRequest.setIdentifier(identifierList);
        openelisServiceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        openelisServiceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        Coding coding = new Coding();
        coding.setSystem("http://openelis-global.org/sample_program");
        coding.setCode("Routine Testing");
        coding.setDisplay("Routine Testing");

        openelisServiceRequest.setCategory(
                Collections.singletonList(new CodeableConcept().setCoding(Collections.singletonList(coding))));
        openelisServiceRequest.setPriority(ServiceRequest.ServiceRequestPriority.ROUTINE);
        openelisServiceRequest.setCode(new CodeableConcept()
                .setCoding(Collections.singletonList(
                        new Coding().setSystem("http://loinc.org").setDisplay("Albumin"))));
        openelisServiceRequest.setSubject(serviceRequest.getSubject());
        openelisServiceRequest.setAuthoredOn(serviceRequest.getAuthoredOn());
        // TODO: Add locationReference
        // TODO: Check if Specimen reference is required

        return openelisServiceRequest;
    }

    private Task buildTask(ServiceRequest serviceRequest, String accessionNumber) {
        Task openelisTask = new Task();
        String taskUuid = UUID.randomUUID().toString();
        openelisTask.setId(taskUuid);

        Identifier orderUuid = new Identifier();
        orderUuid.setSystem("http://openelis-global.org/order_uuid");
        orderUuid.setValue(taskUuid);

        openelisTask.setIdentifier(Collections.singletonList(orderUuid));

        Identifier accessionNumberIdentifier = new Identifier();
        accessionNumberIdentifier.setSystem("http://openelis-global.org/order_accessionNumber");
        accessionNumberIdentifier.setValue(accessionNumber);

        List<Identifier> identifierList = new ArrayList<>();
        identifierList.add(orderUuid);
        identifierList.add(accessionNumberIdentifier);

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
        ownerReference.setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed"); // TODO: Remove hardcode
        ownerReference.setType("Practitioner");

        openelisTask.setOwner(ownerReference);

        return openelisTask;
    }
}
