/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirObservation;

import com.ozonehis.eip.openelis.openmrs.Constants;
import com.ozonehis.eip.openelis.openmrs.client.OpenelisFhirClient;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GetOpenelisFhirObservationRoute extends RouteBuilder {

    @Autowired
    private OpenelisFhirClient openelisFhirClient;

    public static final String GET_ENDPOINT = "/fhir/Observation/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-get-observation-route")
                .log(LoggingLevel.INFO, "Fetching Observation in OpenELIS...")
                .routeId("openelis-get-observation-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.GET))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openelisFhirClient.authHeader()))
                .toD(openelisFhirClient.getOpenelisFhirBaseUrl()
                        + GET_ENDPOINT + "${header." + Constants.HEADER_OBSERVATION_ID + "}")
                .end();
        // spotless:on
    }
}
