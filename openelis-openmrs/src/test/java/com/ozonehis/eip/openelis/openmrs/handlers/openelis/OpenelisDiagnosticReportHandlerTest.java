/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;

class OpenelisDiagnosticReportHandlerTest {

    private static final String DIAGNOSTIC_REPORT_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

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
    public void shouldReturnDiagnosticReportGivenDiagnosticReportID() {
        // Setup
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_DIAGNOSTIC_REPORT_ID, DIAGNOSTIC_REPORT_ID);

        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(DIAGNOSTIC_REPORT_ID);

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(producerTemplate.requestBodyAndHeaders(
                        "direct:openelis-get-diagnostic-report-route", null, headers, DiagnosticReport.class))
                .thenReturn(diagnosticReport);

        // Act
        DiagnosticReport result = openelisDiagnosticReportHandler.getDiagnosticReportByDiagnosticReportID(
                producerTemplate, DIAGNOSTIC_REPORT_ID);

        // Verify
        assertEquals(diagnosticReport, result);
        verify(producerTemplate, times(1))
                .requestBodyAndHeaders(
                        eq("direct:openelis-get-diagnostic-report-route"),
                        eq(null),
                        eq(headers),
                        eq(DiagnosticReport.class));
    }
}
