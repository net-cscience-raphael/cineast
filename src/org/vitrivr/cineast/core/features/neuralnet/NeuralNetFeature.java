package org.vitrivr.cineast.core.features.neuralnet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vitrivr.adam.grpc.AdamGrpc;
import org.vitrivr.cineast.core.config.Config;
import org.vitrivr.cineast.core.config.QueryConfig;
import org.vitrivr.cineast.core.data.FloatVectorImpl;
import org.vitrivr.cineast.core.data.SegmentContainer;
import org.vitrivr.cineast.core.data.StringDoublePair;
import org.vitrivr.cineast.core.data.providers.primitive.PrimitiveTypeProvider;
import org.vitrivr.cineast.core.db.*;
import org.vitrivr.cineast.core.features.abstracts.AbstractFeatureModule;
import org.vitrivr.cineast.core.features.neuralnet.classification.NeuralNet;
import org.vitrivr.cineast.core.features.neuralnet.classification.NeuralNetFactory;
import org.vitrivr.cineast.core.features.neuralnet.label.ConceptReader;
import org.vitrivr.cineast.core.setup.EntityCreator;
import org.vitrivr.cineast.core.util.TimeHelper;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Please use setup as an entrypoint
 */
public class NeuralNetFeature extends AbstractFeatureModule {

    private static final Logger LOGGER = LogManager.getLogger();

    private NeuralNet net;
    private PersistencyWriter<?> classificationWriter;
    private PersistencyWriter<?> classWriter;
    private DBSelector classificationSelector;
    private DBSelector classSelector;
    private static final String fullVectorTableName = "features_neuralnet_fullvector";
    private static final String generatedLabelsTableName = "features_neuralnet_labels";
    private static final String classTableName = "features_neuralnet_classlabels";

    //TODO What is maxDist here?

    /**
     * Careful: This constructor does not initalize the neural net
     */
    protected NeuralNetFeature(float maxDist) {
        super(fullVectorTableName, maxDist);
    }

    protected NeuralNetFeature(float maxDist, NeuralNetFactory factory){
        this(maxDist, factory.get());
    }

    private NeuralNetFeature(float maxDist, NeuralNet net) {
        super(fullVectorTableName, maxDist);
        this.net = net;
    }

    public static String getClassTableName() {
        return classTableName;
    }

    @Override
    public void init(DBSelectorSupplier selectorSupplier){
        super.init(selectorSupplier);
        this.classificationSelector = selectorSupplier.get();
        this.classSelector = selectorSupplier.get();

        this.classificationSelector.open(generatedLabelsTableName);
        this.classSelector.open(classTableName);
    }


    /**
     * Create tables that aren't created by super.
     *
     * Currently Objectid is a string. That is because we get a unique id which has the shape n+....
     * <p>
     * Schema:
     * Table 0: shotid | classification - vector - super
     * Table 1: shotid | objectid | confidence (ex. 4014 | n203843 | 0.4) - generated labels
     *
     * Table 2 is only touched for API-Calls about available labels and at init-time - not during extraction
     * Table 2: objectid | label or concept
     */
    public void init(Supplier<EntityCreator> ecSupplier){
        //TODO Check if entity exists
        EntityCreator ec = ecSupplier.get();
        //TODO Set pk / Create idx -> Logic in the ecCreator
        //TODO Shotid is a string here is that correct?
        AdamGrpc.AttributeDefinitionMessage.Builder attrBuilder = AdamGrpc.AttributeDefinitionMessage.newBuilder();
        ec.createIdEntity(generatedLabelsTableName, new EntityCreator.AttributeDefinition("shotid", AdamGrpc.AttributeType.STRING), new EntityCreator.AttributeDefinition("objectid", AdamGrpc.AttributeType.STRING), new EntityCreator.AttributeDefinition("probability", AdamGrpc.AttributeType.DOUBLE));
        ec.createIdEntity(classTableName, new EntityCreator.AttributeDefinition("objectid", AdamGrpc.AttributeType.STRING), new EntityCreator.AttributeDefinition("label", AdamGrpc.AttributeType.STRING));
    }

    @Override
    public void init(PersistencyWriterSupplier phandlerSupply) {
        super.init(phandlerSupply);
        classificationWriter = phandlerSupply.get();
        classificationWriter.open(generatedLabelsTableName);
        classWriter = phandlerSupply.get();
        classWriter.open(classTableName);
    }

    @Override
    public void finish(){
        super.finish();
        if(this.classificationWriter!= null){
            this.classificationWriter.close();
            this.classificationWriter = null;
        }
        if(this.classWriter!=null){
            this.classWriter.close();
            this.classWriter = null;
        }
        if(this.classificationSelector!=null){
            this.classificationSelector.close();
            this.classificationSelector = null;
        }
        if(this.classSelector!=null){
            this.classSelector.close();
            this.classSelector = null;
        }
    }

    /**
     * TODO This only calls init with the ecSupplier because we expect the other inits to have been called.
     * Creates entities and fills both class and concept-tables.
     */
    public void setup(Supplier<EntityCreator> ecSupplier, String conceptsPath) {
        init(ecSupplier);

        ConceptReader cr = new ConceptReader(conceptsPath);

        //Fill Concept map
        for(Map.Entry<String, String[]> entry : cr.getConceptMap().entrySet()) {
            //values are n... -values being labeled as entry.getKey()
            for(String label : entry.getValue()){
                PersistentTuple tuple = classWriter.generateTuple(label, entry.getKey());
                classWriter.persist(tuple);
            }
        }

        //Fill class names
        for(int i = 0; i<net.getSynSetLabels().length;i++){
            String[] labels = net.getLabels(net.getSynSetLabels()[i]);
            for(String label: labels){
                PersistentTuple tuple = classWriter.generateTuple(net.getSynSetLabels()[i], label);
                classWriter.persist(tuple);
            }
        }
    }

    /**
     * Set neuralNet to specified net if you have called the default constructor
     */
    public void setNeuralNet(NeuralNet net){
        this.net = net;
    }

    /**
     * Classifies an Image with the given neural net
     * @return A float array containing the probabilities given by the neural net.
     */
    private float[] classifyImage(BufferedImage img) {
        return net.classify(img);
    }

    @Override
    public void processShot(SegmentContainer shot) {
        LOGGER.entry();
        TimeHelper.tic();
        //check if shot has been processed
        if (!phandler.idExists(shot.getId())) {
            BufferedImage keyframe = shot.getMostRepresentativeFrame().getImage().getBufferedImage();
            float[] probs = classifyImage(keyframe);

            //Persist best matches
            for (int i = 0; i < probs.length; i++) {
                //TODO Config Dependency
                if (probs[i] > Config.getNeuralNetConfig().getCutoff()) {
                    PersistentTuple tuple = classificationWriter.generateTuple(shot.getId(), net.getSynSetLabels()[i], probs[i]);
                    classificationWriter.persist(tuple);
                }
            }
            persist(shot.getId(), new FloatVectorImpl(probs));
            LOGGER.debug("NeuralNetFeature.processShot() done in {}",
                    TimeHelper.toc());
        }
        LOGGER.exit();
    }

    /**
     *
     * TODO How do we calculate score?
     * Checks if labels have been specified. If no labels have been specified, takes the queryimage.
     * Might perform knn on the 1k-vector in the future.
     * It's also not clear yet if we could combine labels and input image??
     */
    @Override
    public List<StringDoublePair> getSimilar(SegmentContainer sc, QueryConfig qc) {
        LOGGER.entry();
        TimeHelper.tic();
        List<StringDoublePair> _return = null;

        if(!sc.getTags().isEmpty()){
            List<String> wnLabels = new ArrayList();
            for(String label: sc.getTags()){
                LOGGER.debug("Looking for tag: "+label);
                wnLabels = new ArrayList();
                for(Map<String, PrimitiveTypeProvider> row : classSelector.getRows("label", label)){
                    //TODO is this the proper way to get info from the row?
                    wnLabels.add(row.get("label").getString());
                }
            }
            for(String wnLabel : wnLabels){
                for(Map<String, PrimitiveTypeProvider> row : classificationSelector.getRows("objectid", wnLabel)){
                    //TODO Duplicates?
                    LOGGER.debug("Found hit for query: "+row.get("shotid").getString(), row.get("probability").getDouble(), row.get("objectid").toString());
                    _return.add(new StringDoublePair(row.get("shotid").getString(),row.get("probability").getDouble()));
                }
            }
        }else{
            //TODO Can we just take the most representative frame from the sc? Is that the query image?
            float[] res = classifyImage(sc.getMostRepresentativeFrame().getImage().getBufferedImage());

            for(int i = 0; i<res.length; i++){
                //TODO Config dependency. This should be in queryConfig probably
                if(res[i]>Config.getNeuralNetConfig().getCutoff()){
                    //Matching! Wub wub

                    for(Map<String, PrimitiveTypeProvider> row : classificationSelector.getRows("objectid", net.getSynSetLabels()[i])){
                        //TODO Duplicates?
                        LOGGER.debug("Found hit for query: "+row.get("shotid").getString(), row.get("probability").getDouble(), row.get("objectid").toString());
                        _return.add(new StringDoublePair(row.get("shotid").getString(),row.get("probability").getDouble()));
                    }
                }
            }
        }
        //TODO Currently returns mock-result until we get data in the DB
        _return = new ArrayList<StringDoublePair>();
        _return.add(new StringDoublePair("125", 0.5));
        LOGGER.debug("NeuralNetFeature.getSimilar() done in {}",
                TimeHelper.toc());
        return LOGGER.exit(_return);
    }
}
