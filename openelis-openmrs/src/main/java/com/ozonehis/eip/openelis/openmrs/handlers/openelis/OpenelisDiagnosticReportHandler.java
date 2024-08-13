/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.handlers.openelis;

import com.ozonehis.eip.openelis.openmrs.Constants;
import java.util.HashMap;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class OpenelisDiagnosticReportHandler {

    public DiagnosticReport getDiagnosticReportByDiagnosticReportID(
            ProducerTemplate producerTemplate, String diagnosticReportID) {
        Map<String, Object> headers = new HashMap<>();
        headers.put(Constants.HEADER_DIAGNOSTIC_REPORT_ID, diagnosticReportID);

        return producerTemplate.requestBodyAndHeaders(
                "direct:openelis-get-diagnostic-report-route", null, headers, DiagnosticReport.class);
    }
}
