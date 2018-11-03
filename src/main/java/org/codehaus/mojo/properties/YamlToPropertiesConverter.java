/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Reference : https://github.com/ozimov/yaml-properties-maven-plugin/blob/master/src/main/java/org/codehaus/mojo/properties/YamlToPropertiesConverter.java

package org.codehaus.mojo.properties;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Converts a yaml file into a properties.
 */
public class YamlToPropertiesConverter {
    /**
     * Extract a flat representation of a Yaml file into a map of key-value pairs.
     *
     * @param inputStream the stream holding the yaml data
     * @return the map with key-value pairs.
     */
    public static Properties convertToProperties(final InputStream inputStream) {
        final Properties properties = new Properties();

        final Yaml yaml = new Yaml();
        final Object object = yaml.load(new UnicodeReader(inputStream));
        if (object != null && object instanceof Map) {
            final Map map = (Map<String, Object>) object;
            final Map<String, String> flatMap = flattenMap(map);
            properties.putAll(flatMap);
        }

        return properties;
    }

    private static Map<String, String> flattenMap(final Map mapOfObjects) {
        final Map<String, Object> mapOfMaps = toHierarchicalMap(mapOfObjects);
        final Map<String, Object> flattenedMap = toFlatMap(mapOfMaps);

        final Map<String, String> propertiesMap = new LinkedHashMap<String, String>();
        for (final Map.Entry<String, Object> entry : flattenedMap.entrySet()) {
            propertiesMap.put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        return propertiesMap;
    }

    private static Map<String, Object> toHierarchicalMap(final Object content) {
        final Map<String, Object> dataMap = new LinkedHashMap<String, Object>();

        for (final Map.Entry<String, Object> entry : ((Map<String, Object>) content).entrySet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            toHierarchicalValue(dataMap, key, value);
        }

        return dataMap;
    }

    private static void toHierarchicalValue(Map<String, Object> dataMap, String key, Object value) {
        if (value instanceof Map) {
            dataMap.put(key, toHierarchicalMap(value));
        } else if (value instanceof Collection) {
            for (final Object element : ((Collection) value)) {
                toHierarchicalValue(dataMap,key,element);
            }
        } else {
            dataMap.put(key, value == null ? "" : value);
        }
    }

    private static Map<String, Object> toFlatMap(final Map<String, Object> source) {
        final Map<String, Object> flattenedMap = new LinkedHashMap<String, Object>();

        for (final String key : source.keySet()) {
            final Object value = source.get(key);

            if (value instanceof Map) {
                final Map<String, Object> nestedMap = toFlatMap((Map<String, Object>) value);

                for (final String nestedKey : nestedMap.keySet()) {
                    flattenedMap.put(String.format("%s.%s", key, nestedKey), nestedMap.get(nestedKey));
                }
            } else if (value instanceof Collection) {
                final StringBuilder stringBuilder = new StringBuilder();

                boolean firstElement = true;
                for (final Object element : ((Collection) value)) {
                    final Map<String, Object> subMap = toFlatMap(Collections.singletonMap(key, element));
                    if (firstElement) {
                        stringBuilder.append(",");
                    }

                    stringBuilder.append(subMap.entrySet().iterator().next().getValue().toString());
                    firstElement = false;
                }

                flattenedMap.put(key, stringBuilder.toString());
            } else {
                flattenedMap.put(key, value == null ? "" : value);
            }
        }

        return flattenedMap;
    }

}
