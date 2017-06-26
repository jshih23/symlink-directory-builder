package wizardDialog;

import java.io.File;
import java.nio.file.Path;

//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.io.InputStreamReader;
//
public class scriptTest {

	public static void main(String[] args) {
		//		String s = null;
		//		String[] command = {"find", "/work/sirius/opkgbuild/", "-name", "keck_file_list"};
		//
		//		try {
		//			
		//			Process p = Runtime.getRuntime().exec(command);
		//
		//			BufferedReader stdInput = new BufferedReader(new 
		//					InputStreamReader(p.getInputStream()));
		//
		//			BufferedReader stdError = new BufferedReader(new 
		//					InputStreamReader(p.getErrorStream()));
		//
		//			// read the output from the command
		//			System.out.println("standard output:\n");
		//			while ((s = stdInput.readLine()) != null) {
		//				System.out.println(s);
		//			}
		//
		//			// read any errors from the attempted command
		//			System.out.println("standard error:\n");
		//			while ((s = stdError.readLine()) != null) {
		//				System.out.println(s);
		//			}
		//
		//			System.exit(0);
		//		}
		//		catch (IOException e) {
		//			System.out.println("exception happened - here's what I know: ");
		//			e.printStackTrace();
		//			System.exit(-1);
		//		}
		//	}
		//
		System.out.println(errorPrintPathCheck("/work/root"));
		
	}
	
	public static boolean errorPrintPathCheck(String trunk){
		String path = trunk+"/opkgbuild";
		File toCheck = new File(path);
		return toCheck.exists();
	}
}

