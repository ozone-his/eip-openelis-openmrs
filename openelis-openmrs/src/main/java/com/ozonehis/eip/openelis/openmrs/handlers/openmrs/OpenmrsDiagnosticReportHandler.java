/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.context.FhirContext;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsDiagnosticReportHandler {

    public DiagnosticReport sendDiagnosticReport(ProducerTemplate producerTemplate, DiagnosticReport diagnosticReport) {
        String response = producerTemplate.requestBody(
                "direct:openmrs-create-diagnostic-report-route", diagnosticReport, String.class);
        FhirContext ctx = FhirContext.forR4();
        DiagnosticReport savedDiagnosticReport = ctx.newJsonParser().parseResource(DiagnosticReport.class, response);
        return savedDiagnosticReport;
    }
}
