/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.xbean.blueprint.generator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dain Sundstrom
 * @version $Id$
 * @since 1.0
 */
public class DocumentationGenerator implements GeneratorPlugin {
    private final File destFile;
    private LogFacade log;

    public DocumentationGenerator(File destFile) {
        this.destFile = destFile;
    }

    public void generate(NamespaceMapping namespaceMapping) throws IOException {
        String namespace = namespaceMapping.getNamespace();

        // TODO can only handle 1 schema document so far...
        File file = new File(destFile.getParentFile(), destFile.getName() + ".html");
        file.getParentFile().mkdirs();
        log.log("Generating HTML documentation file: " + file + " for namespace: " + namespace);
        PrintWriter out = new PrintWriter(new FileWriter(file));
        try {
            generateDocumentation(out, namespaceMapping);
        } finally {
            out.close();
        }
    }

    private void generateDocumentation(PrintWriter out, NamespaceMapping namespaceMapping) {
        String namespace = namespaceMapping.getNamespace();

        out.println("<!-- NOTE: this file is autogenerated by Apache XBean -->");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Schema for namespace: " + namespace + "</title>");
        out.println("<link rel='stylesheet' href='style.css' type='text/css'>");
        out.println("<link rel='stylesheet' href='http://activemq.org/style.css' type='text/css'>");
        out.println("<link rel='stylesheet' href='http://activemq.org/style-xb.css' type='text/css'>");
        out.println("</head>");
        out.println();
        out.println("<body>");
        out.println();

        generateRootElement(out, namespaceMapping);

        generateElementsSummary(out, namespaceMapping);
        out.println();
        out.println();

        generateElementsDetail(out, namespaceMapping);

        out.println();
        out.println("</body>");
        out.println("</html>");
    }

    private void generateRootElement(PrintWriter out, NamespaceMapping namespaceMapping) {
        ElementMapping rootElement = namespaceMapping.getRootElement();
        if (rootElement != null) {
            out.println("<h1>Root Element</h1>");
            out.println("<table>");
            out.println("  <tr><th>Element</th><th>Description</th><th>Class</th>");
            generateElementSummary(out, rootElement);
            out.println("</table>");
            out.println();
        }
    }

    private void generateElementsSummary(PrintWriter out, NamespaceMapping namespaceMapping) {
        out.println("<h1>Element Summary</h1>");
        out.println("<table>");
        out.println("  <tr><th>Element</th><th>Description</th><th>Class</th>");
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            generateElementSummary(out, element);
        }
        out.println("</table>");
    }

    private void generateElementSummary(PrintWriter out, ElementMapping element) {
        out.println("  <tr>" +
                "<td><a href='#" + element.getElementName() + "'>" + element.getElementName() + "</a></td>" +
                "<td>" + element.getDescription() + "</td>" +
                "<td>" + element.getClassName() + "</td></tr>");
    }

    private void generateElementsDetail(PrintWriter out, NamespaceMapping namespaceMapping) {
        out.println("<h1>Element Detail</h1>");
        for (Iterator iter = namespaceMapping.getElements().iterator(); iter.hasNext();) {
            ElementMapping element = (ElementMapping) iter.next();
            generateHtmlElementDetail(out, namespaceMapping, element);
        }
    }

    private void generateHtmlElementDetail(PrintWriter out, NamespaceMapping namespaceMapping, ElementMapping element) {
        out.println("<h2>Element: <a name='" + element.getElementName() + "'>" + element.getElementName() + "</a></h2>");

        boolean hasAttributes = false;
        boolean hasElements = false;
        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext() && (!hasAttributes || !hasElements);) {
            AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
            Type type = attributeMapping.getType();
            if (namespaceMapping.isSimpleType(type)) {
                hasAttributes = true;
            } else {
                hasElements = true;
            }
        }

        if (hasAttributes) {
            out.println("<table>");
            out.println("  <tr><th>Attribute</th><th>Type</th><th>Description</th>");
            for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
                AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
                Type type = attributeMapping.getPropertyEditor() != null ?  Type.newSimpleType(String.class.getName()) : attributeMapping.getType();
                if (namespaceMapping.isSimpleType(type)) {
                    out.println("  <tr><td>" + attributeMapping.getAttributeName() + "</td><td>" + Utils.getXsdType(type)
                            + "</td><td>" + attributeMapping.getDescription() + "</td></tr>");
                }

            }
            out.println("</table>");
        }

        if (hasElements) {
            out.println("<table>");
            out.println("  <tr><th>Element</th><th>Type</th><th>Description</th>");
            for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
                AttributeMapping attributeMapping = (AttributeMapping) iterator.next();
                Type type = attributeMapping.getType();
                if (!namespaceMapping.isSimpleType(type)) {
                    out.print("  <tr><td>" + attributeMapping.getAttributeName() + "</td><td>");
                    printComplexPropertyTypeDocumentation(out, namespaceMapping, type);
                    out.println("</td><td>" + attributeMapping.getDescription() + "</td></tr>");
                }
            }
            out.println("</table>");
        }
    }

    private void printComplexPropertyTypeDocumentation(PrintWriter out, NamespaceMapping namespaceMapping, Type type) {
        if (type.isCollection()) {
            out.print("(");
        }

        List types;
        if (type.isCollection()) {
            types = Utils.findImplementationsOf(namespaceMapping, type.getNestedType());
        } else {
            types = Utils.findImplementationsOf(namespaceMapping, type);
        }

        for (Iterator iterator = types.iterator(); iterator.hasNext();) {
            ElementMapping element = (ElementMapping) iterator.next();
            out.print("<a href='#" + element.getElementName() + "'>" + element.getElementName() + "</a>");
            if (iterator.hasNext()) {
                out.print(" | ");
            }
        }
        if (types.size() == 0) {
            out.print("&lt;spring:bean/&gt;");
        }

        if (type.isCollection()) {
            out.print(")*");
        }
    }

    public LogFacade getLog() {
        return log;
    }

    public void setLog(LogFacade log) {
        this.log = log;
    }
}
