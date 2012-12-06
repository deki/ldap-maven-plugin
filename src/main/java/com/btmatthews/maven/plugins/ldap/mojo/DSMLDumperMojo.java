/*
 * Copyright 2008-2012 Brian Thomas Matthews
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

package com.btmatthews.maven.plugins.ldap.mojo;

import java.io.PrintWriter;

import netscape.ldap.util.DSMLWriter;
import netscape.ldap.util.LDAPWriter;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * This Mojo implements the dsml-dump goal which dumps content from the LDAP
 * directory server in DSML format.
 *
 * @author <a href="mailto:brian.matthews@btmatthews.com">Brian Matthews</a>
 * @since 1.0.0
 */
@Mojo(name = "dsml-dump")
public final class DSMLDumperMojo extends AbstractLDAPDumperMojo {

    /**
     * Create the LDAP writer that will dump LDAP entries in DSML format.
     *
     * @param writer The writer for the target output stream.
     * @return The LDAP writer.
     */
    @Override
    protected LDAPWriter openLDAPWriter(final PrintWriter writer) {
        writer.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        writer.print("<dsml:dsml xmlns:dsml=\"http://www.dsml.org/DSML\">\n");
        return new DSMLWriter(writer);
    }

    /**
     * Close the LDAP wrtier that was returned by openLDAPWriter.
     *
     * @param writer     The writer for the target output stream.
     * @param ldapWriter The LDAP writer.
     */
    @Override
    protected void closeLDAPWriter(final PrintWriter writer,
                                   final LDAPWriter ldapWriter) {
        writer.print("</dsml:dsml>\n");
    }
}
