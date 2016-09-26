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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.streams.StreamsConfig;

public class StreamsConfigFactory {
    private String applicationId;
    private String bootstrapServers;
    private String zookeeperConnect;

    @JsonProperty
    public String getApplicationId() {
        return applicationId;
    }

    @JsonProperty
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    @JsonProperty
    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    @JsonProperty
    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    @JsonProperty
    public String getBootstrapServers() {
        return bootstrapServers;
    }

    @JsonProperty
    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public StreamsConfig build() {
        return new StreamsConfig(ImmutableMap.of(
                StreamsConfig.APPLICATION_ID_CONFIG, getApplicationId(),
                StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers(),
                StreamsConfig.ZOOKEEPER_CONNECT_CONFIG, getZookeeperConnect()
        ));
    }
}
