/*
 * SymLink Directory Builder Tool
 * 
 * Creates a directory of symbolic links to only the files in your 
 * Sirius repo from a keck_file_list.
 * 
 * Created by:	Jimmy Shih
 * Date: 		6/19/2017
 * Contact:	 	jimmy.shih@hp.com
 * 
 */

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;


@SuppressWarnings("serial")
public class WizardDialog extends JDialog implements ActionListener{

	// Class Variables
	final static String TRUNKPANEL = "trunk select";						//Identifiers for card names.
	final static String KECKPANEL = "keck file select";						//"
	static private final String newline = "\n";								//Newline constant for readability
	boolean trunkLoaded = false;											//Controls wizard flow based on
	boolean keckSelected = false;											//steps completed by user.
	static JFrame frame;													//Parent Container.
	static File trunkPath;													//Stores user entered trunk path.
	static String keckChosen = "";											//Location of 
	String partName;														//Part Name (i.e. limo_engine_pp1_ram_arel)
	File targetFolder;														//Where symbolic links will be placed.
	File policiesLog;														//Policy filter text file
	JFileChooser fc;														//File chooser to select trunk path.
	JTextArea log;															//Information display on processes
	JPanel trunkSelection, keckSelection, buttonPanel, labelPanel, cards;	//Panels that populate the container
	JComboBox<?> cb;														//Combobox (dropdown menu) for keck select

	// Button Definitions
	JButton cancelButton = new JButton("Exit");								//Buttons for wizard navigation
	JButton backButton = new JButton("Back");								//"
	JButton nextButton = new JButton("Next");								//"
	JButton finishButton = new JButton("Run");								//Button for program execution
	JButton loadButton = new JButton("Load...");							//Button for trunk path select


	//Setup wizard GUI. Populate parent container with panels.
	public void addComponentToPane(Container pane) {

		//Log Panel for information output
		log = new JTextArea(4, 60);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		log.append("No folder selected.");
		JScrollPane logScrollPane = new JScrollPane(log);
		logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//Button Panel for wizard navigation
		buttonPanel = new JPanel();
		cancelButton.addActionListener(this);
		backButton.addActionListener(this);
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);
		buttonPanel.add(cancelButton);
		buttonPanel.add(backButton);
		backButton.setEnabled(false);
		buttonPanel.add(nextButton);
		buttonPanel.add(finishButton);
		finishButton.setEnabled(false);
		finishButton.addActionListener(this);

		//File Chooser function and behavior settings
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setCurrentDirectory(new File("/work") );

		//Card for trunk path selection with a message and a button to load a file chooser
		trunkSelection = new JPanel();
		trunkSelection.setLayout(new BoxLayout(trunkSelection, BoxLayout.Y_AXIS));

		JPanel start = new JPanel();
		loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		loadButton.addActionListener(this);

		start.add(new JLabel ("<html>"
				+ "<b P ALIGN=CENTER>Welcome to the SymLink Directory Builder Tool</b>"
				+ "<br>"
				+ "<P ALIGN=CENTER>Please Locate Your Sirius Trunk Directory"));
		trunkSelection.add(start);
		trunkSelection.add(loadButton);


		//Create the panel that contains the cards. At this point, the only card is TRUNKPANEL
		//Later in the process, KECKPANEL will be added as well after user has completed certain steps
		cards = new JPanel(new CardLayout());
		cards.add(trunkSelection, TRUNKPANEL);
		cards.setBorder(new EmptyBorder(10, 10, 10, 10));

		//Populating the parent container with all of the panels
		pane.add(logScrollPane, BorderLayout.PAGE_START);
		pane.add(cards, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.PAGE_END);

	}

	//Defining button behaviors
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void actionPerformed(ActionEvent e) {

		//If a keck_file_list has been selected, user may run the program
		if (keckSelected == true){
			finishButton.setEnabled(true);
		}

		//If cancel/exit button is pressed, close the window.
		if (e.getSource() == cancelButton){
			frame.dispose();
		}

		//If finish/run button is pressed, links are created and directory is built.
		if (e.getSource() == finishButton){
			log.append("--------------------------------------------------"+newline);
			log.append("Creating SymLink Directory. Please wait..."+newline);
			log.append("--------------------------------------------------");

			//Executed in an event dispatch thread to not interfere with current processes.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// Clear existing directory if it exists
					if (targetFolder.exists()){
						//System.out.println("Clearing Existing Directory...");
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
						BufferedReader br = new BufferedReader(new FileReader(getKeckChosen().getPath()));
						log.append("Creating Symbolic Links from"+getKeckChosen().getPath()+newline);

						// Parse through the keck_file_list and build symlink directory
						while ((thisLine = br.readLine()) != null) {
							log.append(thisLine+newline);
							Path source = Paths.get(thisLine);
							// define symlink newLink and source
							Path newLink = Paths.get(targetFolder.getPath()+cutPathPrefix(thisLine)); // edit this to change output directory
//							if(thisLine.contains("/printengine/src/")){
//								//System.out.println(thisLine);
//								String fileName = thisLine.substring( thisLine.lastIndexOf('/')+1, thisLine.length() );
//								//System.out.println("file: "+fileName);
//								if (mfParse(partName)!=null){
//									//System.out.println(isInText(fileName, policiesLog));
//									if(isInText(fileName, policiesLog)){
//										source = Paths.get(thisLine);
//										makeSymLinkDirectory(newLink, source, log);
//									}
//								}
//								else{ }//do nothing, don't create symlink and continue}
//
//							}
//							else{
								//source = Paths.get(thisLine);
								makeSymLinkDirectory(newLink, source, log);
							//}
						}
						br.close();
					} catch(Exception e1) {
						e1.printStackTrace();
					}
					//Wrap up program process, allow user to run again.
					finishButton.setEnabled(true);
					log.append(newline+"------------------------------------------"+newline);
					log.append(" Symbolic Link Directory Created!"+newline);
					log.append("------------------------------------------");
					infoBox("Please navigate to your target directory at: "+targetFolder.getPath()+newline+"You may rename or move the directory wherever you'd like.", "Done");
				}
			});
			//Disallow user to run again while program is in process.
			finishButton.setEnabled(false);


		}

		//Back button can only be pressed if user is on the second card of the wizard
		if (e.getSource() == backButton){
			backButton.setEnabled(false);
			nextButton.setEnabled(true);
			finishButton.setEnabled(false);
			CardLayout cl = (CardLayout)(cards.getLayout());
			cl.show(cards, TRUNKPANEL);
		}
		//Next button can only be pressed if user is on the first card of the wizard
		else if (e.getSource() == nextButton){
			backButton.setEnabled(true);
			nextButton.setEnabled(false);
			if (keckSelected == true){
				finishButton.setEnabled(true);
			}
			CardLayout cl = (CardLayout)(cards.getLayout());
			cl.show(cards, KECKPANEL);

			//

		}

		//If Load button is pressed, user is prompted with a file chooser to select their Sirius trunk path.
		//If selected directory is not the correct Sirius trunk path, use is prompted to try again and cannot
		//press 'next' button. 
		else if (e.getSource() == loadButton){

			//Open file chooser
			int returnVal = fc.showOpenDialog(trunkSelection); 
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				//Update log on file chosen
				trunkPath = fc.getSelectedFile();
				log.setText("");
				log.append("Folder selected: " + trunkPath.getPath()+ newline);

				//Check if is Sirius trunk path. 
				//Currently this is done by checking if /opkgbuild is a directory within given path
				if (!errorPrintPathCheck(trunkPath.toPath())){
					nextButton.setEnabled(false);
					log.append("Please recheck your sirius trunk path");
					trunkLoaded = false;
				}
				else{
					trunkLoaded = true;
				}
			}
			//If invalid folder or no folder chosen, reset log.
			else{
				log.setText("");
				log.append("No folder selected." + newline);
			}

			//If valid folder is chosen, enabled 'next' button, and search for keck_file_lists within the given folder 
			if(trunkLoaded){
				//enable 'next' button
				nextButton.setEnabled(true);
				//update log with selected folder
				log.setText("");
				log.append("Folder selected: " + trunkPath.getPath()+ newline);

				//Create KECKPANEL card and populate it with a combobox that contains existing keck_file_lists found
				//in given folder.
				keckSelection = new JPanel();
				keckSelection.setLayout(new BoxLayout(keckSelection, BoxLayout.Y_AXIS));

				JLabel keckSelectText = new JLabel("Select your keck_file_list:"+newline);
				keckSelectText.setAlignmentX(Component.CENTER_ALIGNMENT);
				keckSelection.add(keckSelectText);
				cb = new JComboBox(findKecks(trunkPath.toPath()).toArray());
				cb.setMaximumSize(new Dimension(1000, 35));
				cb.setEditable(false);
				cb.addActionListener(this);
				keckSelection.add(cb);
				cards.add(keckSelection, KECKPANEL);
			}
		}

		//Allow use to run the program (press 'finish' or 'run') if and only if a valid keck_file_list is chosen
		if(e.getSource() == cb){
			keckChosen = (String) cb.getSelectedItem();
			//System.out.println("selected:" + keckChosen);
			//System.out.println(cb.getSelectedIndex());
			if (cb.getSelectedIndex() == 0){
				keckSelected = false;
				finishButton.setEnabled(false);
			}
			else{
				keckSelected = true;
				partName = getPartName();
				targetFolder = new File("/work/SLDB-links-directories/"+partName);
				finishButton.setEnabled(true);
			}
			//System.out.println(keckSelected);
		}

	}

//	public File mfParse(String partName){
//
//		String defaultPolicies = "PE_POLICY_POLICIES += ";
//		String defaultObjModules = "PE_POLICY_OBJMODULES += ";
//		String partPolicies = null;
//		String objModules = null;
//		if (partName.contains("limo_engine")){
//			partPolicies = "PE_POLICIES_limo += ";
//			objModules = "PE_POLICY_OBJMODULES_limo += ";
//		}
//		else if (partName.contains("bugatti_engine")){
//			partPolicies = "PE_POLICIES_autobahn += ";
//			objModules = "PE_POLICY_OBJMODULES_bugatti += ";
//		}
//		else{
//			return null;
//		}
//
//		File policies = new File("/work/sirius/product/src/build/subsys_pe_policy.mf");
//
//		String[] suffixes = {"_cid.h", "_module.c", "_sys.c", "_sys.h", ".c", ".h", ".mf"};
//
//		try{
//			BufferedWriter b = new BufferedWriter(new FileWriter(targetFolder.toPath()+"_policies.txt"));
//
//			BufferedReader reader = new BufferedReader(new FileReader(policies));
//			String line = null;
//			while ((line = reader.readLine()) != null) {
//				String fileName = null;
//
//				if (line.startsWith(partPolicies)){
//					fileName = concatPolicy(line.substring(partPolicies.length()));
//					//System.out.println(fileName);
//					b.write(fileName+newline);
//				}
//				else if(line.startsWith(objModules)){
//					fileName = line.substring(objModules.length());
//					//System.out.println(fileName);
//					for(int i=0; i < suffixes.length; i++){
//						//System.out.println(fileName+suffixes[i]);
//						b.write(fileName+suffixes[i]+newline);
//					}
//
//				}
//				else if(line.startsWith(defaultPolicies)){
//					fileName = concatPolicy(line.substring(defaultPolicies.length()));
//					//System.out.println(fileName);
//					b.write(fileName+newline);
//				}
//				else if(line.startsWith(defaultObjModules)){
//					fileName = line.substring(defaultObjModules.length());
//					//System.out.println(fileName);
//					for(int i=0; i < suffixes.length; i++){
//						//System.out.println(fileName+suffixes[i]);
//						b.write(fileName+suffixes[i]+newline);
//					}
//				}
//			}
//			b.close();
//			reader.close();
//			policiesLog = new File(targetFolder.toPath()+"_policies.txt");
//			return policiesLog;
//
//		} catch (IOException x) {
//			System.err.format("IOException: %s%n", x);
//			return null;
//		}
//	}

//	public static String concatPolicy(String policy){
//		return "pe_policies_policy_"+policy+".c";
//	}

	//Method for locating existing keck_file_lists in given trunk path
	public ArrayList<String> findKecks(Path trunkLoc) {

		String path = trunkLoc+"/opkgbuild/";
		String[] command = {"find", path, "-name", "keck_file_list"};
		ArrayList<String> kecks = new ArrayList<String>();
		kecks.add("Select a keck_file_list");
		String s = null;

		try {
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));

			@SuppressWarnings("unused")
			BufferedReader stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()));

			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				kecks.add(s);
			}


			//System.exit(0);
		}
		catch (IOException e) {
			log.append("unknown error"+newline);
			e.printStackTrace();
			System.exit(-1);
		}

		return kecks;
	}

//	static boolean isInText(String line, File txt){
//		try {
//			BufferedReader br = new BufferedReader(new FileReader(txt));
//			List<String> list = new ArrayList<>();
//			String thisLine = null;
//			while((thisLine = br.readLine()) != null){
//				list.add(thisLine); 
//			}
//			//System.out.println(Arrays.toString(list.toArray()));
//			if(list.contains(line)){
//				return true;
//			}else{
//				return false;
//			}
//		}catch(Exception e){
//			System.out.println("2");
//			e.printStackTrace();
//			return false;
//		}
//	}
	
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
				e.printStackTrace();
			}

			try {
				Files.createSymbolicLink(target, source);
			} catch (FileAlreadyExistsException x) {
				log.append("Symbolic Link Already Created!: No Action Needed"+newline);
				//System.err.println(x);
			} catch (UnsupportedOperationException x) {
				// Some file systems do not support symbolic links.
				log.append(x+newline);
			} catch (IOException x){
				log.append("Something went wrong."+newline);
			}
		}
		else{
			log.append("File Not Found:" + source + " is not invalid file path"+newline);
		}
	}



	//Checks if a directory is a Sirius trunk directory by checking for the existence of the opkgbuild directory
	public static boolean errorPrintPathCheck(Path trunk){
		String path = trunk+"/opkgbuild";
		File toCheck = new File(path);
		return toCheck.exists();
	}

	//removes the trunk path trunkPath from given path name p
	public static String cutPathPrefix(String p){
		if (p.startsWith(trunkPath.getPath())){
			return p.substring(trunkPath.getPath().length());
		}
		return p;
	}

	//Getter method to retrieve chosen keck_file_list
	public static File getKeckChosen(){
		File f = new File(keckChosen);
		return f;
	}

	public static String getPartName(){
		String keck_list = getKeckChosen().getPath();

		String pathFromObj = keck_list.substring(keck_list.indexOf("obj_")+4);

		int slashIndex = pathFromObj.indexOf("/");

		String name = pathFromObj.substring(0, slashIndex);

		return name;
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event dispatch thread.
	 */
	public static void createAndShowGUI() {
		//Create and set up the window.
		frame = new JFrame("SLDB Wizard");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//Create and set up the content pane.
		WizardDialog demo = new WizardDialog();
		demo.addComponentToPane(frame.getContentPane());

		//Display the window.
		frame.pack();
		frame.setVisible(true);
	}


	public static void main(String[] args) {
		/* Use an appropriate Look and Feel */
		try {
			//UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		// Turn off metal's use of bold fonts
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

}