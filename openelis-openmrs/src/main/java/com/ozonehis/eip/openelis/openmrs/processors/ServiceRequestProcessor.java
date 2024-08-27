/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisLocationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPatientHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPractitionerHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisTaskHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsTaskHandler;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
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
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

    @Autowired
    private OpenelisTaskHandler openelisTaskHandler;

    @Autowired
    private OpenelisPatientHandler openelisPatientHandler;

    @Autowired
    private OpenelisPractitionerHandler openelisPractitionerHandler;

    @Autowired
    private OpenmrsTaskHandler openmrsTaskHandler;

    @Autowired
    private OpenelisLocationHandler openelisLocationHandler;

    @Override
    public void process(Exchange exchange) {
        try {
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
                        Task fetchedTask = openelisTaskHandler.getTaskByServiceRequestID(serviceRequest.getIdPart());
                        if (openelisTaskHandler.doesTaskExists(fetchedTask)) {
                            log.info("Task already exists for ServiceRequest {}", serviceRequest.getIdPart());
                            return;
                        }

                        // TODO: Remove when OpenELIS accepts wildcards `Practitioner/*`
                        Reference requesterReference = serviceRequest.getRequester();
                        requesterReference.setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed");
                        serviceRequest.setRequester(requesterReference);

                        openelisPractitionerHandler.sendPractitioner(
                                openelisPractitionerHandler.buildPractitioner(serviceRequest));
                        openelisPatientHandler.sendPatient(openelisPatientHandler.buildPatient(patient));
                        openelisServiceRequestHandler.sendServiceRequest(serviceRequest);
                        openelisLocationHandler.sendLocation(openelisLocationHandler.buildLocation(
                                encounter.getLocationFirstRep().getLocation()));
                        openelisTaskHandler.sendTask(openelisTaskHandler.buildTask(serviceRequest, encounter));
                        openmrsTaskHandler.sendTask(openmrsTaskHandler.buildTask(serviceRequest));

                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                        cancelOpenelisLabOrder(serviceRequest.getIdPart());
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS
                    cancelOpenelisLabOrder(serviceRequest.getIdPart());
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing ServiceRequest", exchange, e);
        }
    }

    private void cancelOpenelisLabOrder(String serviceRequestID) {
        // Delete OpenELIS Task and then Delete OpenELIS ServiceRequest
        Task openelisTask = openelisTaskHandler.getTaskByServiceRequestID(serviceRequestID);
        if (openelisTaskHandler.doesTaskExists(openelisTask) && openelisTask.getStatus() == Task.TaskStatus.REQUESTED) {
            openelisTaskHandler.deleteTask(openelisTask.getIdPart());
            openelisServiceRequestHandler.deleteServiceRequest(serviceRequestID);
        }

        // Reject OpenMRS Task
        openmrsTaskHandler.rejectTaskByServiceRequestID(serviceRequestID);
    }
}
