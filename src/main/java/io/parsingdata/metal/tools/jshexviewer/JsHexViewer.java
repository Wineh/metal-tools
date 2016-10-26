/*
 * Copyright 2013-2016 Netherlands Forensic Institute
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.parsingdata.metal.tools.jshexviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;

import io.parsingdata.metal.data.ParseGraph;
import io.parsingdata.metal.data.ParseItem;
import io.parsingdata.metal.data.ParseValue;
import io.parsingdata.metal.token.Def;

/**
 * Generate a HTML page to view the Metal ParseGraph in a hex viewer.
 */
public class JsHexViewer {

    private static final int COLUMN_COUNT = 1 << 5;

    public static void generate(final ParseGraph graph) throws URISyntaxException, IOException {
        generate(graph, "jsHexViewer");
    }

    public static String generateJs(final ParseGraph graph) throws IOException {
        final Map<Long, LinkedList<Definition>> map = new TreeMap<>();
        step(graph, map);

        return "/* generated by JsHexViewer */" +
            "var columnCountUpdate = " + COLUMN_COUNT + ";" +
            "var locationsUpdate = " + map.keySet().toString() + ";" +
            "var dataUpdate = " + writeData(map) + ";";
    }

    public static void generate(final ParseGraph graph, final String fileName) throws URISyntaxException, IOException {
        final File root = new File(JsHexViewer.class.getResource("/").toURI());
        generate(graph, fileName, root, true);
    }

    public static void generate(final ParseGraph graph, final String fileName, final File dir, final boolean copyLibs) throws URISyntaxException, IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(dir, fileName + ".js"))) {
            IOUtils.write(generateJs(graph), fos, StandardCharsets.UTF_8);
        }

        final File file = new File(dir, fileName + ".htm");
        try (FileWriter out = new FileWriter(file);
             InputStream in = JsHexViewer.class.getResourceAsStream("/jsHexViewer/template.htm");
             BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("<!-- generated -->")) {
                    if (line.trim().startsWith("var dataUrl =")) {
                        out.write("var dataUrl = '");
                        out.write(fileName + ".js");
                        out.write("';");
                    }
                }
                else {
                    out.write(line);
                }
                out.write('\n');
            }
        }

        if (copyLibs) {
            copyLibs(dir);
        }
    }

    public static void copyLibs(final File dir) throws URISyntaxException, IOException {
        final File libsSource = new File(JsHexViewer.class.getResource("/jsHexViewer/libs/").toURI());
        final File libsDestination = new File(dir, "libs");
        libsDestination.mkdir();
        final Path source = libsSource.toPath();
        final Path destination = libsDestination.toPath();

        Files.walkFileTree(libsSource.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                final Path targetPath = destination.resolve(source.relativize(dir));
                if (!Files.exists(targetPath)) {
                    Files.createDirectory(targetPath);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String writeData(final Map<Long, LinkedList<Definition>> map) throws IOException {
        final StringBuilder builder = new StringBuilder("[");
        for (final Long row : map.keySet()) {
            if (builder.length() > 1) {
                builder.append(",");
            }
            builder.append(map.get(row).toString());
        }
        return builder.append("]").toString();
    }

    private static void step(final ParseItem item, final Map<Long, LinkedList<Definition>> map) {
        if (!item.isGraph()) {
            if (item.getDefinition() instanceof Def) {
                final ParseValue value = item.asValue();
                getList(map, new Definition(value));
            }
            return;
        }
        if (item.asGraph().head == null) {
            return;
        }
        step(item.asGraph().head, map);
        step(item.asGraph().tail, map);
    }

    private static void getList(final Map<Long, LinkedList<Definition>> map, final Definition definition) {
        final long row = definition._offset / COLUMN_COUNT;
        LinkedList<Definition> list = map.get(row);
        if (list == null) {
            list = new LinkedList<>();
            map.put(row, list);
        }
        list.addFirst(definition);
    }

    private static class Definition {
        private final String _name;
        private final long _offset;
        private final long _size;

        public Definition(final ParseValue value) {
            _name = value.name;
            _offset = value.getOffset();
            _size = value.getValue().length;
        }

        @Override
        public String toString() {
            return "[" + _offset + ", " + _size + ", '" + _name + "']";
        }
    }
}
