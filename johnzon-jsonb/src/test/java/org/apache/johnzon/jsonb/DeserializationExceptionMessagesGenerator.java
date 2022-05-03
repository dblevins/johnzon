/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.johnzon.jsonb;

import org.apache.cxf.common.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.johnzon.jsonb.DeserializationExceptionMessagesTest.getExceptionMessage;

//CHECKSTYLE:OFF
public class DeserializationExceptionMessagesGenerator {

    public static void main(String[] args) throws IOException {
        new DeserializationExceptionMessagesGenerator().testMethods();
    }

    private void testMethods() throws IOException {
        final List<Item> fields = Stream.of(Item.values())
                .filter(Item::isField)
                .collect(Collectors.toList());

        final File file = new File("/tmp/testmethods.txt");
        final PrintStream out = new PrintStream(new FileOutputStream(file));

        for (final Item field : fields) {
            final List<Item> input = Stream.of(Item.values())
                    .filter(item -> item.isInputFor(field))
                    .collect(Collectors.toList());

            for (final Item type : input) {
                final String json = String.format("{ \"%s\" : %s }", field.fieldName, type.getJson());
                final String message = getExceptionMessage(json);
                out.printf("%n@Test%npublic void %sFrom%s() throws Exception {%n", field.name, StringUtils.capitalize(type.name));
                out.printf("        assertMessage(\"{ \\\"%s\\\" : %s }\",%n", field.fieldName, type.getEscapedJson());
                out.printf("                      %s);%n", formatException(message));
                out.println("}");
            }
        }

        out.close();

        try (final FileInputStream in = new FileInputStream(file)) {
            final byte[] b = new byte[1024 * 1000];
            final int read = in.read(b);
            System.out.write(b, 0, read);
        }
    }

    private String formatException(String message) {
        final StringBuilder sb = new StringBuilder();
        final int max = 100;
        while (message.length() > max) {
            sb.append("\"");
            sb.append(escape(message.substring(0, max)));
            sb.append("\" + \n");
            message = message.substring(max);
        }
        sb.append("\"").append(escape(message)).append("\"");
        return sb.toString();
    }

    public static String escape(final String string) {
        return string.replace("\"", "\\\"").replace("\n", "\\n");
    }


    public enum Item {
        object("object", "Color", "{\"red\": 255, \"green\": 165, \"blue\":0}"),
        string("string", "String", "Supercalifragilisticexpialidocious"),
        number("number", "Integer", "122333444455555.666666777777788888888"),
        intPrimitive("intPrimitive", "int", null),
        bool("bool", "Boolean", "true"),
        boolPrimitive("boolPrimitive", "boolean", null),
        enumeration("unit", "TimeUnit", null),
        date("date", "Date", null),
        nul("null", "null", "null"),
        arrayOfObject("arrayOfObject", "Object[]", "[{\"red\": 255, \"green\": 165, \"blue\":0},{\"red\": 0, \"green\": 45, \"blue\":127}]"),
        arrayOfString("arrayOfString", "String[]", "[\"Klaatu\", \"barada\", \"nikto\"]"),
        arrayOfNumber("arrayOfNumber", "Number[]", "[2, 3, 5, 7, 11, 13, 17, 19, 23, 29]"),
        arrayOfBoolean("arrayOfBoolean", "Boolean[]", "[true,false,true,true,false]"),
        arrayOfInt("arrayOfInt", "Number[]", null),
        arrayOfByte("arrayOfByte", "Number[]", null),
        arrayOfChar("arrayOfChar", "Number[]", null),
        arrayOfShort("arrayOfShort", "Number[]", null),
        arrayOfLong("arrayOfLong", "Number[]", null),
        arrayOfFloat("arrayOfFloat", "Number[]", null),
        arrayOfDouble("arrayOfDouble", "Number[]", null),
        arrayOfBooleanPrimitive("arrayOfBooleanPrimitive", "Boolean[]", null),
        arrayOfNull("arrayOfNull", "null", "[null,null,null,null,null,null]"),
        listOfObject("listOfObject", "Object[]", null),
        listOfString("listOfString", "String[]", null),
        listOfNumber("listOfNumber", "Number[]", null),
        listOfBoolean("listOfBoolean", "Boolean[]", null),
        ;

        private final String name;
        private final String fieldName;
        private final String javaType;
        private final String json;
        private final Field field;

        Item(final String fieldName, final String javaType, final String json) {
            this.name = getName(fieldName);
            this.fieldName = fieldName;
            this.javaType = javaType;
            this.json = json;

            this.field = getField(fieldName);
        }

        private String getName(final String fieldName) {
            if ("bool".equals(fieldName)) return "boolean";
            if ("unit".equals(fieldName)) return "enum";
            return fieldName;
        }

        private Field getField(final String fieldName) {
            try {
                return DeserializationExceptionMessagesTest.Widget.class.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                return null;
            }
        }

        public String getName() {
            return name;
        }

        public String getField() {
            return fieldName;
        }

        public String getJson() {
            if (field != null && field.getType().equals(String.class)) {
                return "\"" + json + "\"";
            }
            return json;
        }

        public boolean isField() {
            return field != null;
        }

        public String getEscapedJson() {
            if ("string".equalsIgnoreCase(javaType)) {
                return "\\\"" + escape(json) + "\\\"";
            }
            return escape(json);
        }

        public boolean isInputFor(final Item field) {
            if (this == field) return false;
            if (this.json == null) return false;
            if (this == nul) return field.field.getType().isPrimitive();
            if (this == boolPrimitive && field == bool) return false;
            if (this == bool && field == boolPrimitive) return false;
            if (this == arrayOfNull) return field.isArrayOfPrimitive();
            if (this == arrayOfString && field == arrayOfChar) return false;
            if (this == arrayOfNumber) {
                switch (field){
                    case arrayOfInt:
                    case arrayOfChar:
                    case arrayOfByte:
                    case arrayOfShort:
                    case arrayOfLong:
                    case arrayOfFloat:
                    case arrayOfDouble:
                        return false;
                }
            }
            if (field == arrayOfInt && this == arrayOfNumber) return false;
            if (field == arrayOfBooleanPrimitive && this == arrayOfBoolean) return false;
            if (field == listOfBoolean && this == arrayOfBoolean) return false;
            if (field == listOfObject && this == arrayOfObject) return false;
            if (field == listOfString && this == arrayOfString) return false;
            if (field == listOfNumber && this == arrayOfNumber) return false;
            if (field == string) {
                switch (this) {
                    case object:
                    case number:
                    case bool:
                    case boolPrimitive:
                    case nul:
                        return false;
                }
            }

            return true;
        }

        public boolean isArrayOfPrimitive() {
            switch (this) {
                case arrayOfInt:
                case arrayOfBooleanPrimitive:
                    return true;
                default:
                    return false;
            }
        }
    }
}
//CHECKSTYLE:ON
