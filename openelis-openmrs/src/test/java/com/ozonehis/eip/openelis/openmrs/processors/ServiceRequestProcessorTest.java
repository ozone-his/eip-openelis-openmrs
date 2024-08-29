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

import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisLocationHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPatientHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisPractitionerHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisServiceRequestHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openelis.OpenelisTaskHandler;
import com.ozonehis.eip.openelis.openmrs.handlers.openmrs.OpenmrsTaskHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ServiceRequestProcessorTest extends BaseProcessorTest {
    private static final String ENCOUNTER_REFERENCE_ID = "Encounter/1234";

    @Mock
    private OpenelisServiceRequestHandler openelisServiceRequestHandler;

    @Mock
    private OpenelisTaskHandler openelisTaskHandler;

    @Mock
    private OpenelisPatientHandler openelisPatientHandler;

    @Mock
    private OpenelisPractitionerHandler openelisPractitionerHandler;

    @Mock
    private OpenmrsTaskHandler openmrsTaskHandler;

    @Mock
    private OpenelisLocationHandler openelisLocationHandler;

    @InjectMocks
    private ServiceRequestProcessor serviceRequestProcessor;

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
    void shouldCreateResourcesInOpenelisWhenTaskDoesNotExists() {
        // Arrange
        Patient patient = new Patient();
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setRequester(new Reference().setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed"));

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "u");

        // Mock behavior
        when(openelisTaskHandler.getTaskByServiceRequestID(any())).thenReturn(null);

        // Act
        serviceRequestProcessor.process(exchange);

        // Assert
        verify(openelisTaskHandler, times(1)).getTaskByServiceRequestID(any());
        verify(openelisTaskHandler, times(1)).doesTaskExists(any());
        verify(openelisPractitionerHandler, times(1)).sendPractitioner(any());
        verify(openelisPractitionerHandler, times(1)).buildPractitioner(any());
        verify(openelisPatientHandler, times(1)).sendPatient(any());
        verify(openelisPatientHandler, times(1)).buildPatient(any());
        verify(openelisServiceRequestHandler, times(1)).sendServiceRequest(any());
        verify(openelisLocationHandler, times(1)).sendLocation(any());
        verify(openelisLocationHandler, times(1)).buildLocation(any());
        verify(openelisTaskHandler, times(1)).sendTask(any());
        verify(openelisTaskHandler, times(1)).buildTask(any(), any());
        verify(openmrsTaskHandler, times(1)).sendTask(any());
        verify(openmrsTaskHandler, times(1)).buildTask(any());
    }

    @Test
    void shouldNotSaveResourcesInOpenelisWhenTaskAlreadyExists() {
        // Arrange
        Patient patient = new Patient();
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);
        serviceRequest.setRequester(new Reference().setReference("Practitioner/671ee2f8-ced1-411f-aadf-d12fe1e6f2ed"));

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "u");

        Task openelisTask = new Task();
        openelisTask.setId(UUID.randomUUID().toString());
        openelisTask.setStatus(Task.TaskStatus.RECEIVED);

        // Mock behavior
        when(openelisTaskHandler.getTaskByServiceRequestID(any())).thenReturn(openelisTask);
        when(openelisTaskHandler.doesTaskExists(any())).thenReturn(true);

        // Act
        serviceRequestProcessor.process(exchange);

        // Assert
        verify(openelisTaskHandler, times(1)).getTaskByServiceRequestID(any());
        verify(openelisTaskHandler, times(1)).doesTaskExists(any());
        verify(openelisPractitionerHandler, times(0)).sendPractitioner(any());
        verify(openelisPractitionerHandler, times(0)).buildPractitioner(any());
        verify(openelisPatientHandler, times(0)).sendPatient(any());
        verify(openelisPatientHandler, times(0)).buildPatient(any());
        verify(openelisServiceRequestHandler, times(0)).sendServiceRequest(any());
        verify(openelisLocationHandler, times(0)).sendLocation(any());
        verify(openelisLocationHandler, times(0)).buildLocation(any());
        verify(openelisTaskHandler, times(0)).sendTask(any());
        verify(openelisTaskHandler, times(0)).buildTask(any(), any());
        verify(openmrsTaskHandler, times(0)).sendTask(any());
        verify(openmrsTaskHandler, times(0)).buildTask(any());
    }
}
