package informationRetrieval;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import static informationRetrieval.LuceneUtils.*; // Static import allows for static members to be referenced without qualification

/**
 * Driver class for launching the <i> Information Retrieval System </i>.
 * <p> Takes an index directory and a data directory. <p>
 * <p> <b>TODO:</b> Make a graphic interface to input the directory paths, choose the files to be indexed 
 * and a query string and display the search results to the user.
 * Maybe, make Indexer and Searcher Singleton classes???
 * @version 2.0
 * @author Bogdan
 */
public class Launcher {

	private String indexDir = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\index";
	private String dataDir  = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\data";
	private static String queryPath= "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queries.txt";
	private Indexer indexer;
	private Searcher searcher;
	
	public String getIndexDir() {
		return indexDir;
	}

	public void setIndexDir(String indexDir) {
		this.indexDir = indexDir;
	}

	public String getDataDir() {
		return dataDir;
	}
	
	public void setDataDir(String dataDir) {
		this.dataDir = dataDir;
	}
	
	// Creates the Index, filtering the documents from the data folder according to the specified filter
	private void index() throws IOException{
		
		indexer = new Indexer(indexDir);
		int numIndexed;
		long startTime = System.currentTimeMillis();
		// Effectively create the Index, specifying the filter to be applied to the documents
		numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
		long endTime = System.currentTimeMillis();
		indexer.close();
		
		if(numIndexed == 1)
			System.out.println(1 + " file indexed; time elapsed: " + (endTime - startTime) + " ms");
		else
			System.out.println(numIndexed + " files indexed; time elapsed: " + (endTime - startTime) + " ms");
	}
	
	// Performs the Search using the query string provided
	private void search(String searchQuery) throws IOException, ParseException{
		
		searcher = new Searcher(indexDir, searchQuery);
		long startTime = System.currentTimeMillis();
		TopDocs docs = searcher.search(searcher.getQueryString());
		long endTime = System.currentTimeMillis();
		
		if(docs.totalHits == 1){
			System.out.println("==================================================");
			System.out.println("QUERY: " + searchQuery);
			System.out.println(1 + " document found; time elapsed: " + (endTime - startTime) + " ms\n");
		}
		else{
			System.out.println("==================================================");
			System.out.println("QUERY: " + searchQuery);
			System.out.println(docs.totalHits + " documents found; time elapsed: " + (endTime - startTime) + " ms\n");
		}
		
		// Print the name of the file results, in order of their score
		ScoreDoc[] hits = docs.scoreDocs;
		for(ScoreDoc scoreDoc : hits){
			
			Document doc = searcher.getDocument(scoreDoc);
			System.out.println("File: " + doc.get(FILE_NAME) + "  (Path: " + doc.get(FILE_PATH) + ")");
		}
		System.out.println("==================================================\n");
	}
	
	private void search(Scanner in){
		
		String query;
		
		while(in.hasNextLine()){
			
			query = in.nextLine();
			
			try {
				search(query);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args){
		
		Launcher launcher = null;
		
		try{
			launcher = new Launcher();
//			launcher.index();
			File queryFile = new File(queryPath);
			FileInputStream fis = new FileInputStream(queryFile);
			Scanner in = new Scanner(fis);
			launcher.search(in);
			in.close();
			fis.close();
		} catch(IOException e){
			e.printStackTrace();
		}
	}
}
