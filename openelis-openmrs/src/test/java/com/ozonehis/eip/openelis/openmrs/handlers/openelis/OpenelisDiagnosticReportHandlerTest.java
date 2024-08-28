/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IRead;
import ca.uhn.fhir.rest.gclient.IReadExecutable;
import ca.uhn.fhir.rest.gclient.IReadTyped;
import java.util.UUID;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class OpenelisDiagnosticReportHandlerTest {

    @Mock
    private IGenericClient openelisFhirClient;

    @Mock
    private IRead iRead;

    @Mock
    private IReadTyped<DiagnosticReport> iReadTyped;

    @Mock
    private IReadExecutable<DiagnosticReport> iReadExecutable;

    @InjectMocks
    private OpenelisDiagnosticReportHandler openelisDiagnosticReportHandler;

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
    void getDiagnosticReportByDiagnosticReportID() {
        // Setup
        String diagnosticReportID = UUID.randomUUID().toString();
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(diagnosticReportID);

        // Mock behavior
        when(openelisFhirClient.read()).thenReturn(iRead);
        when(iRead.resource(DiagnosticReport.class)).thenReturn(iReadTyped);
        when(iReadTyped.withId(diagnosticReportID)).thenReturn(iReadExecutable);
        when(iReadExecutable.execute()).thenReturn(diagnosticReport);

        // Act
        DiagnosticReport result =
                openelisDiagnosticReportHandler.getDiagnosticReportByDiagnosticReportID(diagnosticReportID);

        // Verify
        assertNotNull(result);
        assertEquals(diagnosticReportID, result.getId());
    }
}
