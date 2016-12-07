-- Copyright 2016 Capital One Services, LLC

-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at

--     http://www.apache.org/licenses/LICENSE-2.0

-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and limitations under the License.

ALTER TABLE IF EXISTS commander
  ADD COLUMN parent    uuid REFERENCES commander (id),
  ADD COLUMN command   boolean DEFAULT false,
  ADD COLUMN action    varchar(255) NOT NULL,
  ADD COLUMN data      bytea NOT NULL,
  ADD COLUMN timestamp bigint CHECK ("timestamp" >= 0),
  ADD COLUMN topic     varchar(255) NOT NULL,
  ADD COLUMN partition smallint CHECK (partition >= 0),
  ADD COLUMN "offset"  bigint CHECK ("offset" >= 0);
  -- ADD COLUMN partition varchar(255) NOT NULL,
  -- ADD COLUMN "offset"  varchar(255) NOT NULL;
