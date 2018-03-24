package informationRetrieval;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static informationRetrieval.LuceneUtils.*;

/**
 * The <i>Searcher</i> class.
 * <p> Takes an index directory and a user query string replacing diacritics. <p>
 * <p> <b>TODO:</b> Use multiple queries </p>
 * @version 2.0
 * @author Bogdan
 */
public class Searcher{
	
	private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());
	private IndexSearcher searcher;
	private QueryParser queryParser;
	private String queryString;
	
	public String getQueryString() {
		return queryString;
	}
	
	public void setQueryString(String qs) {
		queryString = qs;
	}

	@SuppressWarnings("resource")
	public Searcher(String indexDirPath, String userQuery, String field) throws IOException{
		
		Directory indexDirectory = FSDirectory.open(new File(indexDirPath).toPath());
		IndexReader indexReader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(indexReader);
		
		queryString = userQuery;
		Analyzer analyzer;
		Date tempDate = new Date(); // Initialize with current date
		
		if(field.equals(CONTENTS)) {  // Query string must be processed by our SearchAnalyzer
			queryString = removeDiacritics(queryString); /// Move this in setter for queryString
			analyzer = new SearchAnalyzer(queryString); /// Tokenizes, stems and lower cases the query string
		}
		else if((field.equals(LAST_MODIFIED)) || (field.equals(CREATED_AT))) {  // Query string is a date
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				tempDate = sdf.parse(queryString);
			} catch (java.text.ParseException e) { // If the date format is not correct, keep the current date
				LOGGER.log(Level.SEVERE, e.toString(), e);
			}
			queryString = DateTools.dateToString(tempDate, Resolution.DAY);
			analyzer = new KeywordAnalyzer();
		}
		else if(field.equals(FILE_EXTENSION_DATE)) {  // Searching by both extension AND date

			String[] queries = userQuery.split("#");
			
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
				sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
				tempDate = sdf.parse(queries[1]);
			} catch (java.text.ParseException e) { // If the date format is not correct, keep the current date
				LOGGER.log(Level.SEVERE, e.toString(), e);
			}
			queryString = queries[0] + "#" + DateTools.dateToString(tempDate, Resolution.DAY);
			analyzer = new KeywordAnalyzer();
		}
		else {  // Query string must keep its initial form
			analyzer = new KeywordAnalyzer();
		}
			queryParser = new QueryParser(field, analyzer); // Will search in the given field, using the defined analyzer
	}
	
	// Performs the search and returns the specified number of results
	public TopDocs search(String searchQuery) throws IOException, ParseException{
		
		Query query = queryParser.parse(searchQuery);
		return searcher.search(query, MAX_HITS);
	}
	
	public TopDocs searchDate(String queryDate, String field) throws IOException {
		
		String lowerDate = queryDate;
		String upperDate = DateTools.dateToString(new Date(), Resolution.DAY);  // Current date
		boolean includeLower = true;
		boolean includeUpper = true;
		
		TermRangeQuery query = TermRangeQuery.newStringRange(field, lowerDate, upperDate, includeLower, includeUpper);
		return searcher.search(query, MAX_HITS);
	}
	
	public TopDocs searchExtensionDate(String searchQuery) throws IOException {
		
		String[] queries = searchQuery.split("#");
		String lowerDate = queries[1];
		String upperDate = DateTools.dateToString(new Date(), Resolution.DAY);  // Current date
		boolean includeLower = true;
		boolean includeUpper = true;
		
		Query extensionQuery = new TermQuery(new Term(FILE_EXTENSION, queries[0]));
		TermRangeQuery dateQuery = TermRangeQuery.newStringRange(LAST_MODIFIED, lowerDate, upperDate, includeLower, includeUpper);
		
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		builder.add(extensionQuery, Occur.MUST);
		builder.add(dateQuery, Occur.MUST);
		BooleanQuery query = builder.build();
		
		return searcher.search(query, MAX_HITS);
	}
	
	// Returns the id of the Document having the given score
	public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException{
		
		return searcher.doc(scoreDoc.doc);
	}
}
