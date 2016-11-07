/*

    ngs-fhir  Mapping for FHIR XSDs.
    Copyright (c) 2014-2015 National Marrow Donor Program (NMDP)

    This library is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as published
    by the Free Software Foundation; either version 3 of the License, or (at
    your option) any later version.

    This library is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; with out even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
    License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this library;  if not, write to the Free Software Foundation,
    Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA.

    > http://www.gnu.org/licenses/lgpl.html

*/
package org.nmdp.ngs.fhir;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import static org.nmdp.ngs.fhir.FhirReader.read;

import java.net.URL;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;

import com.google.common.io.Files;
import com.google.common.io.Resources;

import org.junit.Test;

import org.nmdp.ngs.fhir.jaxb.FHIRPublicData;

/**
 * Unit test for FhirReader.
 */
public final class FhirReaderTest {

    @Test(expected=NullPointerException.class)
    public void testReadNullReader() throws Exception {
        read((Reader) null);
    }

    @Test(expected=NullPointerException.class)
    public void testReadNullFile() throws Exception {
        read((File) null);
    }

    @Test(expected=NullPointerException.class)
    public void testReadNullURL() throws Exception {
        read((URL) null);
    }

    @Test(expected=NullPointerException.class)
    public void testReadNullInputStream() throws Exception {
        read((InputStream) null);
    }

    @Test
    public void testReadFile() throws Exception {
        validate(read(createFile("fhir-example.xml")));
    }

    @Test(expected=IOException.class)
    public void testReadEmptyFile() throws Exception {
        read(createFile("empty.xml"));
    }

    @Test(expected=IOException.class)
    public void testReadInvalidSyntaxFile() throws Exception {
        read(createFile("invalid-syntax.xml"));
    }

    @Test(expected=IOException.class)
    public void testReadInvalidSchemaFile() throws Exception {
        read(createFile("invalid-schema.xml"));
    }

    @Test
    public void testReadURL() throws Exception {
        validate(read(createURL("fhir-example.xml")));
    }

    @Test
    public void testReadInputStream() throws Exception {
        validate(read(createInputStream("fhir-example.xml")));
    }

    private static void validate(final FHIRPublicData data) {
        assertNotNull(data);
        assertEquals("1.0", data.getVersion());
    }

    private static URL createURL(final String name) throws Exception {
        return FhirReaderTest.class.getResource(name);
    }

    private static InputStream createInputStream(final String name) throws IOException {
        return FhirReaderTest.class.getResourceAsStream(name);
    }

    private static File createFile(final String name) throws IOException {
        File file = File.createTempFile("fhirReaderTest", ".xml");
        Files.write(Resources.toByteArray(FhirReaderTest.class.getResource(name)), file);
        file.deleteOnExit();
        return file;
    }
}
