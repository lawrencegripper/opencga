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

package org.opencb.opencga.core.models;

import org.opencb.biodata.models.commons.OntologyTerm;

import java.util.List;
import java.util.Map;

/**
 * Created by pfurio on 05/06/17.
 */
public class ClinicalAnalysis extends PrivateStudyUid {

    private String id;
    @Deprecated
    private String name;
    private String uuid;
    private String description;
    private Type type;

    private OntologyTerm disease;

    private File germline;
    private File somatic;

    private Individual proband;
    private Family family;
    private List<Interpretation> interpretations;

    private Priority priority;

    private String creationDate;
    private String modificationDate;
    private String dueDate;
    private Status status;
    private int release;
    private Map<String, Object> attributes;

    public enum Priority {
        URGENT, HIGH, MEDIUM, LOW
    }

    public enum Type {
        SINGLE, DUO, TRIO, FAMILY, AUTO, MULTISAMPLE
    }

    // Todo: Think about a better place to have this enum
    @Deprecated
    public enum Action {
        ADD,
        SET,
        REMOVE
    }

    public ClinicalAnalysis() {
    }

    public ClinicalAnalysis(String id, String description, Type type, OntologyTerm disease, File germline, File somatic, Individual proband,
                            Family family, List<Interpretation> interpretations, Priority priority, String creationDate, String dueDate,
                            Status status, int release, Map<String, Object> attributes) {
        this.id = id;
        this.description = description;
        this.type = type;
        this.disease = disease;
        this.germline = germline;
        this.somatic = somatic;
        this.proband = proband;
        this.family = family;
        this.interpretations = interpretations;
        this.priority = priority;
        this.creationDate = creationDate;
        this.dueDate = dueDate;
        this.status = status;
        this.release = release;
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClinicalAnalysis{");
        sb.append("id='").append(id).append('\'');
        sb.append(", uuid='").append(uuid).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append(", type=").append(type);
        sb.append(", disease=").append(disease);
        sb.append(", germline=").append(germline);
        sb.append(", somatic=").append(somatic);
        sb.append(", proband=").append(proband);
        sb.append(", family=").append(family);
        sb.append(", interpretations=").append(interpretations);
        sb.append(", priority=").append(priority);
        sb.append(", creationDate='").append(creationDate).append('\'');
        sb.append(", modificationDate='").append(modificationDate).append('\'');
        sb.append(", dueDate='").append(dueDate).append('\'');
        sb.append(", status=").append(status);
        sb.append(", release=").append(release);
        sb.append(", attributes=").append(attributes);
        sb.append('}');
        return sb.toString();
    }

    public String getUuid() {
        return uuid;
    }

    public ClinicalAnalysis setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getId() {
        return id;
    }

    public ClinicalAnalysis setId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ClinicalAnalysis setUid(long uid) {
        super.setUid(uid);
        return this;
    }

    @Override
    public ClinicalAnalysis setStudyUid(long studyUid) {
        super.setStudyUid(studyUid);
        return this;
    }

    public String getName() {
        return name;
    }

    public ClinicalAnalysis setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ClinicalAnalysis setDescription(String description) {
        this.description = description;
        return this;
    }

    public Type getType() {
        return type;
    }

    public ClinicalAnalysis setType(Type type) {
        this.type = type;
        return this;
    }

    public OntologyTerm getDisease() {
        return disease;
    }

    public ClinicalAnalysis setDisease(OntologyTerm disease) {
        this.disease = disease;
        return this;
    }

    public File getGermline() {
        return germline;
    }

    public ClinicalAnalysis setGermline(File germline) {
        this.germline = germline;
        return this;
    }

    public File getSomatic() {
        return somatic;
    }

    public ClinicalAnalysis setSomatic(File somatic) {
        this.somatic = somatic;
        return this;
    }

    public Individual getProband() {
        return proband;
    }

    public ClinicalAnalysis setProband(Individual proband) {
        this.proband = proband;
        return this;
    }

    public Family getFamily() {
        return family;
    }

    public ClinicalAnalysis setFamily(Family family) {
        this.family = family;
        return this;
    }

    public List<Interpretation> getInterpretations() {
        return interpretations;
    }

    public ClinicalAnalysis setInterpretations(List<Interpretation> interpretations) {
        this.interpretations = interpretations;
        return this;
    }

    public Priority getPriority() {
        return priority;
    }

    public ClinicalAnalysis setPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public String getDueDate() {
        return dueDate;
    }

    public ClinicalAnalysis setDueDate(String dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public ClinicalAnalysis setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public ClinicalAnalysis setModificationDate(String modificationDate) {
        this.modificationDate = modificationDate;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public ClinicalAnalysis setStatus(Status status) {
        this.status = status;
        return this;
    }

    public int getRelease() {
        return release;
    }

    public ClinicalAnalysis setRelease(int release) {
        this.release = release;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public ClinicalAnalysis setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

}
