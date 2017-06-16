package linkFileList;

import java.io.File;

import javax.swing.filechooser.FileFilter;

// Simple FileFilter to filter out non-keck_file_lists from a file chooser
public class KeckFilter extends FileFilter{

	@Override
	public boolean accept(File f) {
		if (f.isDirectory()){
			return true;
		}else
			return (f.getPath().endsWith("/keck_file_list"));
		
	}

	@Override
	public String getDescription() {
		return "keck_file_list only";
	}

}
