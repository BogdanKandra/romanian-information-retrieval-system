package informationRetrieval;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static informationRetrieval.LuceneUtils.*;

/**
 * The <i>Searcher</i> class.
 * <p> Takes an index directory and a user query string replacing diacritics. <p>
 * @version 2.5
 * @author Bogdan
 */
public class Searcher{
	
	private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());
	private IndexSearcher searcher;
	private String queryString;
	
	public String getQueryString() {
		return queryString;
	}
	
	public void setQueryString(String qs) {
		queryString = qs;
	}

	/**
	 * The constructor takes the index directory path and the user query, then normalizes the queries' components.
	 * @param indexDirPath - the path of the index directory
	 * @param userQuery - the user query, in the form 'content|extension|last_modified_date'
	 * @throws IOException
	 */
	@SuppressWarnings("resource")
	public Searcher(String indexDirPath, String userQuery) throws IOException {
		
		Directory indexDirectory = FSDirectory.open(new File(indexDirPath).toPath());
		IndexReader indexReader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(indexReader);
		searcher.setSimilarity(new FirstWordsSimilarity());  // Use our custom scoring class - delete if standard scoring is desired
		
		String[] components = userQuery.split("\\|");
		String contents = components[0], extension = components[1], date = components[2];
		Date tempDate = new Date();  // Initialise with current date
		
		if(!(date.equals("#"))) {  // Convert the date to GMT timezone and Lucene format
			
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			
			try {
				tempDate = sdf.parse(date);
			} catch (java.text.ParseException e) {  // If the date format is not correct, keep the current date
				LOGGER.log(Level.SEVERE, e.toString(), e);
				try {
					tempDate = sdf.parse(tempDate.toString());
				} catch (java.text.ParseException e1) { /// Can't get here, current date is formatted correctly (?)
					LOGGER.log(Level.SEVERE, e1.toString(), e1);
				}
			}
			
			date = DateTools.dateToString(tempDate, Resolution.DAY);
		}
		if(!(contents.equals("#")))
			contents = removeDiacritics(contents);
		
		queryString = contents + "|" + extension + "|" + date;		
	}
	
	// Performs the search and returns the specified number of results
	// TODO -- Add logic for other fields to the query string
	public TopDocs search(String searchQuery) throws IOException, ParseException{
		
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		String[] queries = searchQuery.split("\\|");
		
		if(!(queries[0].equals("#"))) {  // The user searches by contents
			
			QueryParser contentsQP = new QueryParser(CONTENTS, new SearchAnalyzer(queries[0]));
			Query contentsQuery = contentsQP.parse(queries[0]);
			builder.add(contentsQuery, Occur.MUST);
		}
		
		if(!(queries[1].equals("#"))) {  // The user searches by extension
			
			QueryParser extensionQP = new QueryParser(FILE_EXTENSION, new KeywordAnalyzer());
			Query extensionQuery = extensionQP.parse(queries[1]);
			builder.add(extensionQuery, Occur.MUST);
		}
		
		if(!(queries[2].equals("#"))) {  // The user searches by last modified date
			
			String lowerDate = queries[2];
			String upperDate = DateTools.dateToString(new Date(), Resolution.DAY);  // Current date
			boolean includeLower = true;
			boolean includeUpper = true;
			
			TermRangeQuery dateQuery = TermRangeQuery.newStringRange(LAST_MODIFIED, lowerDate, upperDate, includeLower, includeUpper);
			builder.add(dateQuery, Occur.MUST);
		}

		BooleanQuery query = builder.build();
		
		return searcher.search(query, MAX_HITS);
	}
	
	// Returns the id of the Document having the given score
	public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException{
		
		return searcher.doc(scoreDoc.doc);
	}
}
