package ch.unibas.cs.dbis.cineast.core.db;

import java.util.HashMap;

import com.google.common.base.Joiner;

import ch.unibas.cs.dbis.cineast.core.data.FloatArrayIterable;
import ch.unibas.cs.dbis.cineast.core.data.ReadableFloatVector;
import ch.unibas.dmi.dbis.adam.http.Grpc;
import ch.unibas.dmi.dbis.adam.http.Grpc.InsertMessage.TupleInsertMessage;
import ch.unibas.dmi.dbis.adam.http.Grpc.InsertMessage.TupleInsertMessage.Builder;

public abstract class ProtobufTupleGenerator implements PersistencyWriter<TupleInsertMessage> {

	protected String[] names; 
	private static final Builder builder = Grpc.InsertMessage.TupleInsertMessage.newBuilder();
	private Joiner joiner = Joiner.on(',');
	
	
	protected ProtobufTupleGenerator(String...names){
		this.names = names;
	}
	
	protected ProtobufTupleGenerator(){
		this("id", "feature");
	}

	@Override
	public PersistentTuple<TupleInsertMessage> generateTuple(Object... objects) {
		return new ProtobufTuple(objects);
	}

	public void setFieldNames(String...names){
		if(names != null && names.length > 0){
			this.names = names;
		}
	}
	
	class ProtobufTuple extends PersistentTuple<TupleInsertMessage>{

		ProtobufTuple(Object...objects){
			super(objects);
		}
		
		@Override
		public TupleInsertMessage getPersistentRepresentation() {
			synchronized (builder) {
				builder.clear();			
				HashMap<String, String> tmpMap = new HashMap<>();
				int nameIndex = 0;
				
				for(Object o : this.elements){
					if(o instanceof ReadableFloatVector){
						ReadableFloatVector fv = (ReadableFloatVector) o;
						tmpMap.put(names[nameIndex++], joiner.join(fv.toList(null)));
					}else if(o instanceof float[]){
						float[] vector = (float[]) o;
						tmpMap.put(names[nameIndex++], joiner.join(new FloatArrayIterable(vector)));
					}else{
						if(nameIndex >= names.length){
							continue;
						}
						tmpMap.put(names[nameIndex++], o.toString());
					}
				}
				return builder.putAllData(tmpMap).build();
			}
		}
		
	}
	
}
