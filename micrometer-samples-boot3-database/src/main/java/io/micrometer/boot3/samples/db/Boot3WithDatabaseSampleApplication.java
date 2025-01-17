/*
 * Copyright 2022 VMware, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micrometer.boot3.samples.db;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Boot3WithDatabaseSampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(Boot3WithDatabaseSampleApplication.class, args);
    }

    @Bean
    ObservationHandler<Observation.Context> errorHandler() {
        return new ObservationHandler<>() {
            private static final Logger LOGGER = LoggerFactory.getLogger("errorHandler");

            @Override
            public void onError(Observation.Context context) {
                LOGGER.error("Ooops!", context.getError());
            }

            @Override
            public boolean supportsContext(Observation.Context context) {
                return true;
            }
        };
    }

}
