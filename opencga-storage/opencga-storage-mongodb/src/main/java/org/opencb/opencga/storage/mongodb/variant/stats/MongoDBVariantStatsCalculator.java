package org.opencb.opencga.storage.mongodb.variant.stats;

import org.bson.Document;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.stats.VariantStats;
import org.opencb.biodata.tools.variant.stats.VariantStatsCalculator;
import org.opencb.commons.run.Task;
import org.opencb.opencga.storage.core.metadata.models.CohortMetadata;
import org.opencb.opencga.storage.core.metadata.models.StudyMetadata;
import org.opencb.opencga.storage.core.variant.adaptors.GenotypeClass;
import org.opencb.opencga.storage.core.variant.stats.VariantStatsWrapper;
import org.opencb.opencga.storage.mongodb.variant.converters.AbstractDocumentConverter;
import org.opencb.opencga.storage.mongodb.variant.converters.DocumentToStudyVariantEntryConverter;
import org.opencb.opencga.storage.mongodb.variant.converters.DocumentToVariantConverter;

import java.util.*;
import java.util.stream.Collectors;

import static org.opencb.opencga.storage.mongodb.variant.MongoDBVariantStorageEngine.MongoDBVariantOptions.DEFAULT_GENOTYPE;

/**
 * Created on 18/04/18.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public class MongoDBVariantStatsCalculator extends AbstractDocumentConverter implements Task<Document, VariantStatsWrapper> {

    private StudyMetadata studyMetadata;

    private Map<Integer, CohortMetadata> cohorts;
    private String unknownGenotype;
    private String defaultGenotype;
    private final DocumentToVariantConverter variantConverter;

    public MongoDBVariantStatsCalculator(StudyMetadata studyMetadata, Collection<CohortMetadata> cohorts, String unknownGenotype) {
        this.studyMetadata = studyMetadata;
        this.cohorts = cohorts.stream().collect(Collectors.toMap(CohortMetadata::getId, c -> c));
        this.unknownGenotype = unknownGenotype;


        List<String> defaultGenotypes = studyMetadata.getAttributes().getAsStringList(DEFAULT_GENOTYPE.key());

        defaultGenotype = defaultGenotypes.isEmpty() ? null : defaultGenotypes.get(0);
        if (GenotypeClass.UNKNOWN_GENOTYPE.equals(defaultGenotype)) {
            defaultGenotype = this.unknownGenotype;
        }

        variantConverter = new DocumentToVariantConverter();
    }

    @Override
    public List<VariantStatsWrapper> apply(List<Document> batch) throws Exception {
        List<VariantStatsWrapper> result = new ArrayList<>(batch.size());

        for (Document document : batch) {
            result.add(apply(document));
        }
        return result;
    }

    public VariantStatsWrapper apply(Document document) {
        Variant variant = variantConverter.convertToDataModelType(document);
        VariantStatsWrapper statsWrapper = null;

        List<Document> studies = getList(document, DocumentToVariantConverter.STUDIES_FIELD);
        for (Document study : studies) {
            Integer sid = study.getInteger(DocumentToStudyVariantEntryConverter.STUDYID_FIELD);
            if (studyMetadata.getId() == sid) {
                statsWrapper = calculateStats(variant, study);
                break;
            }
        }
        return statsWrapper;
    }

    public VariantStatsWrapper calculateStats(Variant variant, Document study) {
        VariantStatsWrapper statsWrapper = new VariantStatsWrapper(variant, new HashMap<>(cohorts.size()));

        Document gt = study.get(DocumentToStudyVariantEntryConverter.GENOTYPES_FIELD, Document.class);

        // Make a Set from the lists of genotypes for fast indexOf
        Map<String, Set<Integer>> gtsMap = new HashMap<>(gt.size());
        for (Map.Entry<String, Object> entry : gt.entrySet()) {
            gtsMap.put(entry.getKey(), new HashSet<>((Collection) entry.getValue()));
        }

        for (CohortMetadata cohort : cohorts.values()) {
            Map<String, Integer> gtStrCount = new HashMap<>();

            int unknownGenotypes = cohort.getSamples().size();
            for (Integer sampleId : cohort.getSamples()) {
                for (Map.Entry<String, Set<Integer>> entry : gtsMap.entrySet()) {
                    String gtStr = entry.getKey();
                    // If any ?/? is present in the DB, must be read as "unknownGenotype", usually "./."
                    if (GenotypeClass.UNKNOWN_GENOTYPE.equals(gtStr)) {
                        gtStr = unknownGenotype;
                    }
                    if ((entry.getValue()).contains(sampleId)) {
                        addGt(gtStrCount, gtStr, 1);
                        unknownGenotypes--;
                        break;
                    }
                }
            }

            // All the samples not present in any gt list must count as "defaultGenotype", usually "0/0"
            addGt(gtStrCount, defaultGenotype, unknownGenotypes);

            Map<Genotype, Integer> gtCountMap = new HashMap<>(gtStrCount.size());
            gtStrCount.forEach((str, count) -> gtCountMap.compute(new Genotype(str, variant.getReference(), variant.getAlternate()),
                    (key, value) -> value == null ? count : value + count));

            VariantStats stats = VariantStatsCalculator.calculate(variant, gtCountMap);
            statsWrapper.getCohortStats().put(cohort.getName(), stats);
        }

        return statsWrapper;
    }


    private void addGt(Map<String, Integer> gtStrCount, String gt, int num) {
        gtStrCount.compute(gt, (key, value) -> value == null ? num : value + num);
    }
}
