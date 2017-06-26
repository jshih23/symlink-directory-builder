package testing;

import java.awt.Cursor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.JTextArea;

public class parseTest {

	final static String newline = "\n";
	static File targetFolder;
	static File keckChosen;
	static File trunkLoc;

	public static void main(String[] args) {
		targetFolder = new File ("/work/SLDB-test-runs");//("/users/shih/Desktop/SLDB-links-test");
		keckChosen = new File ("/work/sirius/opkgbuild/limo_engine_pp1/src/obj_limo_engine_pp1_ram_arel/keck_file_list");
		trunkLoc = new File ("/work/sirius");
		System.out.println("running");
		
		// Clear existing directory if it exists
		if (targetFolder.exists()){
			System.out.println("Clearing Existing Directory...");
			deleteDir(targetFolder);
		}

		targetFolder.mkdir();


		// Prints contents of given kfl
		String thisLine = null;
		try {

			// open input stream test.txt for reading purpose.
			BufferedReader br = new BufferedReader(new FileReader(keckChosen.getPath()));

			// Parse through the keck_file_list and build symlink directory
			while ((thisLine = br.readLine()) != null) {
				
				// define symlink newLink and source
				Path newLink = Paths.get(targetFolder.getPath()+cutPath(thisLine)); // edit this to change output directory
				Path source = Paths.get(thisLine);

				makeSymLinkDirectory(newLink, source);
			}
			br.close();
		} catch(Exception e1) {
			System.err.println(e1);
			e1.printStackTrace();
		}
		
		System.out.println("fin");
	}


	static void makeSymLinkDirectory(Path target, Path source){

		if(source.toFile().exists()){
			// create paths to target/source files
			try {
				Files.createDirectories(target.getParent());
			} catch (IOException e) {
				//System.err.println(e);
				e.printStackTrace();
			}

			try {
				Files.createSymbolicLink(target, source);
			} catch (FileAlreadyExistsException x) {
				//System.err.println(x);
			} catch (UnsupportedOperationException x) {
				//System.err.println(x);
			} catch (IOException x){
				//System.err.println(x);
			}
		}
	}
	
	public static String cutPath(String p){
		if (p.startsWith(trunkLoc.getPath())){
			return p.substring(trunkLoc.getPath().length());
		}
		return p;
	}

	// delete directory and contents of that directory
	static void deleteDir(File file) {
		File[] contents = file.listFiles();
		if (contents != null) {
			for (File f : contents) {
				deleteDir(f);
			}
		}
		file.delete();
	}
}