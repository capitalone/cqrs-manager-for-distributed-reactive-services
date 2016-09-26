/*
 * Copyright 2016 Capital One Services, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.capitalone.commander.example.kafka_streams;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class ExampleApplication extends Application<ExampleConfiguration> {
    public static void main(String[] args) throws Exception {
        new ExampleApplication().run(args);
    }

    @Override
    public String getName() {
        return "kafka-streams-example";
    }

    @Override
    public void initialize(Bootstrap<ExampleConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)
                )
        );
    }

    @Override
    public void run(ExampleConfiguration exampleConfiguration, Environment environment) throws Exception {
        final CustomerStore customerStore = new CustomerStore();
        final CommandProcessor commandProcessor = new CommandProcessor(
                customerStore,
                exampleConfiguration.getStreamsConfigFactory().build(),
                exampleConfiguration.getCommandsTopic(),
                exampleConfiguration.getEventsTopic(),
                exampleConfiguration.getCustomersTopic()
        );
        environment.lifecycle().manage(commandProcessor);
        environment.jersey().register(new CustomerResource(customerStore));
    }
}
