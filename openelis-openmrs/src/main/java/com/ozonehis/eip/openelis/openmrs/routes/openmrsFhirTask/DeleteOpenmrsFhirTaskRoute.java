/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openmrsFhirTask;

import com.ozonehis.eip.openelis.openmrs.Constants;
import com.ozonehis.eip.openelis.openmrs.client.OpenmrsFhirClient;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeleteOpenmrsFhirTaskRoute extends RouteBuilder {

    @Autowired
    private OpenmrsFhirClient openmrsFhirClient;

    public static final String DELETE_ENDPOINT = "/Task/";

    @Override
    public void configure() {
        // spotless:off
        from("direct:openmrs-delete-task-route")
                .log(LoggingLevel.INFO, "Deleting Task in OpenMRS...")
                .routeId("openmrs-delete-task-route")
                .setHeader(Constants.CAMEL_HTTP_METHOD, constant(Constants.DELETE))
                .setHeader(Constants.CONTENT_TYPE, constant(Constants.APPLICATION_JSON))
                .setHeader(Constants.AUTHORIZATION, constant(openmrsFhirClient.authHeader()))
                .toD(openmrsFhirClient.getOpenmrsFhirBaseUrl() + DELETE_ENDPOINT + "${header."
                        + Constants.HEADER_TASK_ID + "}")
                .end();
        // spotless:on
    }
}
