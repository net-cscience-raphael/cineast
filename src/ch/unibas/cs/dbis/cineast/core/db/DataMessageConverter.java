package ch.unibas.cs.dbis.cineast.core.db;

import java.util.List;

import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.BooleanProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.DoubleProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.FloatArrayProvider;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.FloatArrayProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.FloatProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.IntArrayProvider;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.IntArrayProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.IntProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.LongProviderImpl;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.PrimitiveTypeProvider;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.ProviderDataType;
import ch.unibas.cs.dbis.cineast.core.data.providers.primitive.StringProviderImpl;
import ch.unibas.dmi.dbis.adam.http.Adam.DataMessage;
import ch.unibas.dmi.dbis.adam.http.Adam.DenseVectorMessage;
import ch.unibas.dmi.dbis.adam.http.Adam.FeatureVectorMessage;
import ch.unibas.dmi.dbis.adam.http.Adam.IntVectorMessage;
import ch.unibas.dmi.dbis.adam.http.Adam.SparseVectorMessage;

public final class DataMessageConverter {

	private DataMessageConverter(){}
	
	public static final PrimitiveTypeProvider convert(DataMessage message){
		switch(message.getDatatypeCase()){
		case BOOLEANDATA:
			return new BooleanTypeProvider(message.getBooleanData());
		case DOUBLEDATA:
			return new DoubleTypeProvider(message.getDoubleData());
		case FEATUREDATA:
			FeatureVectorMessage featureVectorMessage = message.getFeatureData();
			switch(featureVectorMessage.getFeatureCase()){
			case DENSEVECTOR:
				return new FloatVectorProvider(featureVectorMessage.getDenseVector());
			case INTVECTOR:
				return new IntVectorProvidre(featureVectorMessage.getIntVector());
			case SPARSEVECTOR:
				return new FloatVectorProvider(featureVectorMessage.getSparseVector());
			case FEATURE_NOT_SET:
			default:
				return new NothingProvider();
			}
		case FLOATDATA:
			return new FloatTypeProvider(message.getFloatData());
		case INTDATA:
			return new IntTypeProvider(message.getIntData());
		case LONGDATA:
			return new LongTypeProvider(message.getLongData());
		case STRINGDATA:
			return new StringTypeProvider(message.getStringData());
		case DATATYPE_NOT_SET:
		default:
			return new NothingProvider();
		
		}
	}
	
	private static class BooleanTypeProvider extends BooleanProviderImpl implements PrimitiveTypeProvider{

		BooleanTypeProvider(boolean value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.BOOLEAN;
		}
		
	}
	
	private static class DoubleTypeProvider extends DoubleProviderImpl implements PrimitiveTypeProvider{

		DoubleTypeProvider(double value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.DOUBLE;
		}
		
	}
	
	private static class FloatTypeProvider extends FloatProviderImpl implements PrimitiveTypeProvider{

		FloatTypeProvider(float value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.FLOAT;
		}
		
	}
	
	private static class IntTypeProvider extends IntProviderImpl implements PrimitiveTypeProvider{

		IntTypeProvider(int value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.INT;
		}
		
	}
	
	private static class LongTypeProvider extends LongProviderImpl implements PrimitiveTypeProvider{

		LongTypeProvider(long value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.LONG;
		}
		
	}
	
	private static class StringTypeProvider extends StringProviderImpl implements PrimitiveTypeProvider{

		StringTypeProvider(String value) {
			super(value);
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.STRING;
		}
		
	}
	
	private static class NothingProvider implements PrimitiveTypeProvider{

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.UNKNOWN;
		}
		
	}
	
	private static class FloatVectorProvider extends FloatArrayProviderImpl implements PrimitiveTypeProvider{

		private static float[] convert(DenseVectorMessage message){
			List<Float> floatlist = message.getVectorList();
			if(floatlist == null || floatlist.isEmpty()){
				return FloatArrayProvider.DEFAULT_FLOAT_ARRAY;
			}
			float[] _return = new float[floatlist.size()];
			int i = 0;
			for(float f : floatlist){
				_return[i++] = f;
			}
			return _return;
		}
		
		private static float[] convert(SparseVectorMessage message) {
			List<Integer> indexList = message.getPositionList();
			if(indexList == null || indexList.isEmpty()){
				return FloatArrayProvider.DEFAULT_FLOAT_ARRAY;
			}
			
			int maxIndex = 0;
			for(int i : indexList){
				maxIndex = maxIndex < i ? i : maxIndex;
			}
			
			float[] _return = new float[maxIndex + 1];
			
			List<Float> valueList = message.getVectorList();
			
			for(int i = 0; i < indexList.size(); ++i){
				_return[indexList.get(i)] = valueList.get(i);
			}
			
			return _return;
		}

		FloatVectorProvider(DenseVectorMessage message) {
			super(convert(message));
		}
		
		FloatVectorProvider(SparseVectorMessage message) {
			super(convert(message));
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.FLOAT_ARRAY;
		}
		
	}
	
	private static class IntVectorProvidre extends IntArrayProviderImpl implements PrimitiveTypeProvider{

		private static int[] convert(IntVectorMessage message) {
			List<Integer> intList = message.getVectorList();
			if(intList == null || intList.isEmpty()){
				return IntArrayProvider.DEFAULT_INT_ARRAY;
			}
			int[] _return = new int[intList.size()];
			int i = 0;
			for(int j : intList){
				_return[i++] = j;
			}
			return _return;
		}

		IntVectorProvidre(IntVectorMessage message) {
			super(convert(message));
		}

		@Override
		public ProviderDataType getType() {
			return ProviderDataType.INT_ARRAY;
		}
		
	}
	
}

