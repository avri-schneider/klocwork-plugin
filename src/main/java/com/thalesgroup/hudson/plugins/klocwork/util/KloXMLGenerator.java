/*******************************************************************************
 * Copyright (c) 2012 Emenda Software Ltd.                                      *
 * Author : Andreas Larfors                                                     *
 *		                                                                        *
 * Permission is hereby granted, free of charge, to any person obtaining a copy *
 * of this software and associated documentation files (the "Software"), to deal*
 * in the Software without restriction, including without limitation the rights *
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell    *
 * copies of the Software, and to permit persons to whom the Software is        *
 * furnished to do so, subject to the following conditions:                     *
 *                                                                              *
 * The above copyright notice and this permission notice shall be included in   *
 * all copies or substantial portions of the Software.                          *
 *                                                                              *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR   *
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,     *
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE  *
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER       *
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,*
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN    *
 * THE SOFTWARE.                                                                *
 *                                                                              *
 *******************************************************************************/
package com.thalesgroup.hudson.plugins.klocwork.util;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import org.emenda.kwjlib.KWJSONRecord;
import org.emenda.kwjlib.KWWebAPIService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

/**
 * KloXMLGenerator uses the kwjlib library to connect to the Klocwork server
 * and retrieve a list of issues using the Web API. This list of issues is then
 * converted to XML format in order to replicate the depreceated kwinspectreport
 * functionality as closely as possible. Only used for systems running Klocwork
 * v9.6 or later.
 */
public class KloXMLGenerator {

    public static int GenerateXMLFromIssues(VirtualChannel channel, String a_host, String a_port,
                                            boolean useSSL,
                                            String a_projectname, String a_filename, BuildListener listener, String a_query) {
        KWWebAPIService KWservice = new KWWebAPIService(a_host, a_port, useSSL);
        listener.getLogger().println("Connecting to Klocwork Web API service... host: " + a_host + " port: " + a_port + " SSL: " + (useSSL ? "true" : "false"));
        if (KWservice.connect()) {
            try {
                listener.getLogger().println("Connection successful, creating XML document");
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                // root (errorList) element
                Document doc = docBuilder.newDocument();
                Element rootElement = doc.createElement("errorList");
                rootElement.setAttribute("xmlns", "http://www.klocwork.com/inForce/report/1.0");
                //Write version number. This is used to determine what data to display in the issue summary page
                rootElement.setAttribute("version", "9.6 or later");
                doc.appendChild(rootElement);

                //Retrieve issues
                KWJSONRecord[] issues = null;
                //Get issues
                listener.getLogger().println("Retrieving Klocwork issues using kwjlib...");
                issues = KWservice.search(a_projectname, a_query);
                listener.getLogger().println("Request sent: " + KWservice.getLastRequest());
                if (issues != null) {
                    listener.getLogger().println("Number of issues returned: " + String.valueOf(issues.length - 1));
                    //Iterate through issues
                    for (KWJSONRecord issue : issues) {
                        if (issue != null) {
                            // problem element
                            Element eproblem = doc.createElement("problem");
                            rootElement.appendChild(eproblem);
                            // problem element elements
                            Element eID = doc.createElement("problemID");
                            Element efile = doc.createElement("file");
                            Element emethod = doc.createElement("method");
                            Element ecode = doc.createElement("code");
                            Element emessage = doc.createElement("message");
                            Element ecitingStatus = doc.createElement("citingStatus");
                            Element estate = doc.createElement("state");
                            Element eowner = doc.createElement("owner");
                            Element eseverity = doc.createElement("severity");
                            Element eseveritylevel = doc.createElement("severitylevel");
                            Element edisplayAs = doc.createElement("displayAs");
                            Element etaxonomies = doc.createElement("taxonomies");
                            Element etaxonomy = doc.createElement("taxonomy");
                            Element eurl = doc.createElement("url");

                            org.w3c.dom.Text tID = doc.createTextNode("problemID");
                            org.w3c.dom.Text tfile = doc.createTextNode("file");
                            org.w3c.dom.Text tmethod = doc.createTextNode("method");
                            org.w3c.dom.Text tcode = doc.createTextNode("code");
                            org.w3c.dom.Text tmessage = doc.createTextNode("message");
                            org.w3c.dom.Text tcitingStatus = doc.createTextNode("citingStatus");
                            org.w3c.dom.Text tstate = doc.createTextNode("state");
                            org.w3c.dom.Text towner = doc.createTextNode("owner");
                            org.w3c.dom.Text tseverity = doc.createTextNode("severity");
                            org.w3c.dom.Text tseveritylevel = doc.createTextNode("severitylevel");
                            org.w3c.dom.Text tdisplayAs = doc.createTextNode("displayAs");
                            org.w3c.dom.Text turl = doc.createTextNode("url");

                            //set values
                            tID.setNodeValue(issue.getValue("id"));
                            tfile.setNodeValue(issue.getValue("file"));
                            tmethod.setNodeValue(issue.getValue("method"));
                            tcode.setNodeValue(issue.getValue("code"));
                            tmessage.setNodeValue(issue.getValue("message"));
                            tcitingStatus.setNodeValue(issue.getValue("status"));
                            tstate.setNodeValue(issue.getValue("state"));
                            towner.setNodeValue(issue.getValue("owner"));
                            tseverity.setNodeValue(issue.getValue("severity"));
                            tseveritylevel.setNodeValue(issue.getValue("severityCode"));
                            tdisplayAs.setNodeValue(issue.getValue("severity"));
                            etaxonomies.appendChild(etaxonomy);
                            etaxonomy.setAttribute("name", issue.getValue("taxonomyName"));
                            etaxonomy.setAttribute("metaInf", "");
                            turl.setNodeValue(issue.getValue("url"));

                            //append all elements
                            eproblem.appendChild(eID);
                            eproblem.appendChild(efile);
                            eproblem.appendChild(emethod);
                            eproblem.appendChild(ecode);
                            eproblem.appendChild(emessage);
                            eproblem.appendChild(ecitingStatus);
                            eproblem.appendChild(estate);
                            eproblem.appendChild(eowner);
                            eproblem.appendChild(eseverity);
                            eproblem.appendChild(eseveritylevel);
                            eproblem.appendChild(edisplayAs);
                            eproblem.appendChild(etaxonomies);
                            eproblem.appendChild(eurl);

                            eID.appendChild(tID);
                            efile.appendChild(tfile);
                            emethod.appendChild(tmethod);
                            ecode.appendChild(tcode);
                            emessage.appendChild(tmessage);
                            ecitingStatus.appendChild(tcitingStatus);
                            estate.appendChild(tstate);
                            eowner.appendChild(towner);
                            eseverity.appendChild(tseverity);
                            eseveritylevel.appendChild(tseveritylevel);
                            edisplayAs.appendChild(tdisplayAs);
                            eurl.appendChild(turl);
                        }
                    }
                } else {
                    listener.getLogger().println("Issues retrieved for project " + a_projectname + " are null.");
                    listener.getLogger().println("Error returned by kwjlib: " + KWWebAPIService.getError());
                }

                // write the content into xml file (possibly on remote host)
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Source xmlSource = new DOMSource(doc);
                Result outputTarget = new StreamResult(outputStream);
                TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
                InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
                FilePath outputFile = new FilePath(channel, a_filename);
                outputFile.copyFrom(is);
            } catch (ParserConfigurationException pce) {
                listener.getLogger().println("ERROR while generating XML - ParserConfigurationException: " +
                        pce.getMessage());
                return 1;
            } catch (TransformerException tfe) {
                listener.getLogger().println("ERROR while generating XML - TransformerException:"
                        + tfe.getMessage() + "      stack:          " + tfe.getStackTrace().toString());
                return 1;
            } catch (IOException ioe) {
                listener.getLogger().println("ERROR while generating XML - IOException:"
                        + ioe.getMessage());
                return 1;
            } catch (InterruptedException ie) {
                listener.getLogger().println("ERROR while generating XML - InterruptedException:"
                        + ie.getMessage());
                return 1;
            }
        } else {
            listener.getLogger().println("Failed to connect to web API. Error message: " + KWservice.getError());
            listener.getLogger().println("Failed to connect to web API. Last request: " + KWservice.getLastRequest());
            return 1;
        }
        FilePath outputFile = new FilePath(channel, a_filename);
        try {
            if (outputFile.exists() && outputFile.length() > 0)
                listener.getLogger().println("Creation of XML file complete. Closing connection to Web API.");
            else
                listener.getLogger().println("Creation of XML file failed. You may have to run the kwauth command on your machine (slave host in a master/slave configuration).");
        } catch (IOException ioe) {
            listener.getLogger().println("ERROR while generating XML - IOException:"
                    + ioe.getMessage());
            return 1;
        } catch (InterruptedException ie) {
            listener.getLogger().println("ERROR while generating XML - InterruptedException:"
                    + ie.getMessage());
            return 1;
        }
        return 0;
    }
}
