package informationRetrieval;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;

import org.apache.commons.io.*;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.DateTools.Resolution;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.SAXException;

import static informationRetrieval.LuceneUtils.*; // Static import allows for static members to be referenced without qualification

/**
 * The Indexer class manages an index by using an <i> IndexWriter. </i>
 * <p> <b>TODO:</b> setter for the IndexWriter, as to change the index directory path. </p>
 * @version 2.5
 * @author Bogdan
 * @see IndexWriter
 */
public class Indexer{
	
	private IndexWriter writer;
	
	/**
	 * The constructor takes the desired index directory path, then configures and builds the IndexWriter.
	 * @param indexDirPath - the path of the desired index directory
	 * @throws InvalidPathException
	 * @throws IOException
	 */
	public Indexer(String indexDirPath, String openMode) throws InvalidPathException, IOException{
		
		@SuppressWarnings("resource")
		Directory indexDir = FSDirectory.open(Paths.get(indexDirPath));
		
		IndexWriterConfig conf = new IndexWriterConfig(); // The default constructor user the StandardAnalyzer as an analyzer
		conf.setSimilarity(new FirstWordsSimilarity());   // Use our custom scoring class - delete if standard scoring is desired
		
		if(openMode.equals("CREATE"))
			conf.setOpenMode(OpenMode.CREATE);
		else if(openMode.equals("APPEND"))
			conf.setOpenMode(OpenMode.APPEND);
		else
			conf.setOpenMode(OpenMode.CREATE_OR_APPEND);
		
		writer = new IndexWriter(indexDir, conf);
	}
	
	public void close() throws CorruptIndexException, IOException{
		
		writer.close();
	}
		
	// Creates an indexable Document from the given File
	private Document createDocument(File file) throws IOException{
		
		Document doc = new Document();
		String contents = "";
		TokenStream tokens = null;
		
		Tika tika = new Tika();
		String filetype = tika.detect(file);
		
		if(filetype.equals("text/plain")){ // File is in .txt format
			
			contents = FileUtils.readFileToString(file, "UTF-8");
			tokens = LuceneUtils.getTokenStream(contents);
			
		} else if(filetype.equals("application/pdf")){ // File is in .pdf format
			
			InputStream input = new FileInputStream(file);
			BodyContentHandler handler = new BodyContentHandler(Integer.MAX_VALUE);
			
			try {
				new PDFParser().parse(input, handler, new Metadata(), new ParseContext());
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (TikaException e) {
				e.printStackTrace();
			}
			
			contents = handler.toString();
			tokens = LuceneUtils.getTokenStream(contents);
			
			input.close();
			
		} else{ // File is in another format
			
			try{
				tika.setMaxStringLength(-1);
				contents = tika.parseToString(file);
				tokens = LuceneUtils.getTokenStream(contents);
			} catch (TikaException e){
				e.printStackTrace();
			}
		}
		
		// Index and store the contents, name and path of the file
		TextField contentField = new TextField(CONTENTS, tokens);
		StringField nameField = new StringField(FILE_NAME, file.getName(), Field.Store.YES);
		StringField pathField = new StringField(FILE_PATH, file.getCanonicalPath(), Field.Store.YES);
		
		// Store the file creation date
		BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
		long milliseconds = attr.creationTime().toMillis();
		Date creation = new Date(milliseconds);
		String formattedDate = DateTools.dateToString(creation, Resolution.DAY);
		
		StringField creationDateField = new StringField(CREATED_AT, formattedDate, Field.Store.YES);

		// Store the file last modification date
		Date lastModified = new Date(file.lastModified());
		formattedDate = DateTools.dateToString(lastModified, Resolution.DAY);
		
		StringField lastModifiedDateField = new StringField(LAST_MODIFIED, formattedDate, Field.Store.YES);
		
		// Store the extension of the file
		String extension = "";

		int i = file.getCanonicalPath().lastIndexOf('.');
		if (i >= 0) {
		    extension = file.getCanonicalPath().substring(i+1);
		}
		
		StringField extensionField = new StringField(FILE_EXTENSION, extension, Field.Store.YES);
		
		// Add the Fields to the Document
		doc.add(contentField);
		doc.add(nameField);
		doc.add(pathField);
		doc.add(creationDateField);
		doc.add(lastModifiedDateField);
		doc.add(extensionField);
		
//		tokens.close(); // Can't close the Stream because the TokenStream contract is violated
		
		return doc;
	}
	
	// Indexes a File
	private void indexFile(File file) throws IOException{
		
		System.out.println("Indexing " + file.getCanonicalPath());
		Document doc = createDocument(file);
		writer.addDocument(doc);
	}
	
	// Creates the Index by indexing the specified files
	public int createIndex(String dataDirPath, FileFilter filter) throws IOException{
		
		// Take all the files from the source directory
		File[] files = new File(dataDirPath).listFiles();
		
		// Index all files which pass the filter
		for(File f : files){
			if(f.exists() && !f.isDirectory() && !f.isHidden() && f.canRead() && filter.accept(f)){
				indexFile(f);
			}
		}
		
		// Return the number of indexed files
		return writer.numDocs();
	}
}
