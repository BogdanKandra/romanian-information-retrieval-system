package informationRetrieval;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Constants used in the project.
 * <p> <b>TODO:</b> Get the stopword file and max hits as input in graphic interface (getter and setter) </p>
 * @version 2.7
 * @author Bogdan
 */
public final class LuceneUtils{
	
	private static final File stopwordFile = new File("F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\stopwords-ro.txt");
	public static final String CONTENTS = "contents";
	public static final String FILE_NAME = "filename";
	public static final String FILE_PATH = "filepath";
	public static final String FILE_EXTENSION = "extension";
	public static final String LAST_MODIFIED = "modifiedDate";
	public static final String CREATED_AT = "creationDate";
	public static final String LANGUAGE = "Romanian";
	public static final int MAX_HITS = 10;
	public static final CharArraySet STOPWORDS = getStopWords(stopwordFile);
	
	// Make the constructor private, as to prevent the creation of an object of this type
	private LuceneUtils() {
		throw new AssertionError();
	}
	
	public static String removeDiacritics(String s) {
		
		s = s.replaceAll("[ăâ]", "a");
		s = s.replaceAll("[ș]", "s");
		s = s.replaceAll("[ț]", "t");
		s = s.replaceAll("[î]", "i");
		
		return s;
	}

	static final TokenStream getTokenStream(String contents) throws IOException{
		
		Tokenizer tokenizer = new StandardTokenizer();
		tokenizer.setReader(new StringReader(contents));
		StringBuilder sb = new StringBuilder();
		TokenStream resultFinal = null;
		
		CharTermAttribute token = tokenizer.getAttribute(CharTermAttribute.class);
		
		tokenizer.reset();
		while(tokenizer.incrementToken()){
			
			if(sb.length() > 0){
				
				sb.append(" ");
			}
			sb.append(token.toString());
		}
		
		tokenizer.end();
		tokenizer.close();
		
		String result = sb.toString();
		result = result.toLowerCase();
		result = removeDiacritics(result);
		
		Tokenizer tokenizer2 = new StandardTokenizer();
		tokenizer2.setReader(new StringReader(result));
		
		resultFinal = new LowerCaseFilter(tokenizer2);
		resultFinal = new StopFilter(resultFinal, STOPWORDS);     // Remove stopwords
		resultFinal = new SnowballFilter(resultFinal, LANGUAGE);  // Perform stemming
		resultFinal = new PayloadFilter(resultFinal);             // Attaches a payload to each token; should delete if payload is not desired
		
		return resultFinal;
	}
	
	/**
	 * @param file - the file to get stopwords from. The file must contain a stopword on each row
	 * @return the stopwords contained in the file, as a CharArraySet
	 */
	static final CharArraySet getStopWords(File file){
		
		String contents = "";
		
		try {
			contents = FileUtils.readFileToString(file, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		contents = removeDiacritics(contents);
		
		List<String> listaCuvinte = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		
		for(int i = 0; i < contents.length(); i++){
			
			if(contents.charAt(i) == '\n'){
				
				listaCuvinte.add(sb.toString());
				sb = new StringBuilder();
			} else{
				
				sb.append(contents.charAt(i));
			}
		}
		
		return new CharArraySet(listaCuvinte, true);
	}
}
