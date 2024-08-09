/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CreateOpenelisFhirResourceRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-create-resource-route")
                .log(LoggingLevel.INFO, "Creating Resource in OpenELIS...")
                .routeId("openelis-create-resource-route")
                .marshal().fhirJson("R4")
                .convertBodyTo(String.class)
                .log("Creating resource ${body}")
                .to("openelisfhir://update/resource?inBody=resourceAsString")
                .marshal().fhirJson("R4")
                .convertBodyTo(String.class)
                .log("Created resource ${body}")
                .end();
        // spotless:on
    }
}
