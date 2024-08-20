/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import static org.apache.camel.builder.AdviceWith.adviceWith;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.camel.Endpoint;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

@Disabled
@UseAdviceWith
class CreateOpenmrsFhirResourceRouteTest extends CamelSpringTestSupport {
    private static final String DIAGNOSTIC_REPORT_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

    private static final String CREATE_RESOURCE_ROUTE = "direct:openmrs-create-resource-route";

    @Override
    protected RoutesBuilder createRouteBuilder() {
        return new CreateOpenmrsFhirResourceRoute();
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new StaticApplicationContext();
    }

    @BeforeEach
    public void setup() throws Exception {
        adviceWith("openmrs-create-resource-route", context, new AdviceWithRouteBuilder() {

            @Override
            public void configure() {
                weaveByToUri("fhir:*").replace().to("mock:create-openmrs-resource");
            }
        });

        Endpoint defaultEndpoint = context.getEndpoint(CREATE_RESOURCE_ROUTE);
        template.setDefaultEndpoint(defaultEndpoint);
    }

    @Test
    public void shouldCreateResource() throws Exception {
        // Setup
        DiagnosticReport diagnosticReport = new DiagnosticReport();
        diagnosticReport.setId(DIAGNOSTIC_REPORT_ID);

        // Expectations
        MockEndpoint mockCreateResourceEndpoint = getMockEndpoint("mock:create-openmrs-resource");
        mockCreateResourceEndpoint.expectedMessageCount(1);
        mockCreateResourceEndpoint.setResultWaitTime(100);

        // Act
        template.send(CREATE_RESOURCE_ROUTE, exchange -> {
            exchange.getMessage().setBody(diagnosticReport);
        });

        // Verify
        mockCreateResourceEndpoint.assertIsSatisfied();
    }
}
