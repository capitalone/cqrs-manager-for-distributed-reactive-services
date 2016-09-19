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

import org.fressian.*;
import org.fressian.handlers.ILookup;
import org.fressian.handlers.ReadHandler;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.fressian.handlers.WriteHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FressianSerde implements Serde {
    public FressianSerde() {}

    @Override
    public void configure(Map map, boolean b) {}

    @Override
    public void close() {}

    @Override
    public Serializer serializer() {
        return new FressianSerializer();
    }

    @Override
    public Deserializer deserializer() {
        return new FressianDeserializer();
    }

    private class FressianSerializer implements Serializer<Object> {

        @Override
        public void configure(Map<String, ?> map, boolean b) {}

        @Override
        public byte[] serialize(String s, Object o) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Writer writer = new FressianWriter(outputStream, new KeywordWriteHandler());
            try {
                writer.writeObject(o);
                writer.writeFooter();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return outputStream.toByteArray();
        }

        @Override
        public void close() {}
    }

    private class FressianDeserializer implements Deserializer<Object> {
        @Override
        public void configure(Map<String, ?> map, boolean b) {}

        @Override
        public Object deserialize(String s, byte[] bytes) {
            Reader reader = new FressianReader(new ByteArrayInputStream(bytes), new KeywordReadHandler(), false);
            Object ret = null;
            try {
                ret = reader.readObject();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return ret;
        }

        @Override
        public void close() {}
    }

    private class KeywordReadHandler implements ReadHandler, ILookup<Object,ReadHandler> {

        @Override
        public Object read(Reader reader, Object tag, int componentCount) throws IOException {
            StringBuffer stringBuffer = new StringBuffer();
            String ns = (String) reader.readObject();
            Object name = reader.readObject();
            if (ns != null && !ns.isEmpty()) {
                stringBuffer.append(ns);
                stringBuffer.append('/');
            }
            stringBuffer.append(name);
            return new Keyword(stringBuffer.toString());
        }

        @Override
        public ReadHandler valAt(Object s) {
            if (s.equals("key")) {
                return this;
            } else {
                return null;
            }
        }
    }

    private class KeywordWriteHandler implements WriteHandler, ILookup<Class, Map<String, WriteHandler>> {

        @Override
        public void write(Writer writer, Object o) throws IOException {
            Keyword keyword = (Keyword) o;
            writer.writeTag("key", 2);
            writer.writeObject(keyword.getNamespace(), true);
            writer.writeObject(keyword.getName(), true);
        }

        @Override
        public Map<String, WriteHandler> valAt(Class aClass) {
            if (aClass.equals(Keyword.class)) {
                Map handlerMap = new HashMap();
                handlerMap.put("key", this);
                return handlerMap;
            } else {
                return null;
            }
        }
    }
}
