/*
 * SymLink Directory Builder Tool 1.5
 * 
 * Creates a directory of symbolic links to only the files in your 
 * Sirius repo from a keck_file_list.
 * 
 * Created by:	Jimmy Shih
 * Date: 		6/29/2017
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
import java.util.Enumeration;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class WizardDialog extends JDialog implements ActionListener{

	// Class Variables
	final static String KECKPANEL = "keck file select";						//"
	static private final String newline = "\n";								//Newline constant for readability
	//boolean trunk_path_isLoaded = false;									//Controls wizard flow based on
	boolean keck_file_isSelected = false;									//steps completed by user.
	static JFrame frame;													//Parent Container.
	//static File loaded_trunk_path;										//Stores user entered trunk path.
	static File keck_file;
	static String selected_keck_file_path_name = "";						//Location of 
	String full_part_name;													//Part Name (i.e. limo_engine_pp1_ram_arel)
	String short_part_name;													//Shortened part name (i.e. limo_engine)
	String product_name;													//General product name (i.e. limo)
	File TARGET_FOLDER;														//Where symbolic links will be placed.
	File POLICIES_TO_INCLUDE;												//A text file of policies to explicitly include
	File POLICIES_TO_EXCLUDE;												//A text file of policies to explicitly exclude
	JFileChooser trunk_selection_fc;										//File chooser to select trunk path.
	JFileChooser keck_selection_fc;											//File chooser to directly select keck_file_list
	JTextArea log;															//Information display on processes
	JPanel trunkSelection, keckSelection, buttonPanel, labelPanel, cards;	//Panels that populate the container
	JComboBox<?> cb;														//Combobox (dropdown menu) for keck select
	static String policy_mf_location;										//Location of the subsys_pe_policies.mf, which determines which policy files to include or exclude
	
	// Button Definitions
	JButton cancelButton = new JButton("Exit");								//Button for closing program
	JButton runButton = new JButton("Run");									//Button for program execution

	//Checkbox definitions
	JCheckBox policy_checkbox = new JCheckBox("(optional) Filter policies if available");
	boolean policy_checkbox_isChecked = false; 


	//Setup wizard GUI. Populate parent container with panels.
	public void addComponentToPane(Container pane) {

		//Log Panel for information output
		log = new JTextArea(4, 60);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		log.append("---------------------------------------------------------------"+newline);
		log.append("Welcome to the Symkink Directory Builder Tool."+newline);
		log.append("---------------------------------------------------------------"+newline);
		log.append("To get started, please select a keck_file_list from the following list: ");
		JScrollPane logScrollPane = new JScrollPane(log);
		logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		logScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		//Button Panel for wizard navigation and operation
		buttonPanel = new JPanel();
		
		cancelButton.addActionListener(this);
		cancelButton.setToolTipText("Close program");
		
		runButton.setEnabled(false);
		runButton.addActionListener(this);
		runButton.setToolTipText("Create Symlink Directory based on selected keck_file_list");
		
		buttonPanel.add(cancelButton);
		buttonPanel.add(runButton);

		//Create KECKPANEL card and populate it with a combobox that contains existing keck_file_lists found in /work
		keckSelection = new JPanel();
		keckSelection.setLayout(new BoxLayout(keckSelection, BoxLayout.Y_AXIS));

		JLabel keckSelectText = new JLabel("Select your keck_file_list:"+newline);
		keckSelectText.setAlignmentX(Component.CENTER_ALIGNMENT);
		keckSelection.add(keckSelectText);
		
		cb = new JComboBox(findKecks().toArray());
		cb.setMaximumSize(new Dimension(1000, 35));
		cb.setEditable(false);
		cb.addActionListener(this);
		
		
		policy_checkbox.addActionListener(this);
		policy_checkbox.setEnabled(false);
		policy_checkbox.setToolTipText("<html>If available for the selected project, you may filter out extraneous policy files.<br>"
										+"Currently supported filters are: limo engine, bugatti engine, triptane engine, and vulcan_cim_engine.");
		
		keckSelection.add(cb);
		keckSelection.add(policy_checkbox);

		//Create the panel that contains the cards. I've reduced the program to a single card KECKPANEL, but there's room for more if needed.
		cards = new JPanel(new CardLayout());
		cards.add(keckSelection, KECKPANEL);
		cards.setBorder(new EmptyBorder(10, 10, 10, 10));

		//Populating the parent container with all of the panels
		pane.add(logScrollPane, BorderLayout.PAGE_START);
		pane.add(cards, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.PAGE_END);

	}

	//Defining button behaviors
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

					findAndSetPolicyMFPath();
					System.out.println("here " + policy_mf_location);
					POLICIES_TO_INCLUDE = policyFilter(product_name);
					POLICIES_TO_EXCLUDE = policyExcludeFilter(product_name);
					
					// Prints contents of selected keck_file_list
					String thisLine = null;
					try {

						// open input stream test.txt for reading purpose.
						BufferedReader br = new BufferedReader(new FileReader(keck_file.getPath()));
						log.append("Creating Symbolic Links from"+keck_file.getPath()+newline);

						// Parse through the keck_file_list and build symlink directory
						while ((thisLine = br.readLine()) != null) {
							Path new_link = Paths.get(TARGET_FOLDER.getPath()+cutPathPrefix(thisLine)); // removing the trunk directory path from the full path
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
					try {
						Desktop.getDesktop().open(new File("/work/SLDB-links-directories"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//infoBox("Please navigate to your target directory at: "+TARGET_FOLDER.getPath()+newline+"You may rename or move the directory wherever you'd like.", "Done");
				}
			});
			//Disallow user to run again while program is in process.
			runButton.setEnabled(false);
			
		}


		//Allow user to run the program if and only if a valid keck_file_list is chosen from the combobox (dropdown menu)
		if (e.getSource() == cb){
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
				keck_file = new File(selected_keck_file_path_name);
				full_part_name = getPartName();
				findAndSetPolicyMFPath();
				
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
				new File("/work/SLDB-links-directories/bin").mkdir();
				runButton.setEnabled(true);
			}
		}

		// if the policy_checkbox is checked, set run mode to apply policy filter
		if(e.getSource() == policy_checkbox){
			policy_checkbox_isChecked = !policy_checkbox_isChecked;
		}

	}

	// make text file of needed policies
	public static File policyFilter(String product_name){

		File policies = new File(policy_mf_location);
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter("/work/SLDB-links-directories/bin/POLICIES_TO_INCLUDE.txt"));
			policyParser("PE_POLICIES_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_POLICIES += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES += ", bw, policies);
			bw.close();
			return new File("/work/SLDB-links-directories/bin/POLICIES_TO_INCLUDE.txt");
		}catch(Exception e){
			return null;
		}
	}
	
	// make text file of policies to exclude
	public static File policyExcludeFilter(String product_name){

		File policies = new File(policy_mf_location);
		try{
			BufferedWriter bw = new BufferedWriter(new FileWriter("/work/SLDB-links-directories/bin/POLICIES_TO_EXCLUDE.txt"));
			policyParser("PE_POLICIES_EXCL_"+product_name+" += ", bw, policies);
			policyParser("PE_POLICY_OBJMODULES_EXCL_"+product_name+" += ", bw, policies);
			bw.close();
			return new File("/work/SLDB-links-directories/bin/POLICIES_TO_EXCLUDE.txt");
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
							//System.out.println("Parent Source: ");
							//System.out.println(target.substring(2,target.indexOf(")"))+" += ");
							policyParser(target.substring(2,target.indexOf(")"))+" += ", bw, policies);
						}
						else{
							fileName = line.substring(source.length());
							//System.out.println(fileName);
							for(int i=0; i < suffixes.length; i++){
								//System.out.println(fileName+suffixes[i]);
								bw.write(fileName+suffixes[i]+newline);
							}
						}
					}
					else {
						if (target.startsWith("$")){
							//System.out.println("Parent Source: ");
							//System.out.println(target.substring(2,target.indexOf(")"))+" += ");
							policyParser(target.substring(2,target.indexOf(")"))+" += ", bw, policies);
						}
						else{
							fileName = concatPolicy(target);
							//System.out.println(fileName);
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
	public ArrayList<String> findKecks() {

		String path = "/work";//trunkLoc+"/";//opkgbuild/";
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
				if (s.endsWith("keck_file_list")){
					kecks.add(s);
				}
			}

		}
		catch (IOException e) {
			log.append("unknown error"+newline);
			e.printStackTrace();
			System.exit(-1);
		}

		return kecks;
	}

	//Method for locating existing keck_file_lists in given trunk path
		public void findAndSetPolicyMFPath() {
			String thisLine = null;
			try {
				// open input stream test.txt for reading purpose.
				BufferedReader br = new BufferedReader(new FileReader(keck_file));
				
				// Parse through the keck_file_list and build symlink directory
				while ((thisLine = br.readLine()) != null) {
					if(thisLine.endsWith("/product/src/build/subsys_pe_policy.mf")){
						policy_mf_location = thisLine;
					}
					
				}
				br.close();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	
	// returns true if a given string is found as a line in given text file
	static boolean isInText(String line, File txt){
		try {
			@SuppressWarnings("resource")
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

			} catch (UnsupportedOperationException x) {

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
		String trunk_path = policy_mf_location.substring(0, policy_mf_location.indexOf("/product/src/build/subsys_pe_policy.mf"));
		if (p.startsWith(trunk_path)){
			return p.substring(trunk_path.length());
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
		String keck_list = keck_file.getPath();

		String pathFromObj = keck_list.substring(keck_list.indexOf("obj_")+4);

		int slashIndex = pathFromObj.indexOf("/");

		String name = pathFromObj.substring(0, slashIndex);

		return name;
	}

	
	//Create the GUI and show it.  For thread safety,
	//this method should be invoked from the
	//event dispatch thread.

	public static void createAndShowGUI() {
		setUIFont(new javax.swing.plaf.FontUIResource("Tahoma",Font.PLAIN,16));
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
	
	
	// sets the font of the UI
	public static void setUIFont (javax.swing.plaf.FontUIResource f){
		Enumeration<Object> keys = UIManager.getDefaults().keys();
	    while (keys.hasMoreElements()) {
	    	Object key = keys.nextElement();
	      Object value = UIManager.get (key);
	      if (value != null && value instanceof javax.swing.plaf.FontUIResource)
	    	  UIManager.put (key, f);
	    }
	} 

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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