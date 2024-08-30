/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.processors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisDiagnosticReportHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisObservationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisTaskHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsDiagnosticReportHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsObservationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsTaskHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class TaskProcessorTest extends BaseProcessorTest {

    @Mock
    private OpenmrsTaskHandler openmrsTaskHandler;

    @Mock
    private OpenmrsServiceRequestHandler openmrsServiceRequestHandler;

    @Mock
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

    @Mock
    private OpenelisTaskHandler openelisTaskHandler;

    @Mock
    private OpenelisDiagnosticReportHandler openelisDiagnosticReportHandler;

    @Mock
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;

    @Mock
    private OpenelisObservationHandler openelisObservationHandler;

    @Mock
    private OpenmrsObservationHandler openmrsObservationHandler;

    @InjectMocks
    private TaskProcessor taskProcessor;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldCreateResultsInOpenmrsWhenTaskIsCompleted() {
        // Arrange
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);

        Task openmrsTask = new Task();
        openmrsTask.setId(UUID.randomUUID().toString());
        openmrsTask.setStatus(Task.TaskStatus.ACCEPTED);
        openmrsTask.setBasedOn(Collections.singletonList(new Reference().setReference(serviceRequest.getIdPart())));

        String diagnosticReportID = UUID.randomUUID().toString();
        Task openelisTask = new Task();
        openelisTask.setId(UUID.randomUUID().toString());
        openelisTask.setStatus(Task.TaskStatus.COMPLETED);
        openelisTask.setBasedOn(Collections.singletonList(
                new Reference().setReference("ServiceRequest/" + serviceRequest.getIdPart())));
        openelisTask.setOutput(Collections.singletonList(new Task.TaskOutputComponent()
                .setValue(new Reference().setReference("DiagnosticReport/" + diagnosticReportID))));

        String observationID = UUID.randomUUID().toString();
        DiagnosticReport openelisDiagnosticReport = new DiagnosticReport();
        openelisDiagnosticReport.setId(diagnosticReportID);
        openelisDiagnosticReport.setResult(
                Collections.singletonList(new Reference().setReference("Observation/" + observationID)));

        Observation openelisObservation = new Observation();
        openelisObservation.setId(observationID);
        openelisObservation.setStatus(Observation.ObservationStatus.FINAL);

        Observation openmrsObservation = openelisObservation;

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(openmrsTask));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "c");

        // Mock behavior
        when(openmrsTaskHandler.doesTaskExists(any())).thenReturn(true);
        when(openmrsServiceRequestHandler.getServiceRequestByID(any())).thenReturn(serviceRequest);
        when(openelisTaskHandler.getTaskByServiceRequestID(any())).thenReturn(openelisTask);
        when(openelisDiagnosticReportHandler.getDiagnosticReportByDiagnosticReportID(any()))
                .thenReturn(openelisDiagnosticReport);
        when(openelisObservationHandler.getObservationByObservationID(any())).thenReturn(openelisObservation);
        when(openmrsObservationHandler.sendObservation(any())).thenReturn(openmrsObservation);
        when(openmrsObservationHandler.buildObservation(any(), any())).thenReturn(openmrsObservation);

        // Act
        taskProcessor.process(exchange);

        verify(openmrsTaskHandler, times(1)).doesTaskExists(any());
        verify(openmrsServiceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(openelisTaskHandler, times(1)).getTaskByServiceRequestID(any());
        verify(openelisDiagnosticReportHandler, times(1)).getDiagnosticReportByDiagnosticReportID(any());
        verify(openelisObservationHandler, times(1)).getObservationByObservationID(any());
        verify(openmrsObservationHandler, times(1)).sendObservation(any());
        verify(openmrsObservationHandler, times(1)).buildObservation(any(), any());
        verify(openmrsDiagnosticReportHandler, times(1)).sendDiagnosticReport(any());
        verify(openmrsDiagnosticReportHandler, times(1)).buildDiagnosticReport(any(), any(), any());
        verify(openmrsTaskHandler, times(1)).updateTask(any(), any());
        verify(openmrsTaskHandler, times(1)).markTaskCompleted(any());
    }

    @Test
    void shouldNotCreateResultsInOpenmrsWhenTaskIsNotCompleted() {
        // Arrange
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(UUID.randomUUID().toString());
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);

        Task openmrsTask = new Task();
        openmrsTask.setId(UUID.randomUUID().toString());
        openmrsTask.setStatus(Task.TaskStatus.ACCEPTED);
        openmrsTask.setBasedOn(Collections.singletonList(new Reference().setReference(serviceRequest.getIdPart())));

        String diagnosticReportID = UUID.randomUUID().toString();
        Task openelisTask = new Task();
        openelisTask.setId(UUID.randomUUID().toString());
        openelisTask.setStatus(Task.TaskStatus.ACCEPTED);
        openelisTask.setBasedOn(Collections.singletonList(
                new Reference().setReference("ServiceRequest/" + serviceRequest.getIdPart())));
        openelisTask.setOutput(Collections.singletonList(new Task.TaskOutputComponent()
                .setValue(new Reference().setReference("DiagnosticReport/" + diagnosticReportID))));

        String observationID = UUID.randomUUID().toString();
        DiagnosticReport openelisDiagnosticReport = new DiagnosticReport();
        openelisDiagnosticReport.setId(diagnosticReportID);
        openelisDiagnosticReport.setResult(
                Collections.singletonList(new Reference().setReference("Observation/" + observationID)));

        Observation openelisObservation = new Observation();
        openelisObservation.setId(observationID);
        openelisObservation.setStatus(Observation.ObservationStatus.FINAL);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(openmrsTask));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "c");

        // Mock behavior
        when(openmrsTaskHandler.doesTaskExists(any())).thenReturn(true);
        when(openmrsServiceRequestHandler.getServiceRequestByID(any())).thenReturn(serviceRequest);
        when(openelisTaskHandler.getTaskByServiceRequestID(any())).thenReturn(openelisTask);

        // Act
        taskProcessor.process(exchange);

        verify(openmrsTaskHandler, times(1)).doesTaskExists(any());
        verify(openmrsServiceRequestHandler, times(1)).getServiceRequestByID(any());
        verify(openelisTaskHandler, times(1)).getTaskByServiceRequestID(any());
        verify(openelisDiagnosticReportHandler, times(0)).getDiagnosticReportByDiagnosticReportID(any());
        verify(openelisObservationHandler, times(0)).getObservationByObservationID(any());
        verify(openmrsObservationHandler, times(0)).sendObservation(any());
        verify(openmrsObservationHandler, times(0)).buildObservation(any(), any());
        verify(openmrsDiagnosticReportHandler, times(0)).sendDiagnosticReport(any());
        verify(openmrsDiagnosticReportHandler, times(0)).buildDiagnosticReport(any(), any(), any());
        verify(openmrsTaskHandler, times(0)).updateTask(any(), any());
        verify(openmrsTaskHandler, times(0)).markTaskCompleted(any());
    }
}
