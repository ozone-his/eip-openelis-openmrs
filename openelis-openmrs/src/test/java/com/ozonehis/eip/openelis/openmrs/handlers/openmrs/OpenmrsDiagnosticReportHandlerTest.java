/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

class OpenmrsDiagnosticReportHandlerTest {
    private static final String DIAGNOSTIC_REPORT_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

    private static final String SERVICE_REQUEST_ID = "45d050e1-c1be-4b4c-b407-c48d2db49b34";

    private static final String CODE_UUID = "74AAAAAAAAAAAAAAAAAA";

    @InjectMocks
    private OpenmrsDiagnosticReportHandler openmrsDiagnosticReportHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldCallCreateRouteAndReturnDiagnosticReportGivenDiagnosticReport() {
        // Setup
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(DIAGNOSTIC_REPORT_ID);

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(producerTemplate.requestBody(
                        "direct:openmrs-create-resource-route", diagnosticReport, DiagnosticReport.class))
                .thenReturn(diagnosticReport);

        // Act
        DiagnosticReport result =
                openmrsDiagnosticReportHandler.sendDiagnosticReport(producerTemplate, diagnosticReport);

        // Verify
        assertEquals(diagnosticReport, result);
        verify(producerTemplate, times(1))
                .requestBody(
                        eq("direct:openmrs-create-resource-route"), eq(diagnosticReport), eq(DiagnosticReport.class));
    }

    @Test
    void shouldReturnOpenmrsDiagnosticReportGivenOpenelisServiceRequestObservationUuidsAndDiagnosticReport() {
        // Setup
        DiagnosticReport openelisDiagnosticReport = new DiagnosticReport();
        openelisDiagnosticReport.setId(DIAGNOSTIC_REPORT_ID);
        openelisDiagnosticReport.setCode(new CodeableConcept().addCoding(new Coding().setCode(CODE_UUID)));

        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(SERVICE_REQUEST_ID);
        serviceRequest.setSubject(new Reference().setReference("Patient/65d050e1-c1be-4b4c-b407-c48d2db49b44"));

        ArrayList<String> observationUuids = new ArrayList<>();
        observationUuids.add("15d050e1-c1be-4b4c-b407-c48d2db49b43");
        observationUuids.add("25d050e1-c1be-4b4c-b407-c48d2db49b42");
        observationUuids.add("35d050e1-c1be-4b4c-b407-c48d2db49b41");

        // Act
        DiagnosticReport result = openmrsDiagnosticReportHandler.buildDiagnosticReport(
                serviceRequest, observationUuids, openelisDiagnosticReport);

        // Verify
        assertEquals(result.getStatus(), DiagnosticReport.DiagnosticReportStatus.FINAL);
        assertFalse(result.getBasedOn().isEmpty());
    }
}
