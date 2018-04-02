package informationRetrieval;

import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

import static informationRetrieval.LuceneUtils.*;

/**
 * Performs the analysis of the user's search query.
 * <p> Takes the user query string, tokenizes it, converts it to lowercase, removes topwords and stems it. </p>
 * @version 2.5
 * @author Bogdan
 * @see Analyzer
 */
public class SearchAnalyzer extends Analyzer{

	private String queryString;
	
	public SearchAnalyzer(String userQuery){
		queryString = userQuery;
	}
	
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(String fieldName){
		
		Tokenizer tokenizer = new StandardTokenizer();
		tokenizer.setReader(new StringReader(queryString));
		TokenStream tokens = null;
		
		tokens = new LowerCaseFilter(tokenizer);
		tokens = new StopFilter(tokens, STOPWORDS);    // Remove stopwords
		tokens = new SnowballFilter(tokens, LANGUAGE); // Perform stemming
		
		return new TokenStreamComponents(tokenizer, tokens);
	}
}
