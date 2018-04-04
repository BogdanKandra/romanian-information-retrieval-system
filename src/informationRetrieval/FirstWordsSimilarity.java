package informationRetrieval;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.util.BytesRef;

/**
 * A custom similarity measure used for boosting the score of documents 
 * containing the search term among the first 10 words of the contents field.
 * @version 1.0
 * @author Bogdan
 * @see ClassicSimilarity
 */
public class FirstWordsSimilarity extends ClassicSimilarity {
	
	@Override
	public float coord(int overlap, int maxOverlap) {
		// Used for boosting score based on the number of overlapping terms in the query and document
		// Example: if (overlap == 1) return 10; -> boosts single-term matchings
		return super.coord(overlap, maxOverlap);
	}
	
	@Override
	public float idf(long docFreq, long docCount) {
		// Used for boosting score based on the number of docs which contain the term and the total number of docs
		// Example: if (docFreq <= 2) return super.idf(docFreq * 100, docCount); -> penalizes docs containing rare terms
		return super.idf(docFreq, docCount);
	}
	
	@Override
	public float lengthNorm(FieldInvertState state) {
		// Used for boosting score based on current field properties
		// Example: if (state.getLength() % 2 == 1) return super.lengthNorm(state) * 100; -> boosts docs which have the Field value as odd number
		return super.lengthNorm(state);
	}
	
	@Override
	public float queryNorm(float sumOfSquaredWeights) {
		// Used for boosting the score of all results of a query
		return super.queryNorm(sumOfSquaredWeights);
	}
	
	@Override
	public float scorePayload(int doc, int start, int end, BytesRef payload) {
		// Used for boosting or hindering score by modifying the payload of terms
		float val = PayloadHelper.decodeFloat(payload.bytes); // It's 1, as we defined it in PayloadFilter
		if(start >= 0 && start <= 9) // boost docs when the term is among the first 10 words of contents field
			return val * 3f;
		return super.scorePayload(doc, start, end, payload);
	}
	
	@Override
	public float sloppyFreq(int distance) {
		return super.sloppyFreq(distance);
	}
	
	@Override
	public float tf(float freq) {
		// Used for boosting score based on the number of term occurences within the document
		// Example: if (freq > 1f) return super.tf(freq) * 100; -> boosts docs containing the term more than once
		return super.tf(freq);
	}
}
