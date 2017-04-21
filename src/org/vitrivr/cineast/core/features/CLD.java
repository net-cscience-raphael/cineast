package org.vitrivr.cineast.core.features;

import java.util.List;
import org.vitrivr.cineast.core.config.ReadableQueryConfig;
import org.vitrivr.cineast.core.data.FloatVector;
import org.vitrivr.cineast.core.data.score.ScoreElement;
import org.vitrivr.cineast.core.data.segments.SegmentContainer;
import org.vitrivr.cineast.core.features.abstracts.AbstractFeatureModule;
import org.vitrivr.cineast.core.util.ColorLayoutDescriptor;

public class CLD extends AbstractFeatureModule {

	public CLD(){
		super("features_CLD", 1960f / 4f);
	}
	

	@Override
	public void processShot(SegmentContainer shot) {
		if(!phandler.idExists(shot.getId())){
			FloatVector fv = ColorLayoutDescriptor.calculateCLD(shot.getMostRepresentativeFrame().getImage());
			persist(shot.getId(), fv);
		}
	}

	@Override
	public List<ScoreElement> getSimilar(SegmentContainer sc, ReadableQueryConfig qc) {
		FloatVector query = ColorLayoutDescriptor.calculateCLD(sc.getMostRepresentativeFrame().getImage());
		return getSimilar(query.toArray(null), qc);
	}

}
