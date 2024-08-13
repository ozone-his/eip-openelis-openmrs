/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirTask;

import com.ozonehis.eip.openelis.openmrs.Constants;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Component;

@Component
public class GetOpenelisFhirTaskRoute extends RouteBuilder {

    public static final String GET_ENDPOINT = "/Task?based-on:ServiceRequest=";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-get-task-route")
                .log(LoggingLevel.INFO, "Fetching Task in OpenELIS...")
                .routeId("openelis-get-task-route")
                .toD("openelisfhir://search/searchByUrl?url=" + GET_ENDPOINT + "${header."
                        + Constants.HEADER_SERVICE_REQUEST_ID + "}")
                .marshal()
                .fhirJson("R4")
                .convertBodyTo(Bundle.class)
                .end();
        // spotless:on
    }
}
