package informationRetrieval;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final Logger LOGGER = Logger.getLogger(Launcher.class.getName());
	private static final String queryFileContents = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queriesContents.txt";
	private static final String queryFileExtension = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queriesExtension.txt";
	private static final String queryFileModifiedDate = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queriesModifiedDate.txt";
	private static final String queryFileName = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queriesName.txt";
	private static final String queryFileExtensionDate = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queriesExtensionDate.txt";
	private static final String queries = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\queryFiles\\queries.txt";
	
	private String indexDir = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\index";
	private String dataDir  = "F:\\Programare\\Java\\Eclipse\\InformationRetrieval\\data";

	private Indexer indexer;
	private Searcher searcher;
	private static boolean flag = true;
	
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
	private void index(String indexOpenMode) throws IOException{
		
		indexer = new Indexer(indexDir, indexOpenMode);
		int numIndexed;
		long startTime = System.currentTimeMillis();
		// Effectively create the Index, specifying the filter to be applied to the documents
		numIndexed = indexer.createIndex(dataDir, new TextFileFilter());
		long endTime = System.currentTimeMillis();
		indexer.close();
		
		if(numIndexed == 1)
			System.out.println("1 file indexed; time elapsed: " + (endTime - startTime) + " ms\n");
		else
			System.out.println(numIndexed + " files indexed; time elapsed: " + (endTime - startTime) + " ms\n");
	}
	
	// Performs the Search using the query string provided, on the field given
	private void search(String searchQuery, String field) throws IOException, ParseException{
		/// I want a single instance of Searcher (do not create the object for each search)

		searcher = new Searcher(indexDir, searchQuery, field);
		TopDocs docs = null;
		long startTime = 0;
		long endTime = 0;
		
		if((field.equals(LAST_MODIFIED)) || (field.equals(CREATED_AT))) {

			startTime = System.currentTimeMillis();
			docs = searcher.searchDate(searcher.getQueryString(), field);  // Should catch IOException
			endTime = System.currentTimeMillis();
		}
		else if(field.equals(FILE_EXTENSION_DATE)) {
			
			startTime = System.currentTimeMillis();
			docs = searcher.searchExtensionDate(searcher.getQueryString());
			endTime = System.currentTimeMillis();
		}
		else if(field.equals(FILE_CONTENTS_EXTENSION_DATE)) {
			
			startTime = System.currentTimeMillis();
			docs = searcher.searchContentsExtensionDate(searcher.getQueryString());
			endTime = System.currentTimeMillis();
		}
		else {  // Searching for anything else

			startTime = System.currentTimeMillis();
			docs = searcher.search(searcher.getQueryString());
			endTime = System.currentTimeMillis();
		}
		
		if(docs.totalHits == 1) {
			System.out.println("==================================================");
			System.out.println("QUERY: " + searchQuery);
			System.out.println(1 + " document found; time elapsed: " + (endTime - startTime) + " ms\n");
		}
		else {
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
	
	// Performs the search using the query string provided (in the form 'contents|extension|date', any of them possibly being empty strings)
	private void search(String queryString) throws IOException, ParseException {
		
		searcher = new Searcher(indexDir, queryString);
		TopDocs docs = null;
		long startTime = 0;
		long endTime = 0;
		
		startTime = System.currentTimeMillis();
		docs = searcher.search(searcher.getQueryString()); // ???
		endTime = System.currentTimeMillis();
		
		if(docs.totalHits == 1) {
			System.out.println("==================================================");
			System.out.println("QUERY: " + queryString);
			System.out.println("1 document found; time elapsed: " + (endTime - startTime) + " ms\n");
		}
		else {
			System.out.println("==================================================");
			System.out.println("QUERY: " + queryString);
			System.out.println(docs.totalHits + " documents found; time elapsed: " + (endTime - startTime) + " ms\n");
		}
		
		// Print the name of the file results (if there are any), in order of their score
		if(!(docs.totalHits == 0)) {
			ScoreDoc[] hits = docs.scoreDocs;
			for(ScoreDoc scoreDoc : hits){
				
				Document doc = searcher.getDocument(scoreDoc);
				System.out.println("File: " + doc.get(FILE_NAME) + "  (Path: " + doc.get(FILE_PATH) + ")");
			}
			System.out.println("==================================================\n");
		}
	}
	
	// Reads queries from a text file and passes them to the search method
	// TODO: Cleanup here
	private void search(Scanner in, String field){
		
		if(field.equals(FILE_EXTENSION_DATE)) {
			
			String query, extension, date;
			
			while(in.hasNextLine()){
				
				extension = in.nextLine();
				date = in.nextLine();
				query = extension + "#" + date;
				
				try {
					search(query, field);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		else if(field.equals(FILE_CONTENTS_EXTENSION_DATE)) {
			
			String query, contents, extension, date;
			
			while(in.hasNextLine()) {
				
				contents = in.nextLine();
				extension = in.nextLine();
				date = in.nextLine();
				query = contents + "#" + extension + "#" + date;
				
				try {
					search(query, field);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		else {
			
			String query;
			
			while(in.hasNextLine()){
				
				query = in.nextLine();
				
				try {
					search(query, field);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	// Reads queries from the generic queries file, processes them and passes them to the search method
	private void search(Scanner in) {
		
		String query, contents, extension, date;
		String[] components;
		
		while(in.hasNextLine()) {
			
			contents = "#";
			extension = "#";
			date = "#";
			
			query = in.nextLine();
			components = query.split("\\|");
			
			for(String component : components) {
				
				component = component.trim();
				if(component.startsWith("."))    // Component must be extension
					extension = component;
				else if(component.contains(".")) // Component must be date
					date = component;
				else if(!(component.equals(""))) // Component must be contents
					contents = component;
			}
			
			if(!(extension.equals("#")))  // Remove the . (dot) from the extension
					extension = extension.substring(1);
			
			query = contents + "|" + extension + "|" + date;
			
			try {
				search(query);
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.toString(), e);
			} catch (ParseException e) {
				LOGGER.log(Level.SEVERE, e.toString(), e);
			}
		}
	}
		
	private void searchMenu(Scanner in) {
		
		System.out.println("What criterion do you want to search by?");
		System.out.println("1. Contents");
		System.out.println("2. Extension");
		System.out.println("3. Last modified date");
		System.out.println("4. Extension AND Last modified date");
		System.out.println("5. Name");
		System.out.println("6. Generic Search");
		
		int option;
		File queryFile = null;
		FileInputStream fis = null;
		Scanner fileIn = null;
		
		try{
			System.out.print("Enter option: ");
			option = in.nextInt();
			
			if((option < 1) || (option > 6)) {
				throw new InputMismatchException();
			}
			
			if(option == 1) {  // Search by contents

				queryFile = new File(queryFileContents);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn, CONTENTS);
			}
			else if(option == 2) {  // Search by extension

				queryFile = new File(queryFileExtension);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn, FILE_EXTENSION);
			}
			else if(option == 3) {  // Search by last modified date

				queryFile = new File(queryFileModifiedDate);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn, LAST_MODIFIED);
			}
			else if(option == 4) {  // Search by extension AND last modified date
				
				queryFile = new File(queryFileExtensionDate);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn, FILE_EXTENSION_DATE);
			}
			else if(option == 5) {  // Search by file name

				queryFile = new File(queryFileName);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn, FILE_NAME);
			}
			else {  // Perform a generic search
				
				queryFile = new File(queries);
				fis = new FileInputStream(queryFile);
				fileIn = new Scanner(fis);
				
				search(fileIn);
			}
		} catch (InputMismatchException ex){
			System.out.println("You have to enter a number between 1 and 4 !");
			in.nextLine();
			searchMenu(in);
		} catch (FileNotFoundException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		} finally {
			if(!(fileIn == null))
				fileIn.close();
			if(!(fis == null))
				try {
					fis.close();
				} catch (IOException e) {
					LOGGER.log(Level.SEVERE, e.toString(), e);
				}
		}
	}
	
	private void indexMenu(Scanner in) {
		
		System.out.println("The index already exists. Choose to:");
		System.out.println("1. Rebuild the index from scratch");
		System.out.println("2. Append to existing index");
		System.out.println("3. Go to the Search menu instead");
		
		int option;
		
		try{
			System.out.print("Enter option: ");
			option = in.nextInt();
			
			if((option < 1) || (option > 3)) {
				throw new InputMismatchException();
			}
			
			if(option == 1) {  // Rebuild the index
				
				System.out.println("Rebuilding index...");
				index("CREATE");
				System.out.println("Rebuilding the index complete!\n");
			}
			else if(option == 2) {  // Append to the existing index
/// TODO - Problem with appending to index; existing files are being indexed again
				System.out.println("Appending new documents to existing index...");
				index("APPEND");
				System.out.println("Appending to the index complete!\n");
			}
			else {  // Go to the search menu
				
				searchMenu(in);
			}
		} catch (InputMismatchException ex){
			System.out.println("You have to enter either 1, 2 or 3 !");
			in.nextLine();
			indexMenu(in);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
	}
	
	private int printMainMenu(Scanner in) {
		
		System.out.println("What do you want to do?");
		System.out.println("1. Index");
		System.out.println("2. Search");
		System.out.println("3. Quit");
		
		int option = 0;
		
		try{
			System.out.print("Enter Option: ");
			option = in.nextInt();
			
			if ((option < 1) || (option > 3)) {
				throw new InputMismatchException();
			}
		} catch (InputMismatchException ex){
			System.out.println("You have to enter either 1, 2 or 3 !");
			in.nextLine(); // Consume the \n character left by nextInt() in the buffer
			option = printMainMenu(in);
		}
		
		return option;
	}
	
	public void loop() {
		
		Scanner in = new Scanner(System.in);
		
		while(flag) {
			
			int option = printMainMenu(in);
			
			if(option == 1) {  // The user wants to Index
				
				File[] files = new File(getIndexDir()).listFiles();
				
				if(files.length == 0) {  // The index does not exist, create a new one
					
					System.out.println("Indexing folder...");
					
					try {
						index("CREATE");
					} catch (IOException e) {
						LOGGER.log(Level.SEVERE, e.toString(), e);
					}
					
					System.out.println("Indexing complete!\n");
				}
				else {  // The index already exists
					
					indexMenu(in);
				}
			}
			else if(option == 2) {  // The user wants to Search
				
				searchMenu(in);
			}
			else {  // The user wants to Quit
				flag = false;
				System.out.println("Goodbye!\n");
			}
		}
		
		in.close();
	}
	
	public static void main(String[] args){
		
		Launcher launcher = new Launcher();
		launcher.loop();
	}
}
