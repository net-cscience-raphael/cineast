package org.vitrivr.cineast.core.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.vitrivr.cineast.core.data.MediaType;
import org.vitrivr.cineast.core.run.ExtractionContextProvider;

import java.nio.file.Path;
import java.util.ArrayList;

/**
 * @author rgasser
 * @version 1.0
 * @created 13.01.17
 */
public class ImportConfig implements ExtractionContextProvider {
    /**
     *
     */
    public class InputConfig {
        private Path path;
        private String name;
        private String id;
        private Integer depth = 1;
        private Integer limit = Integer.MAX_VALUE;

        @JsonProperty
        public Path getPath() {
            return path;
        }
        public void setPath(Path path) {
            this.path = path;
        }

        @JsonProperty
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        @JsonProperty
        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }

        @JsonProperty
        public Integer getDepth() {
            return depth;
        }
        public void setDepth(Integer depth) {
            this.depth = depth;
        }

        @JsonProperty
        public Integer getLimit() {
            return limit;
        }
        public void setLimit(Integer limit) {
            this.limit = limit;
        }

    }

    /** */
    private MediaType type;

    /** */
    private InputConfig input;

    /** */
    private ArrayList<String> categories;

    /** */
    private ArrayList<String> exporters;

    /** */
    private DatabaseConfig database;

    @JsonCreator
    public ImportConfig() {

    }

    @JsonProperty
    public MediaType getType() {
        return type;
    }
    public void setType(MediaType type) {
        this.type = type;
    }

    @JsonProperty
    public InputConfig getInput() {
        return input;
    }
    public void setInput(InputConfig input) {
        this.input = input;
    }

    @JsonProperty
    public ArrayList<String> getCategories() {
        return categories;
    }
    public void setCategories(ArrayList<String> categories) {
        this.categories = categories;
    }

    @JsonProperty
    public ArrayList<String > getExporters() {
        return exporters;
    }
    public void setExporters(ArrayList<String> exporters) {
        this.exporters = exporters;
    }

    @JsonProperty
    public DatabaseConfig getDatabase() {
        return database;
    }
    public void setDatabase(DatabaseConfig database) {
        this.database = database;
    }

    @Override
    public Path inputPath() {
        if (this.input != null) {
            return this.input.getPath();
        } else {
            return null;
        }
    }

    /**
     *
     */
    @Override
    public MediaType sourceType() {
        return this.type;
    }

    /**
     * Limits the number of files that should be extracted. This a predicate is applied
     * before extraction starts. If extraction fails for some fails the effective number
     * of extracted files may be lower.
     *
     * @return A number greater than zero.
     */
    @Override
    public int limit() {
        return this.input.getLimit();
    }

    /**
     * Limits the depth of recursion when extraction folders of files.
     *
     * @return A number greater than zero.
     */
    @Override
    public int depth() {
        return this.input.getDepth();
    }
}