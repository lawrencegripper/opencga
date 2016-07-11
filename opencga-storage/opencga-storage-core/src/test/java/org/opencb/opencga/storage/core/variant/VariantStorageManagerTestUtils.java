package org.opencb.opencga.storage.core.variant;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.opencb.biodata.formats.io.FileFormatException;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.test.GenericTest;
import org.opencb.opencga.core.common.IOUtils;
import org.opencb.opencga.storage.core.StorageETLResult;
import org.opencb.opencga.storage.core.metadata.StudyConfiguration;
import org.opencb.opencga.storage.core.exceptions.StorageManagerException;
import org.opencb.opencga.storage.core.variant.adaptors.VariantDBAdaptor;
import org.opencb.opencga.storage.core.variant.annotation.VariantAnnotationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Objects;

/**
 * Created by jacobo on 31/05/15.
 */
@Ignore
public abstract class VariantStorageManagerTestUtils extends GenericTest implements VariantStorageTest {


    public static final String VCF_TEST_FILE_NAME = "10k.chr22.phase3_shapeit2_mvncall_integrated_v5.20130502.genotypes.vcf.gz";
    public static final String SMALL_VCF_TEST_FILE_NAME = "variant-test-file.vcf.gz";
    public static final String VCF_CORRUPTED_FILE_NAME = "variant-test-file-corrupted.vcf";
    public static final int NUM_VARIANTS = 9792;
    public static final int STUDY_ID = 1;
    public static final String STUDY_NAME = "1000g";
    public static final String DB_NAME = "opencga_variants_test";
    public static final int FILE_ID = 6;

    protected static URI inputUri;
    protected static URI smallInputUri;
    protected static URI corruptedInputUri;
    protected static URI outputUri;
    protected VariantStorageManager variantStorageManager;
    public static Logger logger;
    private static Path rootDir = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void _beforeClass() throws Exception {
//        System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "debug");
        newRootDir();
        if (rootDir.toFile().exists()) {
            IOUtils.deleteDirectory(rootDir);
            Files.createDirectories(rootDir);
        }
        Path inputPath = rootDir.resolve(VCF_TEST_FILE_NAME);
        Path smallInputPath = rootDir.resolve(SMALL_VCF_TEST_FILE_NAME);
        Path corruptedInputPath = rootDir.resolve(VCF_CORRUPTED_FILE_NAME);
        Files.copy(VariantStorageManagerTest.class.getClassLoader().getResourceAsStream(VCF_TEST_FILE_NAME), inputPath,
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(VariantStorageManagerTest.class.getClassLoader().getResourceAsStream(SMALL_VCF_TEST_FILE_NAME), smallInputPath,
                StandardCopyOption.REPLACE_EXISTING);
        Files.copy(VariantStorageManagerTest.class.getClassLoader().getResourceAsStream(VCF_CORRUPTED_FILE_NAME), corruptedInputPath,
                StandardCopyOption.REPLACE_EXISTING);

        inputUri = inputPath.toUri();
        smallInputUri = smallInputPath.toUri();
        corruptedInputUri = corruptedInputPath.toUri();
        outputUri = rootDir.toUri();
        logger = LoggerFactory.getLogger(VariantStorageManagerTest.class);

    }

    public static URI getResourceUri(String resourceName) throws IOException {
        Path rootDir = getTmpRootDir();
        Path resourcePath = rootDir.resolve(resourceName);
        if (!resourcePath.getParent().toFile().exists()) {
            Files.createDirectory(resourcePath.getParent());
        }
        if (!resourcePath.toFile().exists()) {
            Files.copy(VariantStorageManagerTest.class.getClassLoader().getResourceAsStream(resourceName), resourcePath, StandardCopyOption
                    .REPLACE_EXISTING);
        }
        return resourcePath.toUri();
    }

    protected static Path getTmpRootDir() throws IOException {
//        Path rootDir = Paths.get("/tmp", "VariantStorageManagerTest");

        if (rootDir == null) {
            newRootDir();
        }
        return rootDir;
    }

    private static void newRootDir() throws IOException {
        rootDir = Paths.get("target/test-data", "junit-opencga-storage-" + System.currentTimeMillis() + "_" + RandomStringUtils.randomAlphanumeric(5));
        Files.createDirectories(rootDir);
    }

    public static void setRootDir(Path rootDir) {
        VariantStorageManagerTestUtils.rootDir = rootDir;
    }

//    private static File tempFile = null;
//    protected static Path getTmpRootDir() throws IOException {
//        if (tempFile == null) {
//            tempFile = File.createTempFile("opencga-variants-storage-test-", null);
//        }
//        return tempFile.toPath();
//    }

//    @ClassRule
//    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
//
//    protected static Path getTmpRootDir() throws IOException {
//        return temporaryFolder.getRoot().toPath();
//    }

    @Before
    public void before() throws Exception {
        clearDB(DB_NAME);
    }

    @Before
    public final void _before() throws Exception {
        variantStorageManager = getVariantStorageManager();
    }


    /* ---------------------------------------------------- */
    /* Static methods to run a simple ETL to index Variants */
    /* ---------------------------------------------------- */


    public static StorageETLResult runETL(VariantStorageManager variantStorageManager, ObjectMap options)
            throws IOException, FileFormatException, StorageManagerException {
        return runETL(variantStorageManager, options, true, true, true);
    }

    public static StorageETLResult runETL(VariantStorageManager variantStorageManager, ObjectMap options,
                                   boolean doExtract,
                                   boolean doTransform,
                                   boolean doLoad)
            throws IOException, FileFormatException, StorageManagerException {
        return runETL(variantStorageManager, inputUri, outputUri, options, options, options, options, options, options, options,
                doExtract, doTransform, doLoad);
    }

    public static StorageETLResult runDefaultETL(VariantStorageManager variantStorageManager, StudyConfiguration studyConfiguration)
            throws URISyntaxException, IOException, FileFormatException, StorageManagerException {
        return runDefaultETL(inputUri, variantStorageManager, studyConfiguration);
    }

    public static StorageETLResult runDefaultETL(URI inputUri, VariantStorageManager variantStorageManager, StudyConfiguration studyConfiguration)
            throws URISyntaxException, IOException, FileFormatException, StorageManagerException {
        return runDefaultETL(inputUri, variantStorageManager, studyConfiguration, new ObjectMap());
    }

    public static StorageETLResult runDefaultETL(URI inputUri, VariantStorageManager variantStorageManager,
                                                 StudyConfiguration studyConfiguration, ObjectMap params)
            throws URISyntaxException, IOException, FileFormatException, StorageManagerException {
        return runDefaultETL(inputUri, variantStorageManager, studyConfiguration, params, true, true);
    }

    public static StorageETLResult runDefaultETL(URI inputUri, VariantStorageManager variantStorageManager,
                                                 StudyConfiguration studyConfiguration, ObjectMap params, boolean doTransform, boolean doLoad)
            throws URISyntaxException, IOException, FileFormatException, StorageManagerException {

        ObjectMap newParams = new ObjectMap(params);

        newParams.put(VariantStorageManager.Options.STUDY_CONFIGURATION.key(), studyConfiguration);
        newParams.putIfAbsent(VariantStorageManager.Options.AGGREGATED_TYPE.key(), studyConfiguration.getAggregation());
        newParams.putIfAbsent(VariantStorageManager.Options.STUDY_ID.key(), studyConfiguration.getStudyId());
        newParams.putIfAbsent(VariantStorageManager.Options.STUDY_NAME.key(), studyConfiguration.getStudyName());
        newParams.putIfAbsent(VariantStorageManager.Options.DB_NAME.key(), DB_NAME);
        newParams.putIfAbsent(VariantStorageManager.Options.FILE_ID.key(), FILE_ID);
        newParams.putIfAbsent(VariantStorageManager.Options.TRANSFORM_FORMAT.key(), "json");
        newParams.putIfAbsent(VariantStorageManager.Options.ANNOTATE.key(), true);
        newParams.putIfAbsent(VariantAnnotationManager.SPECIES, "hsapiens");
        newParams.putIfAbsent(VariantAnnotationManager.ASSEMBLY, "GRc37");
        newParams.putIfAbsent(VariantStorageManager.Options.CALCULATE_STATS.key(), true);

        StorageETLResult storageETLResult = runETL(variantStorageManager, inputUri, outputUri, newParams, newParams, newParams,
                newParams, newParams, newParams, newParams, true, doTransform, doLoad);

        try (VariantDBAdaptor dbAdaptor = variantStorageManager.getDBAdaptor(DB_NAME)) {
            StudyConfiguration newStudyConfiguration = dbAdaptor.getStudyConfigurationManager().getStudyConfiguration(studyConfiguration.getStudyId(), null).first();
            if (newStudyConfiguration != null) {
                studyConfiguration.copy(newStudyConfiguration);
            }
        }

        return storageETLResult;
    }

    public static StorageETLResult runETL(VariantStorageManager variantStorageManager, URI inputUri, URI outputUri,
                                   ObjectMap extractParams,
                                   ObjectMap preTransformParams, ObjectMap transformParams, ObjectMap postTransformParams,
                                   ObjectMap preLoadParams, ObjectMap loadParams, ObjectMap postLoadParams,
                                   boolean doExtract,
                                   boolean doTransform,
                                   boolean doLoad)
            throws IOException, FileFormatException, StorageManagerException {
        ObjectMap params = new ObjectMap();
        params.putAll(extractParams);
        params.putAll(preTransformParams);
        params.putAll(transformParams);
        params.putAll(postTransformParams);
        params.putAll(preLoadParams);
        params.putAll(loadParams);
        params.putAll(postLoadParams);
        return runETL(variantStorageManager, inputUri, outputUri, params, doExtract, doTransform, doLoad);
    }

    public static StorageETLResult runETL(VariantStorageManager variantStorageManager, URI inputUri, URI outputUri,
                                   ObjectMap params,
                                   boolean doExtract,
                                   boolean doTransform,
                                   boolean doLoad)
            throws IOException, FileFormatException, StorageManagerException {


        params.putIfAbsent(VariantStorageManager.Options.DB_NAME.key(), DB_NAME);
        variantStorageManager.getConfiguration()
                .getStorageEngine(variantStorageManager.getStorageEngineId()).getVariant().getOptions().putAll(params);
        StorageETLResult storageETLResult =
                variantStorageManager.index(Collections.singletonList(inputUri), outputUri, doExtract, doTransform, doLoad).get(0);

        checkFileExists(storageETLResult.getExtractResult());
        checkFileExists(storageETLResult.getPreTransformResult());
        checkFileExists(storageETLResult.getTransformResult());
        checkFileExists(storageETLResult.getPostTransformResult());
        checkFileExists(storageETLResult.getPreLoadResult());
        checkFileExists(storageETLResult.getLoadResult());
        checkFileExists(storageETLResult.getPostLoadResult());

        return storageETLResult;
    }

    public static void checkFileExists(URI uri) {
        if (uri != null && ( uri.getScheme() == null || Objects.equals(uri.getScheme(), "file") )) {
            Assert.assertTrue("Intermediary file " + uri + " does not exist", Paths.get(uri).toFile().exists());
        }
    }

    protected static StudyConfiguration newStudyConfiguration() {
        return new StudyConfiguration(STUDY_ID, STUDY_NAME);
    }


}
