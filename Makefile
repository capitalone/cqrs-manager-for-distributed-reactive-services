# Copyright 2017 Capital One Services, LLC

# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#     http://www.apache.org/licenses/LICENSE-2.0

# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and limitations under the License.

default: build

.PHONY: clean
clean: destroy
	cd example && mvn clean

example/target/commander-kafka-streams-example-*.jar:
	cd example && mvn clean package

.PHONY: build
build: example/target/commander-kafka-streams-example-*.jar
	docker-compose --project-name=commander -f docker-compose.yml -f docker-compose-example.yml build

.PHONY: network
network:
	-docker network create commander

.PHONY: example
example: service-bootstrap
	docker-compose --project-name=commander -f docker-compose-example.yml up -d

.PHONY: services
services: network
	docker-compose --project-name=commander up -d

.PHONY: service-bootstrap
service-bootstrap: build services
	docker run --network commander --rm -it commander_rest com.capitalone.commander.database 'jdbc:postgresql://postgres/postgres?user=postgres&password=postgres' commander commander commander

.PHONY: stop
stop:
	docker-compose --project-name=commander -f docker-compose.yml -f docker-compose-example.yml stop

.PHONY: destroy
destroy:
	docker-compose --project-name=commander -f docker-compose.yml -f docker-compose-example.yml down -v --rmi local && docker network prune -f

.PHONY: example-logs
example-logs:
	docker-compose --project-name=commander -f docker-compose.yml -f docker-compose-example.yml logs -t -f

.PHONY: service-logs
service-logs:
	docker-compose --project-name=commander logs -t -f

.PHONY: psql
psql:
	docker run --network commander --rm -it --entrypoint psql postgres:9.5.3 -h postgres -U commander -d commander
