/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.openelis.openmrs.routes;

import com.ozonehis.eip.openelis.openmrs.converter.FhirResourceConverter;
import com.ozonehis.eip.openelis.openmrs.processors.TaskProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class TaskRouting extends RouteBuilder {

    @Autowired
    private TaskProcessor taskProcessor;

    @Autowired
    private FhirResourceConverter fhirResourceConverter;

    @Override
    public void configure() {
        getContext().getTypeConverterRegistry().addTypeConverters(fhirResourceConverter);
        // spotless:off
        from("scheduler:taskUpdate?initialDelay=10000&delay=10000")
            .routeId("poll-senaite")
            .log(LoggingLevel.INFO, "Polling Tasks started...")
            .to("direct:openmrs-get-task-by-status-route")
            .process(taskProcessor)
            .log(LoggingLevel.INFO, "Polling Tasks completed.")
                .end();
        // spotless:on
    }
}
