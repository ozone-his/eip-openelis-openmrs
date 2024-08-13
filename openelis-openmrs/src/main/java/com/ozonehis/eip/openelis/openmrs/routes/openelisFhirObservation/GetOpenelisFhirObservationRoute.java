/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirObservation;

import com.ozonehis.eip.openelis.openmrs.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.stereotype.Component;

@Component
public class GetOpenelisFhirObservationRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-get-observation-route")
                .log(LoggingLevel.INFO, "Fetching Observation in OpenELIS...")
                .routeId("openelis-get-observation-route")
                .toD("openelisfhir:read/resourceById?resourceClass=Observation&stringId=" + "${header."
                        + Constants.HEADER_OBSERVATION_ID + "}")
                .marshal()
                .fhirJson("R4")
                .convertBodyTo(Observation.class)
                .end();
        // spotless:on
    }
}
