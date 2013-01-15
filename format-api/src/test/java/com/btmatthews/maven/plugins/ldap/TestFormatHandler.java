/*
 * Copyright 2013 Brian Thomas Matthews
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

package com.btmatthews.maven.plugins.ldap;

import com.unboundid.ldap.sdk.*;
import com.unboundid.ldif.LDIFChangeRecord;
import com.unboundid.ldif.LDIFException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author <a href="mailto:brian@btmatthews.com">Brian Matthews</a>
 * @since 1.2.0
 */
public class TestFormatHandler {

    /**
     * Mock for the connection to the LDAP directory server.
     */
    @Mock
    private LDAPInterface connection;
    /**
     * Mock for the input stream from which directory entries are read.
     */
    @Mock
    private InputStream inputStream;
    /**
     * Mock for the output stream to which directory entries are written.
     */
    @Mock
    private OutputStream outputStream;
    /**
     * Mock for the object that reads LDAP directory entries from an input stream.
     */
    @Mock
    private FormatReader reader;
    /**
     * Mock for the object that writes LDAP directory entries to an output stream.
     */
    @Mock
    private FormatWriter writer;
    /**
     * Mock for the object used to log information and error messages.
     */
    @Mock
    private FormatLogger logger;
    /**
     * The main test fixture is which extends {@link AbstractFormatHandler}.
     */
    private FormatHandler handler;

    /**
     * Prepare for test case execution by creating the mock objects and test fixtures.
     */
    @Before
    public void setUp() {
        initMocks(this);
        handler = new AbstractFormatHandler() {
            @Override
            protected FormatWriter createWriter(final OutputStream outputStream,
                                                final FormatLogger logger) {
                return writer;
            }

            @Override
            protected FormatReader openReader(final InputStream inputStream,
                                              final FormatLogger logger) {
                return reader;
            }
        };
    }

    @Test
    public void handleLDIFExceptionWhileReading() throws Exception {
        doThrow(LDIFException.class).when(reader).nextRecord();
        handler.load(connection, inputStream, false, logger);
        verify(logger).logError(eq("Error parsing directory entry read from the input stream"), any(LDIFException.class));
        verify(reader).close();
    }

    @Test
    public void handleIOExceptionWhileReading() throws Exception {
        doThrow(IOException.class).when(reader).nextRecord();
        handler.load(connection, inputStream, false, logger);
        verify(logger).logError(eq("I/O error reading directory entry from input stream"), any(IOException.class));
        verify(reader).close();
    }

    @Test
    public void handleIOExceptionWhileClosing() throws Exception {
        doThrow(IOException.class).when(reader).close();
        handler.load(connection, inputStream, false, logger);
        verify(logger).logError(eq("I/O error closing the input stream reader"), any(IOException.class));
        verify(reader).close();
    }

    @Test
    public void handleLDAPExceptionWhileProcessing() throws Exception {
        final LDIFChangeRecord first = mock(LDIFChangeRecord.class);
        when(reader.nextRecord()).thenReturn(first);
        doThrow(LDAPException.class).when(first).processChange(same(connection));
        handler.load(connection, inputStream, false, logger);
        logger.logError(eq("Error loading directory entry into the LDAP directory server"), any(LDAPException.class));
        verify(reader).close();
    }

    @Test
    public void loadEmptyFile() throws IOException {
        handler.load(connection, inputStream, true, logger);
        verify(reader).close();
    }

    @Test
    public void loadFileWithOneItem() throws Exception {
        final LDIFChangeRecord first = mock(LDIFChangeRecord.class);
        when(reader.nextRecord()).thenReturn(first, null);
        handler.load(connection, inputStream, true, logger);
        verify(first).processChange(same(connection));
        verify(reader).close();
    }

    @Test
    public void loadFileWithTwoItems() throws Exception {
        final LDIFChangeRecord first = mock(LDIFChangeRecord.class);
        final LDIFChangeRecord second = mock(LDIFChangeRecord.class);
        when(reader.nextRecord()).thenReturn(first, second, null);
        handler.load(connection, inputStream, true, logger);
        verify(first).processChange(same(connection));
        verify(second).processChange(same(connection));
        verify(reader).close();
    }

    @Test
    public void dumpEmptyResultSet() throws Exception {
        final SearchResult result = FormatTestUtils.createSearchResult();
        when(connection.search(any(SearchRequest.class))).thenReturn(result);
        handler.dump(connection, "dc=btmatthews,dc=com", "(objectclass=*)", outputStream, logger);
        verify(writer).close();
    }

    @Test
    public void dumpResultSetWithOneItem() throws Exception {
        final SearchResultEntry first = FormatTestUtils.createSearchResultEntry(
                "ou=People,dc=btmatthews,dc=com",
                "ou", "People",
                "objectclass", "organisationalUnit");
        final SearchResult result = FormatTestUtils.createSearchResult(first);
        when(connection.search(any(SearchRequest.class))).thenReturn(result);
        handler.dump(connection, "dc=btmatthews,dc=com", "(objectclass=*)", outputStream, logger);
        verify(writer).printEntry(same(first));
        verify(writer).close();
    }

    @Test
    public void dumpResultSetWithTwoItems() throws Exception {
        final SearchResultEntry first = FormatTestUtils.createSearchResultEntry(
                "ou=People,dc=btmatthews,dc=com",
                "ou", "People",
                "objectclass", "organisationalUnit");
        final SearchResultEntry second = FormatTestUtils.createSearchResultEntry(
                "cn=Bart Simpson,ou=People,dc=btmatthews,dc=com",
                "cn", "Bart Simpson",
                "sn", "Simpson",
                "givenName", "Bart",
                "uid", "bsimpson",
                "objectclass", "inetOrgPerson");
        final SearchResult result = FormatTestUtils.createSearchResult(first, second);
        when(connection.search(any(SearchRequest.class))).thenReturn(result);
        handler.dump(connection, "dc=btmatthews,dc=com", "(objectclass=*)", outputStream, logger);
        verify(writer).printEntry(same(first));
        verify(writer).printEntry(same(second));
        verify(writer).close();
    }
}
