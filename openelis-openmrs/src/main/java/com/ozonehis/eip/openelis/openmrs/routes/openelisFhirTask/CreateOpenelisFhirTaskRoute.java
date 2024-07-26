/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirTask;

import com.ozonehis.eip.openelis.openmrs.Constants;
import com.ozonehis.eip.openelis.openmrs.client.OpenelisFhirClient;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreateOpenelisFhirTaskRoute extends RouteBuilder {

    @Autowired
    private OpenelisFhirClient openelisFhirClient;

    public static final String CREATE_ENDPOINT = "/fhir/Task/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-create-task-route")
            .log(LoggingLevel.INFO, "Creating Task in OpenELIS...")
            .routeId("openelis-create-task-route")
            .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.PUT))
            .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
            .setHeader(Constants.AUTHORIZATION, constant(openelisFhirClient.authHeader()))
            .toD(openelisFhirClient.getOpenelisFhirBaseUrl() + CREATE_ENDPOINT + "${header."
                        + Constants.HEADER_TASK_ID + "}")
                .end();
        // spotless:on
    }
}
