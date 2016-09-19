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

public class Keyword implements Comparable<Keyword> {

    final String ns;
    final String name;
    transient String _str;
    transient int _hash;

    public Keyword(String nsname) {
        int i = nsname.indexOf('/');
        if(i == -1) {
            ns = null;
            name = nsname.intern();
        } else {
            ns = nsname.substring(0, i).intern();
            name = nsname.substring(i + 1).intern();
        }
    }

    @Override
    public String toString() {
        if(_str == null){
            if(ns != null)
                _str = ":" + ns + "/" + name;
            else
                _str = ":" + name;
        }
        return _str;
    }

    public String getName() {
        return name;
    }

    public String getNamespace() {
        return ns;
    }

    @Override
    public boolean equals(Object o) {

        if(o == this)
            return true;

        //N.B. relies on interned strings
        return o instanceof Keyword &&
                ((Keyword)o).getNamespace() == ns &&
                ((Keyword)o).getName() == name;

    }

    @Override
    public int hashCode() {
        if(_hash == 0)
            _hash = 17 * toString().hashCode();
        return _hash;
    }

    @Override
    public int compareTo(Keyword keyword) { return toString().compareTo(keyword.toString()); }
}
