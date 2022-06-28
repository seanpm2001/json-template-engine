// Copyright 2019,2022 VMware, Inc.
// SPDX-License-Indentifier: Apache-2.0

package com.vmware.jsonteng;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.fasterxml.jackson.databind.ObjectMapper;

public class DefaultJsonLoader implements JsonLoader {
    private final Stack<DirData> dirStack;
    private final ObjectMapper objectMapper;
    private final boolean verbose;

    public DefaultJsonLoader(String rootPath, boolean verbose) {
        this.dirStack = new Stack<>();
        this.objectMapper = new ObjectMapper();
        if (rootPath == null) {
            rootPath = "";
        }
        dirStack.push(new DirData("root", rootPath));
        this.verbose = verbose;
    }

    @Override
    public Object load(String jsonResource) throws TemplateEngineException {
        final DirData parent = dirStack.peek();
        String effectiveURL = parent.effectiveURL + jsonResource;
        if (!effectiveURL.contains("://")) {
            effectiveURL = "file://" + effectiveURL;
        }
        Object jsonObject;
        try {
            final URL url = new URL(effectiveURL);
            jsonObject = objectMapper.readValue(url, Map.class);
            int lastDirIndex = effectiveURL.lastIndexOf('/');
            dirStack.push(new DirData(jsonResource, effectiveURL.substring(0, lastDirIndex + 1)));
        } catch (IOException e) {
            if (verbose) {
                System.err.printf("Treat %s as JSON value.%n", effectiveURL);
            }
            try {
                jsonObject = objectMapper.readValue(jsonResource, Map.class);
            } catch (IOException e1) {
                // Not a Map, try List.
                try {
                    jsonObject = objectMapper.readValue(jsonResource, List.class);
                } catch (IOException e2) {
                    // Not a List, try number.
                    try {
                        jsonObject = Integer.valueOf(jsonResource);
                    } catch (NumberFormatException e3) {
                        try {
                            jsonObject = Double.valueOf(jsonResource);
                        } catch (NumberFormatException e4) {
                            jsonObject = jsonResource;
                        }
                    }
                }
            }
            dirStack.push(new DirData(jsonResource, null));
        }
        return jsonObject;
    }

    @Override
    public void unload(String jsonResource) throws TemplateEngineException {
        final DirData dirData = dirStack.pop();
        if (dirData.jsonResource != jsonResource) {
            throw new TemplateEngineException("JSON resource loading is out of order.");
        }
    }

    private static class DirData {
        private final Object jsonResource;
        private final String effectiveURL;

        DirData(Object jsonResource, String effectiveURL) {
            this.jsonResource = jsonResource;
            this.effectiveURL = effectiveURL;
        }
    }
}
