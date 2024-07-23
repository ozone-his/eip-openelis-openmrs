/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.openelis.openmrs.converter.FhirResourceConverter;
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

    @Autowired
    private FhirResourceConverter fhirResourceConverter;

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(fhirResourceConverter);
        // spotless:off
        from("direct:openmrs-patient-to-openelis-patient-router")
            .routeId("openmrs-patient-to-openelis-patient-router")
            .filter(exchange -> exchange.getMessage().getBody() instanceof Patient)
            .log(LoggingLevel.INFO, "Processing Patient")
            .process(patientProcessor)
            .choice()
                .when(header(HEADER_FHIR_EVENT_TYPE).isEqualTo("c"))
                    .toD("direct:openelis-create-patient-route")
                .endChoice()
            .end();

        from("direct:fhir-patient")
            .routeId("fhir-openmrs-patient-to-openelis-patient-router")
            .to("direct:openmrs-patient-to-openelis-patient-router")
                .end();
        // spotless:on
    }
}
