/*
 * Copyright 2015-2017 OpenCB
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

package org.opencb.opencga.catalog.utils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResult;
import org.opencb.commons.utils.StringUtils;
import org.opencb.opencga.catalog.managers.CatalogManagerExternalResource;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.db.api.SampleDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.managers.FileUtils;
import org.opencb.opencga.catalog.managers.CatalogManager;
import org.opencb.opencga.core.common.TimeUtils;
import org.opencb.opencga.core.models.*;
import org.opencb.opencga.core.models.File;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.opencb.opencga.catalog.utils.FileMetadataReader.VARIANT_FILE_METADATA;

public class FileMetadataReaderTest {

    @Rule
    public CatalogManagerExternalResource catalogManagerExternalResource = new CatalogManagerExternalResource();

    public static final String PASSWORD = "asdf";
    private CatalogManager catalogManager;
    private String sessionIdUser;
    private Project project;
    private Study study;
    private File folder;
    private URI vcfFileUri;
    public static final String VCF_FILE_NAME = "variant-test-file.vcf.gz";
    public static final String BAM_FILE_NAME = "HG00096.chrom20.small.bam";
    private URI bamFileUri;
    private final List<String> expectedSampleNames = Arrays.asList("NA19600", "NA19660", "NA19661", "NA19685");

    @Before
    public void setUp() throws IOException, CatalogException, URISyntaxException {
        catalogManager = catalogManagerExternalResource.getCatalogManager();

        catalogManager.getUserManager().create("user", "User Name", "mail@ebi.ac.uk", PASSWORD, "", null, Account.FULL, null, null);
        sessionIdUser = catalogManager.getUserManager().login("user", PASSWORD);
        project = catalogManager.getProjectManager().create("1000G", "Project about some genomes", "", "ACME", "Homo sapiens",
                null, null, "GRCh38", new QueryOptions(), sessionIdUser).first();
        study = catalogManager.getStudyManager().create(project.getId(), "phase1", null, "Phase 1", Study.Type.TRIO, null, "Done", null, null, null, null, null, null, null, null, sessionIdUser).first();
        folder = catalogManager.getFileManager().createFolder(study.getId(), Paths.get("data/vcf/").toString(), null, true,
                null, QueryOptions.empty(), sessionIdUser).first();

        Path vcfPath = catalogManagerExternalResource.getOpencgaHome().resolve(VCF_FILE_NAME);
        Files.copy(this.getClass().getClassLoader().getResourceAsStream("biofiles/" + VCF_FILE_NAME), vcfPath, StandardCopyOption.REPLACE_EXISTING);
        vcfFileUri = vcfPath.toUri();

        Path bamPath = catalogManagerExternalResource.getOpencgaHome().resolve(BAM_FILE_NAME);
        Files.copy(this.getClass().getClassLoader().getResourceAsStream("biofiles/" + BAM_FILE_NAME), bamPath, StandardCopyOption.REPLACE_EXISTING);
        bamFileUri = bamPath.toUri();

    }

    @Test
    public void testCreate() throws CatalogException {
        QueryResult<File> fileQueryResult = FileMetadataReader.get(catalogManager).
                create(study.getFqn(), vcfFileUri, folder.getPath() + VCF_FILE_NAME, "", false, null, sessionIdUser);

        File file = fileQueryResult.first();

        assertEquals(File.FileStatus.STAGE, file.getStatus().getName());
        assertEquals(File.Format.VCF, file.getFormat());
        assertEquals(File.Bioformat.VARIANT, file.getBioformat());
        assertNotNull(file.getAttributes().get(VARIANT_FILE_METADATA));
        assertEquals(4, file.getSamples().size());
        assertEquals(21499, file.getSize());

        new FileUtils(catalogManager).upload(vcfFileUri, file, null, sessionIdUser, false, false, true, true, Integer.MAX_VALUE);
        file = catalogManager.getFileManager().get(study.getFqn(), file.getPath(), null, sessionIdUser).first();

        assertEquals(File.FileStatus.READY, file.getStatus().getName());
        assertEquals(File.Format.VCF, file.getFormat());
        assertEquals(File.Bioformat.VARIANT, file.getBioformat());
        assertNotNull(file.getAttributes().get(VARIANT_FILE_METADATA));
        assertNotNull(((Map) file.getAttributes().get(VARIANT_FILE_METADATA)).get("sampleIds"));
        assertEquals(4, ((List) ((Map) file.getAttributes().get(VARIANT_FILE_METADATA)).get("sampleIds")).size());
        assertNotNull(((Map) file.getAttributes().get(VARIANT_FILE_METADATA)).get("header"));
        assertEquals(4, file.getSamples().size());
        assertEquals(21499, file.getSize());
    }

    @Test
    public void testGetBasicMetadata() throws CatalogException, IOException {
        File file = catalogManager.getFileManager().create(study.getFqn(), new File().setPath(folder.getPath() + "test.txt"), false,
                StringUtils.randomString(1000), null, sessionIdUser).first();

        assertEquals(1000, file.getSize());
        String creationDate = file.getCreationDate();
        String modificationDate = file.getModificationDate();

        URI fileUri = catalogManager.getFileManager().getUri(file);

        try {
            Thread.sleep(1000); //Sleep 1 second to see changes on the "modificationDate"
        } catch (InterruptedException ignored) {}

        OutputStream outputStream = new FileOutputStream(Paths.get(fileUri).toFile(), true);
        byte[] bytes2 = StringUtils.randomString(100).getBytes();
        outputStream.write(bytes2);
        outputStream.close();

        file = FileMetadataReader.get(catalogManager).
                setMetadataInformation(file, null, null, sessionIdUser, false);

        assertEquals(1000 + bytes2.length, file.getSize());
        assertTrue(TimeUtils.toDate(modificationDate).getTime() < TimeUtils.toDate(file.getModificationDate()).getTime());
        assertEquals(creationDate, file.getCreationDate());
    }

    @Test
    public void testGetMetadataFromVcf() throws CatalogException, FileNotFoundException {
        File file = catalogManager.getFileManager().upload(study.getFqn(), new FileInputStream(Paths.get(vcfFileUri).toFile()),
                new File().setPath(folder.getPath() + VCF_FILE_NAME), false, false, sessionIdUser).first();

        assertTrue(file.getSize() > 0);

        file = FileMetadataReader.get(catalogManager).
                setMetadataInformation(file, null, null, sessionIdUser, false);

        assertEquals(File.FileStatus.READY, file.getStatus().getName());
        assertEquals(File.Format.VCF, file.getFormat());
        assertEquals(File.Bioformat.VARIANT, file.getBioformat());
        assertNotNull(file.getAttributes().get(VARIANT_FILE_METADATA));
        assertEquals(4, file.getSamples().size());
        assertEquals(expectedSampleNames, ((Map<String, Object>) file.getAttributes().get(VARIANT_FILE_METADATA)).get("sampleIds"));
        catalogManager.getSampleManager().get(study.getFqn(),
                new Query(SampleDBAdaptor.QueryParams.ID.key(),
                        file.getSamples().stream().map(Sample::getId).collect(Collectors.toList())), new QueryOptions(),
                sessionIdUser).getResult();

        assertTrue(expectedSampleNames.containsAll(file.getSamples().stream().map(Sample::getId).collect(Collectors.toSet())));
    }


    @Test
    public void testGetMetadataFromVcfWithAlreadyExistingSamples() throws CatalogException, FileNotFoundException {
        //Create the samples in the same order than in the file
        for (String sampleName : expectedSampleNames) {
            catalogManager.getSampleManager().create(study.getFqn(), sampleName, "", "", null, false, null, new HashMap<>(), Collections
                    .emptyMap(), new QueryOptions(), sessionIdUser);
        }
        testGetMetadataFromVcf();
    }

    @Test
    public void testGetMetadataFromVcfWithAlreadyExistingSamplesUnsorted() throws CatalogException, FileNotFoundException {
        //Create samples in a different order than the file order
        catalogManager.getSampleManager().create(study.getFqn(), expectedSampleNames.get(2), "", "", null, false, null, new
                HashMap<>(), Collections.emptyMap(), new QueryOptions(), sessionIdUser);

        catalogManager.getSampleManager().create(study.getFqn(), expectedSampleNames.get(0), "", "", null, false, null, new HashMap<>(), Collections.emptyMap(), new QueryOptions(), sessionIdUser);
        catalogManager.getSampleManager().create(study.getFqn(), expectedSampleNames.get(3), "", "", null, false, null, new HashMap<>(), Collections.emptyMap(), new QueryOptions(), sessionIdUser);
        catalogManager.getSampleManager().create(study.getFqn(), expectedSampleNames.get(1), "", "", null, false, null, new HashMap<>(), Collections.emptyMap(), new QueryOptions(), sessionIdUser);

        testGetMetadataFromVcf();
    }

    @Test
    public void testGetMetadataFromVcfWithSomeExistingSamples() throws CatalogException, FileNotFoundException {
        catalogManager.getSampleManager().create(study.getFqn(), new Sample().setId(expectedSampleNames.get(2)), new QueryOptions(),
                sessionIdUser);
        catalogManager.getSampleManager().create(study.getFqn(), new Sample().setId(expectedSampleNames.get(0)), new QueryOptions(),
                sessionIdUser);

        testGetMetadataFromVcf();
    }

    @Test
    public void testDoNotOverwriteSampleIds() throws CatalogException, FileNotFoundException {
        File file = catalogManager.getFileManager().upload(study.getFqn(), new FileInputStream(Paths.get(vcfFileUri).toFile()),
                new File().setPath(folder.getPath() + VCF_FILE_NAME), false, false, sessionIdUser).first();
        assertEquals(File.FileStatus.READY, file.getStatus().getName());
        assertEquals(File.Format.VCF, file.getFormat());
        assertEquals(File.Bioformat.VARIANT, file.getBioformat());
        assertNotNull(file.getAttributes().get(VARIANT_FILE_METADATA));
        assertEquals(4, file.getSamples().size());

        //Add a sampleId
        String sampleId = catalogManager.getSampleManager().create(study.getFqn(), new Sample().setId("Bad_Sample"), null, sessionIdUser)
                .first().getId();
        catalogManager.getFileManager().update(study.getFqn(), file.getPath(), new ObjectMap(FileDBAdaptor.QueryParams.SAMPLES.key(),
                        Collections.singletonList(sampleId)), new QueryOptions(), sessionIdUser);

        file = catalogManager.getFileManager().get(study.getFqn(), file.getPath(), null, sessionIdUser).first();
        assertEquals(5, file.getSamples().size());
        assertEquals(sampleId, file.getSamples().get(4).getId());
    }

    @Test
    public void testGetMetadataFromBam()
            throws CatalogException, FileNotFoundException {

        File file = catalogManager.getFileManager().upload(study.getFqn(), new FileInputStream(Paths.get(bamFileUri).toFile()),
                new File().setPath(folder.getPath() + BAM_FILE_NAME), false, false, sessionIdUser).first();

        assertTrue(file.getSize() > 0);

        file = FileMetadataReader.get(catalogManager).
                setMetadataInformation(file, null, null, sessionIdUser, false);

        assertEquals(File.FileStatus.READY, file.getStatus().getName());
//        assertEquals(File.Format.GZIP, file.getFormat());
        assertEquals(File.Bioformat.ALIGNMENT, file.getBioformat());
        assertNotNull(file.getAttributes().get("alignmentHeader"));
        assertEquals(1, file.getSamples().size());
        assertEquals("HG00096", catalogManager.getSampleManager().get(study.getFqn(), file.getSamples().get(0).getId(), null,
                sessionIdUser).first().getId());
    }


}