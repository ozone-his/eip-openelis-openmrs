/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenmrsDiagnosticReportHandler {

    @Autowired
    @Qualifier("openmrsFhirClient") private IGenericClient openmrsFhirClient;

    public void sendDiagnosticReport(DiagnosticReport diagnosticReport) {
        MethodOutcome methodOutcome = openmrsFhirClient
                .create()
                .resource(diagnosticReport)
                .encodedJson()
                .execute();

        log.debug("OpenmrsDiagnosticReportHandler: DiagnosticReport created {}", methodOutcome.getCreated());
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
