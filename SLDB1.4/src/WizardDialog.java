/*
 * SymLink Directory Builder Tool 1.4
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
	boolean trunk_path_isLoaded = false;									//Controls wizard flow based on
	boolean keck_file_isSelected = false;									//steps completed by user.
	static JFrame frame;													//Parent Container.
	static File loaded_trunk_path;											//Stores user entered trunk path.
	static String selected_keck_file_path_name = "";						//Location of 
	String full_part_name;													//Part Name (i.e. limo_engine_pp1_ram_arel)
	String short_part_name;													//Shortened part name (i.e. limo_engine)
	String product_name;													//General product name (i.e. limo)
	File TARGET_FOLDER;														//Where symbolic links will be placed.
	File POLICIES_TO_INCLUDE;												//Policy filter text file
	File POLICIES_TO_EXCLUDE;
	JFileChooser fc;														//File chooser to select trunk path.
	JTextArea log;															//Information display on processes
	JPanel trunkSelection, keckSelection, buttonPanel, labelPanel, cards;	//Panels that populate the container
	JComboBox<?> cb;														//Combobox (dropdown menu) for keck select

	// Button Definitions
	JButton cancelButton = new JButton("Exit");								//Buttons for wizard navigation
	JButton backButton = new JButton("Back");								//"
	JButton nextButton = new JButton("Next");								//"
	JButton runButton = new JButton("Run");									//Button for program execution
	JButton loadButton = new JButton("Load...");							//Button for trunk path select

	//Checkbox definitions
	JCheckBox policy_checkbox = new JCheckBox("Filter policies for limo engine, bugatti engine, or vulcan engine");
	boolean policy_checkbox_isChecked = false; 


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
		buttonPanel.add(runButton);
		runButton.setEnabled(false);
		runButton.addActionListener(this);

		//File Chooser function and behavior settings
		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setCurrentDirectory(new File("/work") );

		//Card for trunk path selection with a message and a button to load a file chooser
		trunkSelection = new JPanel();
		trunkSelection.setLayout(new BoxLayout(trunkSelection, BoxLayout.Y_AXIS));

		// Add load button for user to select their sirius trunk directory
		JPanel start = new JPanel();
		loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		loadButton.addActionListener(this);

		// Add info text
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
		if (keck_file_isSelected){
			runButton.setEnabled(true);
		}

		//If cancel/exit button is pressed, close the window.
		if (e.getSource() == cancelButton){
			frame.dispose();
		}

		//If finish/run button is pressed, links are created and directory is built.
		if (e.getSource() == runButton){
			log.append("--------------------------------------------------"+newline);
			log.append("Creating SymLink Directory. Please wait..."+newline);
			log.append("--------------------------------------------------");

			//Executed in an event dispatch thread to not interfere with current processes.
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					// Clear existing directory if it exists
					if (TARGET_FOLDER.exists()){
						//System.out.println("Clearing Existing Directory...");
						deleteDir(TARGET_FOLDER);
					}

					// create empty symlink directory
					if (TARGET_FOLDER.mkdir())
						log.append("Directory succesfully created"+newline);
					else
						log.append("Directory creation failed, please double check inputs"+newline);

					POLICIES_TO_INCLUDE = policyFilter(product_name);
					POLICIES_TO_EXCLUDE = policyExcludeFilter(product_name);
					
					// Prints contents of selected keck_file_list
					String thisLine = null;
					try {

						// open input stream test.txt for reading purpose.
						BufferedReader br = new BufferedReader(new FileReader(getKeckChosen().getPath()));
						log.append("Creating Symbolic Links from"+getKeckChosen().getPath()+newline);

						// Parse through the keck_file_list and build symlink directory
						while ((thisLine = br.readLine()) != null) {
							Path new_link = Paths.get(TARGET_FOLDER.getPath()+cutPathPrefix(thisLine)); 
							Path source;
							if (!policy_checkbox_isChecked){
								log.append(thisLine+newline);
								source = Paths.get(thisLine);
								makeSymLinkDirectory(new_link, source, log);
							}
							// if policy checkbox is checked
							else if (policy_checkbox_isChecked){
								
								// and if the current line is a policy file
								if(thisLine.contains("/pe_policies/src/policies/")){
									// get the name of the policy file
									String fileName = thisLine.substring( thisLine.lastIndexOf('/')+1, thisLine.length() );
									
									// if the policy file is found in the POLICIES_TO_EXCLUDE list...
									if (isInText(fileName, POLICIES_TO_EXCLUDE)){
										// don't create a symbolic link of it (do nothing)
									}
									// or if the policy file is found in the POLICIES_TO_INCLUDE, create symlink
									else if(isInText(fileName, POLICIES_TO_INCLUDE)){
										log.append("policy added: "+ thisLine+newline);
										source = Paths.get(thisLine);
										makeSymLinkDirectory(new_link, source, log);
									}
									// otherwise, the policy file is not needed and can be excluded as well
									else{
										//do nothing
									}
								}
								
								// or if the current line is in printengine/src, it might be a PE_POLICY_OBJMODULE file
								else if (thisLine.contains("/printengine/src/")){
									// get the name of the current folder or file
									String fileName = thisLine.substring( thisLine.lastIndexOf('/')+1, thisLine.length() );
									
									// if the file/folder is in the POLICIES_TO_EXCLUDE list...
									if (isInText(fileName, POLICIES_TO_EXCLUDE)){
										// don't create a symbolic link of it (do nothing)
									}
									// otherwise, the policy file might be needed and should be included.
									else{
										log.append(thisLine+newline);
										source = Paths.get(thisLine);
										makeSymLinkDirectory(new_link, source, log);
									}
								}
								
								// if the file isn't a policy file or and OBJMODULE, create a symlink 
								else{
									log.append(thisLine+newline);
									source = Paths.get(thisLine);
									makeSymLinkDirectory(new_link, source, log);
								}


							}
						}
						
						// close the buffered reader
						br.close();
					} catch(Exception e1) {
						e1.printStackTrace();
					}
					//Wrap up program process, and allow user to run again if desired.
					runButton.setEnabled(true);
					log.append(newline+"------------------------------------------"+newline);
					log.append(" Symbolic Link Directory Created!"+newline);
					log.append("------------------------------------------");
					infoBox("Please navigate to your target directory at: "+TARGET_FOLDER.getPath()+newline+"You may rename or move the directory wherever you'd like.", "Done");
				}
			});
			//Disallow user to run again while program is in process.
			runButton.setEnabled(false);


		}

		//Back button can only be pressed if user is on the second card of the wizard
		if (e.getSource() == backButton){
			// When back button is pressed, user is at the first card again, so back button is re-disabled
			backButton.setEnabled(false);
			// Now that user is at the first card again, next button is re-enabled
			nextButton.setEnabled(true);
			// To avoid user mistakes, user must be in the second card to initiate program process, so run button is disabled
			runButton.setEnabled(false);
			
			// display the current updated card
			CardLayout cl = (CardLayout)(cards.getLayout());
			cl.show(cards, TRUNKPANEL);
		}
		//Next button can only be pressed if user is on the first card of the wizard
		else if (e.getSource() == nextButton){
			// When next button is pressed, user is at the second and last card, so back button is enabled
			backButton.setEnabled(true);
			// When next button is pressed, user is at the second and last card, so next button is disabled
			nextButton.setEnabled(false);
			
			//If user had selected a keck_file_list in the second card, but pressed back (run button is disabled on the first card),
			//when the user presses next again, the run button will be re-enabled
			if (keck_file_isSelected == true){
				runButton.setEnabled(true);
			}
			
			// display the current updated card
			CardLayout cl = (CardLayout)(cards.getLayout());
			cl.show(cards, KECKPANEL);


		}

		//If Load button is pressed, user is prompted with a file chooser to select their Sirius trunk path.
		//If selected directory is not the correct Sirius trunk path, use is prompted to try again and cannot
		//press 'next' button. 
		else if (e.getSource() == loadButton){

			//Open file chooser
			int returnVal = fc.showOpenDialog(trunkSelection); 
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				//Update log on file chosen
				loaded_trunk_path = fc.getSelectedFile();
				log.setText("");
				log.append("Folder selected: " + loaded_trunk_path.getPath()+ newline);

				//Check if is Sirius trunk path. 
				//Currently this is done by checking if /opkgbuild is a directory within given path
				if (!errorPrintPathCheck(loaded_trunk_path.toPath())){
					nextButton.setEnabled(false);
					log.append("Please recheck your sirius trunk path");
					trunk_path_isLoaded = false;
				}
				else{
					trunk_path_isLoaded = true;
				}
			}
			//If invalid folder or no folder chosen, reset log.
			else{
				log.setText("");
				log.append("No folder selected." + newline);
			}

			//If valid folder is chosen, enabled 'next' button, and search for keck_file_lists within the given folder 
			if(trunk_path_isLoaded){
				//enable 'next' button
				nextButton.setEnabled(true);
				//update log with selected folder
				log.setText("");
				log.append("Folder selected: " + loaded_trunk_path.getPath()+ newline);

				//Create KECKPANEL card and populate it with a combobox that contains existing keck_file_lists found
				//in given folder.
				keckSelection = new JPanel();
				keckSelection.setLayout(new BoxLayout(keckSelection, BoxLayout.Y_AXIS));

				//Second card isn't generated until after a trunk path has been selected by the user. This might seem a bit strange,
				// but it must be done this way because the contents of the second page depend on the trunk path selected by the user,
				// which cannot be determined preemptively
				JLabel keckSelectText = new JLabel("Select your keck_file_list:"+newline);
				keckSelectText.setAlignmentX(Component.CENTER_ALIGNMENT);
				keckSelection.add(keckSelectText);
				cb = new JComboBox(findKecks(loaded_trunk_path.toPath()).toArray());
				cb.setMaximumSize(new Dimension(1000, 35));
				cb.setEditable(false);
				cb.addActionListener(this);
				keckSelection.add(cb);
				policy_checkbox.addActionListener(this);
				policy_checkbox.setEnabled(false);
				keckSelection.add(policy_checkbox);
				cards.add(keckSelection, KECKPANEL);
			}
		}

		//Allow user to run the program if and only if a valid keck_file_list is chosen from the combobox (dropdown menu)
		if(e.getSource() == cb){
			selected_keck_file_path_name = (String) cb.getSelectedItem();
			
			//If the user selects the default combobox option ("Select Keck File" text)...
			if (cb.getSelectedIndex() == 0){
				
				//Disable the run button, disable run button and clear+disable the policy checkbox
				keck_file_isSelected = false;
				runButton.setEnabled(false);
				policy_checkbox.setEnabled(false);
				policy_checkbox_isChecked = false;
				policy_checkbox.setSelected(false);
				
			}
			//If the user selects anything other than the default combobox option...
			else{
				
				//Enable run button and retrieve part name from the path of the selected keck_file_list
				keck_file_isSelected = true;
				full_part_name = getPartName();
				
				// if it is a vulcan part, differentiate between vulcan_atlas and vulcan_cim by including the substring of full_part_name
				// up to the index of the third "_" (i.e vulcan_cim_engine_lp2_ram_arel -> vulcan_cim_engine)
				if (full_part_name.startsWith("vulcan")){
					short_part_name = full_part_name.substring(0, full_part_name.indexOf("_", full_part_name.indexOf("_", full_part_name.indexOf("_")+1)+1));
				}
				
				// otherwise, include the substring of the full_part_name up until the index of the second "_" (i.e limo_engine_pp1_ram_arel -> limo_engine).
				else
					short_part_name = full_part_name.substring(0, full_part_name.indexOf("_", full_part_name.indexOf("_")+1));
				
				// product name is simply the substring of the full_part_name until the first "_" (i.e limo, bugatti, vulcan)
				product_name = short_part_name.substring(0, short_part_name.indexOf("_"));

				//System.out.println(short_part_name);

				// if the short_part_name happens to be limo, bugatti, triptane, or vulcan_cim engines, then the checkbox to apply the policy filter is enabled
				if ( short_part_name.equals("limo_engine") || short_part_name.equals("bugatti_engine") || short_part_name.equals("triptane_engine") || short_part_name.equals("vulcan_cim_engine")){
					if (short_part_name.equals("triptane_engine")){
						product_name = "pentane";
					}
					policy_checkbox.setEnabled(true);
				}
				// otherwise, disable and uncheck the policy checkbox
				else{
					policy_checkbox.setSelected(false);
					policy_checkbox.setEnabled(false);
					policy_checkbox_isChecked = false;
				}
				// The output folder will be named full_part_name and it will be located in a folder in the user's work directory
				TARGET_FOLDER = new File("/work/SLDB-links-directories/"+full_part_name);
				runButton.setEnabled(true);
			}
			//System.out.println(keck_file_isSelected);
			System.out.println(policy_checkbox_isChecked);
		}

		// if the policy_checkbox is checked, set run mode to apply policy filter
		if(e.getSource() == policy_checkbox){
			policy_checkbox_isChecked = !policy_checkbox_isChecked;
		}

	}

	// make text file of needed policies
	public static File policyFilter(String product_name){

		File policies = new File("/work/sirius/product_name/src/build/subsys_pe_policy.mf");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter("/work/SLDB-links-directories/POLICIES_TO_INCLUDE.txt"));
			policyParser("PE_POLICIES_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_POLICIES += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES += ", bw, policies);
			bw.close();
			return new File("/work/SLDB-links-directories/POLICIES_TO_INCLUDE.txt");
		}catch(Exception e){
			return null;
		}
	}
	
	// make text file of policies to exclude
	public static File policyExcludeFilter(String product_name){

		File policies = new File("/work/sirius/product_name/src/build/subsys_pe_policy.mf");
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter("/work/SLDB-links-directories/POLICIES_TO_EXCLUDE.txt"));
			policyParser("PE_POLICIES_EXCL_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES_EXCL_"+product_name+" += ", bw, policies);
			bw.close();
			return new File("/work/SLDB-links-directories/POLICIES_TO_EXCLUDE.txt");
		}catch(Exception e){
			return null;
		}
	}

	// looks through br, finds every line that starts with source, and writes the tails to bw
	public static void policyParser(String source, BufferedWriter bw, File policies){
		String[] suffixes = {"_cid.h", "_module.c", "_sys.c", "_sys.h", ".c", ".h", ".mf"};
		try{
			BufferedReader br = new BufferedReader(new FileReader(policies));
			String line = null;

			while ((line = br.readLine()) != null) {
				String fileName = null;
				String target = null;

				if (line.startsWith(source)){
					target = line.substring(source.length());
					if (source.contains("_OBJMODULES")){
						if (target.startsWith("$")){
							System.out.println("Parent Source: ");
							System.out.println(target.substring(2,target.indexOf(")"))+" += ");
							policyParser(target.substring(2,target.indexOf(")"))+" += ", bw, policies);
						}
						else{
							fileName = line.substring(source.length());
							System.out.println(fileName);
							for(int i=0; i < suffixes.length; i++){
								System.out.println(fileName+suffixes[i]);
								bw.write(fileName+suffixes[i]+newline);
							}
						}
					}
					else {
						if (target.startsWith("$")){
							System.out.println("Parent Source: ");
							System.out.println(target.substring(2,target.indexOf(")"))+" += ");
							policyParser(target.substring(2,target.indexOf(")"))+" += ", bw, policies);
						}
						else{
							fileName = concatPolicy(target);
							System.out.println(fileName);
							bw.write(fileName+newline);
						}
					}
				}

				//				else if(line.startsWith(objModules)){


			}
			br.close();
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
	}
	public static String concatPolicy(String policy){
		return "pe_policies_policy_"+policy+".c";
	}

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

	// returns true if a given string is found as a line in a text file
	static boolean isInText(String line, File txt){
		try {
			BufferedReader br = new BufferedReader(new FileReader(txt));
			List<String> list = new ArrayList<>();
			String thisLine = null;
			while((thisLine = br.readLine()) != null){
				list.add(thisLine); 
			}
			//System.out.println(Arrays.toString(list.toArray()));
			if(list.contains(line)){
				return true;
			}else{
				return false;
			}
		}catch(Exception e){
			System.out.println("2");
			e.printStackTrace();
			return false;
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

	//removes the trunk path loaded_trunk_path from given path name p
	public static String cutPathPrefix(String p){
		if (p.startsWith(loaded_trunk_path.getPath())){
			return p.substring(loaded_trunk_path.getPath().length());
		}
		return p;
	}

	//Getter method to retrieve chosen keck_file_list
	public static File getKeckChosen(){
		File f = new File(selected_keck_file_path_name);
		return f;
	}

	//Retrieve the full_part_name from the path of the keck_file_list
	public static String getPartName(){
		String keck_list = getKeckChosen().getPath();

		String pathFromObj = keck_list.substring(keck_list.indexOf("obj_")+4);

		int slashIndex = pathFromObj.indexOf("/");

		String name = pathFromObj.substring(0, slashIndex);

		return name;
	}

	
	//Create the GUI and show it.  For thread safety,
	//this method should be invoked from the
	//event dispatch thread.

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