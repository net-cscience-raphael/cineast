package org.vitrivr.cineast.core.data.entities;

import java.nio.file.Path;

import org.vitrivr.cineast.core.data.ExistenceCheck;
import org.vitrivr.cineast.core.data.MediaType;
import org.vitrivr.cineast.core.db.dao.reader.MultimediaObjectLookup;
import org.vitrivr.cineast.core.idgenerator.ObjectIdGenerator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author rgasser
 * @version 1.0
 * @created 10.01.17
 */
public class MultimediaObjectDescriptor implements ExistenceCheck {


    /** Name of the entity in the persistence layer. */
    public static final String ENTITY = "cineast_multimediaobject";

    /** Field names in the persistence layer. */
    public static final String[] FIELDNAMES = {"objectid", "mediatype", "name", "path"};

    private static MultimediaObjectLookup mlookup = new MultimediaObjectLookup();

    private final String objectId;
    private final String name, path;
    private final boolean exists;
    private final int mediatypeId;

    public static MultimediaObjectDescriptor makeMultimediaDescriptor(String objectId, String name, String path, MediaType type) {
        return new MultimediaObjectDescriptor(objectId, name, path, type, true);
    }

    /**
     * Convenience method to create a MultimediaObjectDescriptor marked as new. The method will assign
     * a new ID to this MultimediaObjectDescriptor using the provided ObjectIdGenerator.
     *
     * @param generator ObjectIdGenerator used for ID generation.
     * @param path The Path that points to the file for which a new MultimediaObjectDescriptor should be created.
     * @param type MediaType of the new MultimediaObjectDescriptor
     * @return A new MultimediaObjectDescriptor
     */
    public static MultimediaObjectDescriptor newMultimediaObjectDescriptor(ObjectIdGenerator generator, Path path, MediaType type) {
        String objectId = generator.next(path, type);
        return new MultimediaObjectDescriptor(objectId, path.getFileName().toString(), path.toString(), type, false);
    }

    /**
     * 
     * Convenience method to lookup a MultimediaObjectDescriptor for a given path and type or create a new one if needed.
     * If a new descriptor is required, newMultimediaObjectDescriptor is used.
     * 
     * @param generator ObjectIdGenerator used for ID generation.
     * @param path The Path that points to the file for which a new MultimediaObjectDescriptor should be created.
     * @param type MediaType of the new MultimediaObjectDescriptor
     * @return the existing or a new MultimediaObjectDescriptor
     */
    public static MultimediaObjectDescriptor getOrCreateMultimediaObjectDescriptor(ObjectIdGenerator generator, Path path, MediaType type){
      MultimediaObjectDescriptor descriptor = mlookup.lookUpObjectByPath(path.getFileName().toString());
      if(descriptor.exists() && descriptor.getMediatype() == type){
        return descriptor;
      }
      return newMultimediaObjectDescriptor(generator, path, type);
    }
    
    /**
     * Default constructor for an empty MultimediaObjectDescriptor.
     */
    public MultimediaObjectDescriptor() {
        this("", "", "", MediaType.VIDEO, false);
    }

    /**
     * Default constructor for an empty MultimediaObjectDescriptor.
     */
    public MultimediaObjectDescriptor(String objectId, String name, String path, MediaType mediatypeId, boolean exists) {
      this.objectId = objectId;
      this.name = name;
      this.path = path;
      this.mediatypeId = mediatypeId.getId();
      this.exists = exists;
    }

    @JsonProperty
    public final String getObjectId() {
      return objectId;
    }

    @JsonProperty
    public final String getName() {
      return name;
    }

    @JsonProperty
    public final String getPath() {
      return path;
    }

    @JsonProperty
    public final MediaType getMediatype() {
        return MediaType.fromId(this.mediatypeId);
    }

    @JsonIgnore
    public final int getMediatypeId() {
        return this.mediatypeId;
    }

    @Override
    @JsonIgnore
    public boolean exists() {
        return this.exists;
    }

    @Override
    public String toString() {
      return "MultimediaObjectDescriptor(" + objectId + ")";
    }
}
