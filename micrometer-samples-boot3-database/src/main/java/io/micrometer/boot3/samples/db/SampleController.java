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

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SampleController {

    private static final Logger log = LoggerFactory.getLogger(SampleController.class);

    private final ObservationRegistry registry;

    private final JdbcTemplate jdbcTemplate;

    SampleController(ObservationRegistry registry, JdbcTemplate jdbcTemplate) {
        this.registry = registry;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/")
    List<String> allPeople() {
        return Observation.createNotStarted("allPeople", registry)
                .observe(slowDown(() -> jdbcTemplate.queryForList("SELECT * FROM emp").stream()
                        .map(map -> map.get("name").toString())
                        .toList()));
    }

    @GetMapping("/greet/{name}")
    String greet(@PathVariable String name) {
        Observation observation = Observation.createNotStarted("greeting", registry).start();
        try (Observation.Scope scope = observation.openScope()) {
            String foundName = jdbcTemplate.queryForObject("SELECT name FROM emp where name=?", String.class, name);
            if (foundName != null) {
                // only 2 names are valid (low cardinality)
                observation.lowCardinalityKeyValue("greeting.name", name);
                observation.event(Observation.Event.of("greeted"));
                return fetchDataSlowly(() -> String.format("Hello %s!", name));
            }
            else {
                observation.lowCardinalityKeyValue("greeting.name", "N/A");
                observation.highCardinalityKeyValue("greeting.name", name);
                observation.event(Observation.Event.of("failed"));
                throw new IllegalArgumentException("Invalid name!");
            }
        }
        catch (Exception exception) {
            observation.error(exception);
            throw exception;
        }
        finally {
            observation.stop();
        }
    }

    private <T> Supplier<T> slowDown(Supplier<T> supplier) {
        return () -> {
            try {
                if (Math.random() < 0.02) { // huge latency, less frequent
                    Thread.sleep(1_000);
                }
                Thread.sleep(((int) (Math.random() * 100)) + 100); // +base latency
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Fetching the data");
            return supplier.get();
        };
    }

    private <T> T fetchDataSlowly(Supplier<T> supplier) {
        return slowDown(supplier).get();
    }

}