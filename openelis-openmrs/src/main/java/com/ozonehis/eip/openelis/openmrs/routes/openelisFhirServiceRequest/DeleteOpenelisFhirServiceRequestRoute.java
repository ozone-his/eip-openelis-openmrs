/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes.openelisFhirServiceRequest;

import com.ozonehis.eip.openelis.openmrs.Constants;
import lombok.AllArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DeleteOpenelisFhirServiceRequestRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:openelis-delete-service-request-route")
                .log(LoggingLevel.INFO, "Deleting ServiceRequest in OpenELIS...")
                .routeId("openelis-delete-service-request-route")
                .toD("openelisfhir://delete/resourceById?type=ServiceRequest&stringId=" + "${header."
                        + Constants.HEADER_SERVICE_REQUEST_ID + "}")
                .marshal()
                .fhirJson("R4")
                .convertBodyTo(String.class)
                .end();
        // spotless:on
    }
}
