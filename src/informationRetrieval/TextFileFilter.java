package informationRetrieval;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.tika.Tika;

/**
 * A filter for files of type .txt, .pdf, .doc, .docx and .pptx.
 * @version 2.0
 * @author Bogdan
 * @see FileFilter
 */
public class TextFileFilter implements FileFilter{

	@Override
	public boolean accept(File pathname){
		
		Tika tika = new Tika();
		
		try {
			String filetype = tika.detect(pathname);
			
			if(filetype.equals("text/plain") || 
			   filetype.equals("application/pdf") || 
			   filetype.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || 
			   filetype.equals("application/msword") || 
			   filetype.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation"))
				return true;
			else
				return false;
		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}
}
