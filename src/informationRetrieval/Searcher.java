package informationRetrieval;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static informationRetrieval.LuceneUtils.*;

/**
 * The <i>Searcher</i> class.
 * <p> Takes an index directory and a user query string replacing diacritics. <p>
 * <p> <b>TODO:</b> Make the search work for file titles as well and use multiple queries. </p>
 * @version 2.0
 * @author Bogdan
 */
public class Searcher{
	
	private IndexSearcher searcher;
	private QueryParser queryParser;
	private Query query;
	private String queryString;
	
	public String getQueryString() {
		return queryString;
	}

	public void setQueryString(String qs) {
		this.queryString = qs;
	}

	@SuppressWarnings("resource")
	public Searcher(String indexDirPath, String userQuery) throws IOException{
		
		Directory indexDirectory = FSDirectory.open(new File(indexDirPath).toPath());
		IndexReader indexReader = DirectoryReader.open(indexDirectory);
		searcher = new IndexSearcher(indexReader);
		
		queryString = userQuery;
		queryString = removeDiacritics(queryString);
		
		SearchAnalyzer analyzer = new SearchAnalyzer(queryString);
		queryParser = new QueryParser(CONTENTS, analyzer);
	}
	
	// Performs the search and returns the specified number of results
	public TopDocs search(String searchQuery) throws IOException, ParseException{
		
		query = queryParser.parse(searchQuery);
		return searcher.search(query, MAX_HITS);
	}
	
	// Returns the id of the Document having the given score
	public Document getDocument(ScoreDoc scoreDoc) throws CorruptIndexException, IOException{
		
		return searcher.doc(scoreDoc.doc);
	}
}
