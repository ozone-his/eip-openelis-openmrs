/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisDiagnosticReportHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisObservationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisTaskHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsDiagnosticReportHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsObservationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsTaskHandler;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Getter
@Component
public class TaskProcessor implements Processor {

    @Autowired
    private OpenmrsTaskHandler openmrsTaskHandler;

    @Autowired
    private OpenmrsServiceRequestHandler openmrsServiceRequestHandler;

    @Autowired
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

    @Autowired
    private OpenelisTaskHandler openelisTaskHandler;

    @Autowired
    private OpenelisDiagnosticReportHandler openelisDiagnosticReportHandler;

    @Autowired
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;

    @Autowired
    private OpenelisObservationHandler openelisObservationHandler;

    @Autowired
    private OpenmrsObservationHandler openmrsObservationHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();
            for (Bundle.BundleEntryComponent entry : entries) {
                Task openmrsTask = null;
                Resource resource = entry.getResource();
                if (resource instanceof Task) {
                    openmrsTask = (Task) resource;
                }

                if (!openmrsTaskHandler.doesTaskExists(openmrsTask)) {
                    continue;
                }
                String taskBasedOnServiceRequestID =
                        openmrsTask.getBasedOn().get(0).getReference();
                ServiceRequest openmrsServiceRequest =
                        openmrsServiceRequestHandler.getServiceRequestByID(taskBasedOnServiceRequestID);
                if (openmrsServiceRequest.getStatus() != ServiceRequest.ServiceRequestStatus.REVOKED) {

                    Task openelisTask = openelisTaskHandler.getTaskByServiceRequestID(taskBasedOnServiceRequestID);

                    if (openelisTask.getStatus() == Task.TaskStatus.COMPLETED
                            && openmrsTask.getStatus() != Task.TaskStatus.COMPLETED) {
                        updateLabOrderResultsInOpenmrs(openelisTask, openmrsServiceRequest);

                        openmrsTaskHandler.updateTask(
                                openmrsTaskHandler.markTaskCompleted(openmrsTask), openmrsTask.getIdPart());
                    }
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing Task", exchange, e);
        }
    }

    private void updateLabOrderResultsInOpenmrs(Task openelisTask, ServiceRequest openmrsServiceRequest) {
        for (Task.TaskOutputComponent taskOutputComponent : openelisTask.getOutput()) {
            Type value = taskOutputComponent.getValue();
            if (value instanceof Reference reference) {
                String openelisDiagnosticReportID = reference.getReference().split("/")[1];
                DiagnosticReport openelisDiagnosticReport =
                        openelisDiagnosticReportHandler.getDiagnosticReportByDiagnosticReportID(
                                openelisDiagnosticReportID);

                if (openelisDiagnosticReport == null
                        || openelisDiagnosticReport.getId().isEmpty()) {
                    continue;
                }
                ArrayList<String> observationUuids = new ArrayList<>();
                for (Reference observationReference : openelisDiagnosticReport.getResult()) {
                    String openelisObservationID =
                            observationReference.getReference().split("/")[1];
                    Observation openelisObservation =
                            openelisObservationHandler.getObservationByObservationID(openelisObservationID);

                    Observation savedOpenmrsObservation = openmrsObservationHandler.sendObservation(
                            openmrsObservationHandler.buildObservation(openmrsServiceRequest, openelisObservation));
                    observationUuids.add(savedOpenmrsObservation.getIdPart());
                }
                openmrsDiagnosticReportHandler.sendDiagnosticReport(
                        openmrsDiagnosticReportHandler.buildDiagnosticReport(
                                openmrsServiceRequest, observationUuids, openelisDiagnosticReport));
            }
        }
    }
}
