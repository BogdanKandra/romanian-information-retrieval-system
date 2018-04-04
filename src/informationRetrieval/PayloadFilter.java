package informationRetrieval;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

/**
 * A token filter attributing a payload equal to 1 to each token.
 * The payload will be used as a basis for score boosting in the custom similarity class.
 * @version 1.0
 * @author Bogdan
 * @see TokenFilter
 */
public class PayloadFilter extends TokenFilter {

	// First, attributes relevant to the filtering have to be declared
	PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);    // Payload of a Token - used for influencing scoring
	CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class); // The term text of a Token - not used, but put for illustration purposes
	
	protected PayloadFilter(TokenStream input) {
		super(input);
	}
	
	// Computes the payload of a term
	protected float determinePayload(String term) {
		
		float score = 1f;
		// Here I could put extra logic for boosting the payload of certain terms, if they fulfill certain conditions
		// For example if term contains vowels, or is of certain length, boost its score (score += 0.2f)
		return score;
	}

	@Override
	public boolean incrementToken() throws IOException {
		if(!(input.incrementToken()))
			return false;
		else {
			payloadAtt.setPayload(new BytesRef(PayloadHelper.encodeFloat(determinePayload(charTermAtt.toString()))));
			return true;
		}
	}
	
	
}
