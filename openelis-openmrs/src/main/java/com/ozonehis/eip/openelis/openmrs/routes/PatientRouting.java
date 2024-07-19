/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import com.ozonehis.eip.openelis.openmrs.processors.PatientProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class PatientRouting extends RouteBuilder {

    @Autowired
    private PatientProcessor patientProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:patient-to-partner-router")
            .routeId("patient-to-partner-router")
            .filter(exchange -> exchange.getMessage().getBody() instanceof Patient)
            .log(LoggingLevel.INFO, "Processing Patient")
            .process(patientProcessor)
                //TODO: Implement PatientRouting
                .end();

        from("direct:fhir-patient")
            .routeId("fhir-patient-to-partner-router")
            .to("direct:patient-to-partner-router")
                .end();
        // spotless:on
    }
}
