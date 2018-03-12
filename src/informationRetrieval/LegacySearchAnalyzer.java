package informationRetrieval;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import static informationRetrieval.LuceneUtils.*;

public class LegacySearchAnalyzer extends Analyzer {

	private String queryString;
	
	public LegacySearchAnalyzer(String userQuery){
		queryString = userQuery;
	}
	
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(String fieldName){
		
		Tokenizer tokenizer = new StandardTokenizer();
		tokenizer.setReader(new StringReader(queryString));
		TokenStream resultFinal = null;
		
		StringBuilder sb = new StringBuilder();
		CharTermAttribute token = tokenizer.getAttribute(CharTermAttribute.class);
		
		try{
			tokenizer.reset();

			while(tokenizer.incrementToken()) {
				if(sb.length() > 0) {	
					sb.append(" ");
				}
				sb.append(token.toString());
			}
			
			tokenizer.end();
			tokenizer.close();
		} catch (IOException e){
			e.printStackTrace();
		}
		
		String result = sb.toString();
		result = result.toLowerCase();
		result = removeDiacritics(result);
		
		Tokenizer tokenizer2 = new StandardTokenizer();
		tokenizer2.setReader(new StringReader(result));
		
		try {
			tokenizer2.reset();
			tokenizer2.end();
			tokenizer2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		resultFinal = new LowerCaseFilter(tokenizer2);
		resultFinal = new StopFilter(resultFinal, STOPWORDS);    // Remove stopwords
		resultFinal = new SnowballFilter(resultFinal, LANGUAGE); // Perform stemming
		
		return new TokenStreamComponents(tokenizer, resultFinal);
	}
}
