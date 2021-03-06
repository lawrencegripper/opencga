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

package org.opencb.opencga.storage.benchmark.variant.generators;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Throwables;
import org.opencb.commons.datastore.core.Query;
import org.opencb.opencga.storage.benchmark.variant.queries.RandomQueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * Created on 06/04/17.
 *
 * @author Jacobo Coll &lt;jacobo167@gmail.com&gt;
 */
public abstract class QueryGenerator {
    public static final String DATA_DIR = "dataDir";
    public static final String ARITY = "arity";
    public static final String FILE = "file";
    public static final String OUT_DIR = "outDir";
    public static final String USER_PROPERTIES_FILE = "user.properties";
    protected Random random;

    private Logger logger = LoggerFactory.getLogger(getClass());
    private int arity;

    public void setUp(Map<String, String> params) {
        random = new Random(System.nanoTime());
        arity = Integer.parseInt(params.getOrDefault(ARITY, "1"));
    }

    public <T> T readYmlFile(Path path, Class<T> clazz) {
        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            return objectMapper.readValue(inputStream, clazz);
        } catch (IOException e) {
            logger.error("Error reading file " + path, e);
            throw Throwables.propagate(e);
        }
    }

    public abstract Query generateQuery(Query query);

    public String getQueryId() {
        return "";
    }

    protected int getArity() {
        return arity;
    }

    public QueryGenerator setArity(int arity) {
        this.arity = arity;
        return this;
    }

    protected void appendRandomSessionId(List<String> sessionIds, Query query) {
        if (Objects.nonNull(sessionIds)) {
            query.append("sid", sessionIds.get(random.nextInt(sessionIds.size())));
        }
    }

    protected void appendbaseQuery(RandomQueries randomQueries, Query query) {
        if (Objects.nonNull(randomQueries.getBaseQuery())) {
            query.putAll(randomQueries.getBaseQuery());
        }
    }
}
