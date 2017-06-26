/*
 * SymLink Directory Builder Tool
 * 
 * Creates a directory of symbolic links to only the files in your 
 * Sirius repo from a keck_file_list.
 * 
 * Created by:	Jimmy Shih
 * Date: 		6/14/2017
 * Contact:	 	jimmy.shih@hp.com
 * 
 */

package linkFileList;


import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import wizardDialog.WizardDialog;

//import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class FileChooserGUI extends JPanel implements ActionListener {
	WizardDialog wd = new WizardDialog();
	// 
	static private final String newline = "\n";
	
	//UI Elements
	JButton sourceButton, targetButton, runButton;
	JTextArea log;
	JFileChooser fc1, fc2;
	
	//Stores the selected keck_file_list and targetFolder in File form
	File keck_file, targetFolder;
	
	//When both of these are true, then user can execute the program
	boolean keckLoaded = false; 	//if a keck_file_list has been successfully loaded, change to true
	boolean targetLoaded = false; 	//if a valid directory has been selected, change to true
	
	//Used for error messages after execution
	boolean success = true; 		//Unless the program runs into any problems, the 
	
	//User's home directory (i.e. "users/smith")
	String homeDir = System.getProperty("user.home");

	public FileChooserGUI() {
		//set border layout
		super(new BorderLayout());

		//Create the log first, because the action listeners
		//need to refer to it.
		log = new JTextArea(5,20);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		JScrollPane logScrollPane = new JScrollPane(log);

		//Create a file chooser
		fc1 = new JFileChooser();
		fc2 = new JFileChooser();
		
		// File Filters
		KeckFilter df = new KeckFilter(); 	// This filter only allows keck_file_lists to be selected from the fileChooser
		fc1.addChoosableFileFilter(df); 	// Adding this filter to fc1


		// Restrict file choosers to certain file types
		fc1.setFileSelectionMode(JFileChooser.FILES_ONLY); 			// Allow only files to be chosen for the sourceButton
		fc2.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); 	// Allow only directories to be chosen for the targetButton
		fc1. setAcceptAllFileFilterUsed(false);						// Prevent users from choosing other file types.
		fc2. setAcceptAllFileFilterUsed(false);						// Prevent users from choosing other file types.

		//Create the keck_file_list button. 
		sourceButton = new JButton("Upload keck_file_list...");
		sourceButton.addActionListener(this);
		sourceButton.setToolTipText("<html>Please select your project's keck_list_file.<br>"
				+ "This file is generated after you have built your<br>"
				+ "project at least once (using makepkg), and can be found in:<br>"
				+ "<br>"
				+ "/opkgbuild/[your project]_[hardware phase]/src/obj_[your project]_[hardware phase]_ram_arel/keck_file_list</html>");

		//Create the target directory button.
		targetButton = new JButton("Set Target Directory...");
		targetButton.addActionListener(this);
		targetButton.setToolTipText("<html>Please select your target directory.<br>"
				+ "This is where the symlinks will be generated.<br>"
				+ "After symlinks are created, import this folder to your preferred IDE/Text Editor<br>"
				+ "<br>"
				+ "! IMPORTANT: If the folder already exists, all of its contents will be permanently deleted. <br>"
				+ "Do not select a folder that you do not want deleted !(i.e. home, Desktop, etc...)</html>");

		//Create the Run button that Executes program.
		runButton = new JButton("Create SymLink Directory");
		runButton.addActionListener(this);
		runButton.setEnabled(false);
		runButton.setToolTipText("<html>Create your SymLink directory.<br>"
				+ "<br>"
				+ "! IMPORTANT: PLEASE ENSURE THAT YOUR SELECTED TARGET FOLDER IS NOT IMPORTANT.<br>"
				+ "ITS CONTENTS WILL BE DELETED AND REPLACED !</html>");

		//For layout purposes, put the buttons in a separate panel
		JPanel buttonPanel = new JPanel(); 	//use FlowLayout for buttons
		buttonPanel.add(sourceButton);
		buttonPanel.add(targetButton);
		buttonPanel.add(runButton);
		
		// Add instructions panel
		JPanel infoPanel = new JPanel();
		JLabel infoLabel = new JLabel("<html><HR><br>"
				+ "<H1 P ALIGN=CENTER>Welcome to the SymLink Directory Builder Tool</H1><br>"
				+ "<HR><br>"
				+ "<P ALIGN=CENTER>Please read the instructions carefully before using this tool:</U><br>"
				+ "<H2 P ALIGN=CENTER><U>INSTRUCTIONS:</H2>"
				+ "<OL>"
				+ "<LI>Click [Upload keck_file_list...] and locate your keck_file_list."
				+ "<OL><LI>A keck_file_list is a text file that is generated whenever you makepkg your project.<br>"
				+ "<LI>Make sure you have makepkg'd your project at least once, or else you won't find the keck_file_list.<br>"
				+ "<LI>The keck_file_list for your particular project can be found in your local Sirius directory under:<br>"
				+ "<BLOCKQUOTE>/opkgbuild/[your project]_[hardware phase]/src/obj_[your project]_[hardware phase]_ram_arel/keck_file_list</BLOCKQUOTE>"
				+ "</OL><br>"
				+ "<LI>Click on [Set TargetDirectory...] and select the your output folder.<br>"
				+ "------------------------------------------------------------------------------------------------------------------------------------------------------------------<br>"
				+ "NOTE: ! IMPORTANT: DO NOT SELECT AN IMPORTANT FOLDER THAT YOU CANNOT AFFORD TO DELETE (i.e. /users, /Desktop, etc...)<br>"
				+ "THE CONTENTS OF THE FOLDER WILL BE REPLACED WITH SYMLINKS TO YOUR LOCAL SIRIUS REPO ! THIS IS IRREVERSIBLE !<br>"
				+ "------------------------------------------------------------------------------------------------------------------------------------------------------------------"
				+ "<OL><LI>For the above reason, it is recommended that you create a new empty folder and select this as your target folder.<br>"
				+ "<LI>This can be done by right clicking in the folder selection wizard and selecting [New Folder].<br>"
				+ "</OL><br>"
				+ "<LI>Once your keck_file_list has been uploaded and your target folder has been set, click [Create SymLink Directory].<br>"
				+ "<OL><LI>The program will take a minute to execute.<br>"
				+ "<LI>Once finished, your target folder should be populated with symbolic links and their relevant parent directories.<br>"
				+ "</OL><br>"
				+ "<LI>Close the SymLink Directory Builder Tool, navigate to your target folder, and import it to your IDE/text editor of choice.<br>"
				+ "<OL><LI>In Eclipse:"
				+ "<OL><LI>Right click in your Package Explorer View, then click on Import->General->Projects from Folder or Archive,"
				+ "<br>then press [Next>]."
				+ "<LI>Next to Import Source, click on [Directory], and then navigate to your target folder, and clikc [OK]<br>"
				+ "<LI>Ensure Search For Nested Projects and Detect and Configure Project Natures are both checked.<br>"
				+ "<LI>Click [Finish]</OL>"
				+ "</OL><br>");
		infoLabel.setFont(new Font("Verdana",1,12)); //For readability
		infoPanel.add(infoLabel);

		//Add the buttons and the log to this panel.
		add(buttonPanel, BorderLayout.PAGE_START);
		add(logScrollPane, BorderLayout.CENTER);
		add(infoPanel, BorderLayout.PAGE_END);

	}

	public void actionPerformed(ActionEvent e) {

		//Handle source button action:
		//This opens a file chooser, stores the selected file in keck_file,
		//updates the status of keckLoaded, and prints user actions to the log panel. Nothing is executed yet
		if (e.getSource() == sourceButton) {			
			
			wd.createAndShowGUI();

			
//			int returnVal = fc1.showOpenDialog(FileChooserGUI.this); //Open file chooser
//			if (returnVal == JFileChooser.APPROVE_OPTION) {
//
//				keck_file = fc1.getSelectedFile();

			

			//Handle target button action:
			//Before continuing, a warning window pops up
			//Then a file chooser opens, and the selected directory is stored in targetFolder. In the file
			//chooser, the user is limited to non-critical directories. If such a directory is chosen, a
			//warning window pops up and the user cannot execute the program until a different directory
			//is chosen. User actions are printed in the log panel.
		} else if (e.getSource() == targetButton) {
			infoBox("If the folder you select is NOT empty, all if its contents will be DELETED and REPLACED."+newline
					+"DO NOT choose a folder that you do not wish to erase (i.e. home, workspace, etc...)","WARNING: Tread Carefully");
			int returnVal = fc2.showOpenDialog(FileChooserGUI.this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				targetFolder = fc2.getSelectedFile();
				
				// If the targetFolder is not a safe directory to delete
				if(targetFolder.getPath().endsWith(homeDir) || targetFolder.getPath().endsWith(homeDir+"/Desktop") ||
						targetFolder.getPath().endsWith(homeDir+"/Documents") || targetFolder.getPath().endsWith(homeDir+"/Pictures") ||
						targetFolder.getPath().endsWith(homeDir+"/Vidoes") || targetFolder.getPath().endsWith(homeDir+"/Downloads") ||
						targetFolder.getPath().endsWith(homeDir+"/Music") || targetFolder.getPath().endsWith("/users") || 
						targetFolder.getPath().endsWith(homeDir+"/Public") || targetFolder.getPath().endsWith("/")){
					infoBox("Please select a different directory","WARNING: Unsafe Target");
					targetLoaded = false; // don't allow user to continue until a new directory is chosen
				}
				else{
					targetLoaded = true;
				}
				log.append("target directory selected: " + targetFolder.getName() + "." + newline);
			} else {
				log.append("command cancelled by user." + newline);
			}
			log.setCaretPosition(log.getDocument().getLength());
		}
		
		System.out.println("chosen: " + wd.getKeckChosen());
		keck_file = wd.getKeckChosen();
		keckLoaded = true;
		log.append("keck_file_list selected: " + keck_file.getName() + "." + newline);
		log.setCaretPosition(log.getDocument().getLength());
		
		// If user selects valid keck_file_list and valid target directory, allow them to continue
		// to execute the program
		if (keckLoaded && targetLoaded){
			runButton.setEnabled(true);
		}

		// Handle run button action:
		// Creates the symbolic link directory
		if (e.getSource() == runButton) {
			log.append("--------------------------------------------------"+newline);
			log.append("Creating SymLink Directory. Please wait..."+newline);
			log.append("--------------------------------------------------"+newline);

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					// Clear existing directory if it exists
					if (targetFolder.exists()){
						System.out.println("Clearing Existing Directory...");
						deleteDir(targetFolder);
					}

					// create empty symlink directory
					if (targetFolder.mkdir())
						log.append("Directory succesfully created"+newline);
					else
						log.append("Directory creation failed, please double check inputs"+newline);

					// Prints contents of given kfl
					String thisLine = null;
					try {

						// open input stream test.txt for reading purpose.
						BufferedReader br = new BufferedReader(new FileReader(keck_file.getPath()));
						log.append("Creating Symbolic Links from"+keck_file.getPath()+newline);

						// Parse through the keck_file_list and build symlink directory
						while ((thisLine = br.readLine()) != null) {
							log.append(thisLine+newline);

							// define symlink newLink and source
							Path newLink = Paths.get(targetFolder.getPath()+thisLine); // edit this to change output directory
							Path source = Paths.get(thisLine);

							makeSymLinkDirectory(newLink, source, log);
						}
						br.close();
					} catch(Exception e1) {
						System.out.println("2");
						e1.printStackTrace();
					}
					if (success){
						log.append("------------------------------------------"+newline);
						log.append(" Symbolic Link Directory Created!"+newline);
						log.append("------------------------------------------"+newline);
						infoBox("Please close the program and navigate to your target directory at: "+targetFolder.getPath(), "SUCCESS: Symbolic Link Directory Created!");
					}
					else{
						log.append("-----------------------------------------------------------------------------------------------"+newline);
						log.append("Directory Creation Incomplete. Please make sure your keck_file_list is correct."+newline);
						log.append("-----------------------------------------------------------------------------------------------"+newline);
						infoBox("There were files in the keck_file_list that could not be located in your local Sirius repo."+newline
								+ "A Symbolic Link Directory was still created without those files."+newline
								+newline+ "To run again, please quit and restart the program", "NOTICE: Directory Creation Incomplete.");
					}
				}
			});
			// Disable buttons to prevent potential issues. It's easier to just restart the program.
			runButton.setEnabled(false);
			sourceButton.setEnabled(false);
			targetButton.setEnabled(false);
		}
	}

	// Creates a single instance of a popup window. Used for warnings and notifications
	public static void infoBox(String infoMessage, String titleBar)
	{
		JOptionPane.showMessageDialog(null, infoMessage, titleBar, JOptionPane.INFORMATION_MESSAGE);
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

	//Create the symbolic links and relevant parent directories
	static void makeSymLinkDirectory(Path target, Path source, JTextArea log){

		if(source.toFile().exists()){
			// create paths to target/source files
			try {
				Files.createDirectories(target.getParent());
			} catch (IOException e) {
				System.out.println("1");
				e.printStackTrace();
			}

			try {
				Files.createSymbolicLink(target, source);
			} catch (FileAlreadyExistsException x) {
				log.append("Symbolic Link Already Created!: No Action Needed"+newline);
				//System.err.println(x);
			} catch (UnsupportedOperationException x) {
				// Some file systems do not support symbolic links.
				System.err.println(x);
			} catch (IOException x){
				log.append("Something went wrong."+newline);
			}
		}
		else{
			log.append("File Not Found:" + source + " is not invalid file path"+newline);
		}
	}


	 //Create the GUI and show it.  For thread safety,
	 //this method should be invoked from the
	 //event dispatch thread.
	private static void createAndShowGUI() {
		//Create and set up the window.
		JFrame frame = new JFrame("SymLink Directory Builder Tool");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Add content to the window.
		frame.add(new FileChooserGUI());

		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				UIManager.put("swing.boldMetal", Boolean.FALSE); 
				createAndShowGUI();
			}
		});
	}
}
