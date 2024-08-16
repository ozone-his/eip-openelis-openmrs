/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsDiagnosticReportHandler {

    public DiagnosticReport sendDiagnosticReport(ProducerTemplate producerTemplate, DiagnosticReport diagnosticReport) {

        return producerTemplate.requestBody(
                "direct:openmrs-create-resource-route", diagnosticReport, DiagnosticReport.class);
    }

    public DiagnosticReport buildDiagnosticReport(
            ServiceRequest openmrsServiceRequest,
            ArrayList<String> observationUuids,
            DiagnosticReport openelisDiagnosticReport) {
        DiagnosticReport openmrsDiagnosticReport = new DiagnosticReport();

        openmrsDiagnosticReport
                .addBasedOn()
                .setReference(openmrsServiceRequest.getIdPart())
                .setType("ServiceRequest");
        openmrsDiagnosticReport.setStatus(DiagnosticReport.DiagnosticReportStatus.FINAL);
        openmrsDiagnosticReport.setCode(openelisDiagnosticReport.getCode());
        openmrsDiagnosticReport.setSubject(openmrsServiceRequest.getSubject());

        List<Reference> referenceList = new ArrayList<>();
        for (String observationUuid : observationUuids) {
            referenceList.add(new Reference().setReference("Observation/" + observationUuid));
        }
        openmrsDiagnosticReport.setResult(referenceList);

        return openmrsDiagnosticReport;
    }
}
