package wizardDialog;

/*
 * CardLayoutDemo.java
 *
 */
import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.*;

import linkFileList.FileChooserGUI;

public class WizardDialog extends JDialog implements ActionListener{
	JPanel cards; //a panel that uses CardLayout
	final static String TRUNKPANEL = "trunk select";
	final static String KECKPANEL = "keck file select";

	static private final String newline = "\n";

	boolean trunkLoaded = false;
	boolean keckSelected = false;

	static JFrame frame;
	File trunkPath;
	JFileChooser fc;
	JTextArea log;
	JPanel trunkSelection, keckSelection;

	JButton cancelButton = new JButton("Cancel");
	JButton backButton = new JButton("Back");
	JButton nextButton = new JButton("Next");
	JButton finishButton = new JButton("Finish");
	JButton loadButton = new JButton("Load...");

	JComboBox cb;

	static String keckChosen = "";

	public ArrayList<String> findKecks(Path trunkLoc, JTextArea log) {

		String path = trunkLoc+"/opkgbuild/";
		String[] command = {"find", path, "-name", "keck_file_list"};
		ArrayList<String> kecks = new ArrayList<String>();
		kecks.add("Select a keck_file_list");
		String s = null;

		try {
			Process p = Runtime.getRuntime().exec(command);

			BufferedReader stdInput = new BufferedReader(new 
					InputStreamReader(p.getInputStream()));

			BufferedReader stdError = new BufferedReader(new 
					InputStreamReader(p.getErrorStream()));

			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				kecks.add(s);
			}


			//System.exit(0);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}

		return kecks;
	}

	public void addComponentToPane(Container pane) {

		//Put the JComboBox in a JPanel to get a nicer look.
		JPanel wizardShell = new JPanel(); //use FlowLayout
		cancelButton.addActionListener(this);
		backButton.addActionListener(this);
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);
		wizardShell.add(cancelButton);
		wizardShell.add(backButton);
		backButton.setEnabled(false);
		wizardShell.add(nextButton);
		wizardShell.add(finishButton);
		finishButton.setEnabled(false);
		finishButton.addActionListener(this);

		//String comboBoxItems[] = { TRUNKPANEL, KECKPANEL };
		//JComboBox cb = new JComboBox(comboBoxItems);
		//cb.setEditable(false);
		//cb.addItemListener(this);
		//keckSelection.add(cb);

		fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setCurrentDirectory(new File("/work") );

		trunkSelection = new JPanel();
		trunkSelection.setLayout(new BoxLayout(trunkSelection, BoxLayout.Y_AXIS));
		JPanel start = new JPanel();
		log = new JTextArea(3, 60);
		log.setMargin(new Insets(5,5,5,5));
		log.setEditable(false);
		log.append("No folder selected.");


		loadButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		loadButton.addActionListener(this);

		start.add(new JLabel ("Please Locate Your Sirius Trunk"));
		trunkSelection.add(log);
		trunkSelection.add(start);
		trunkSelection.add(loadButton);



		//Create the panel that contains the "cards".
		cards = new JPanel(new CardLayout());
		cards.add(trunkSelection, TRUNKPANEL);


		pane.add(cards, BorderLayout.CENTER);
		pane.add(wizardShell, BorderLayout.PAGE_END);

	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		if (keckSelected == true){
			finishButton.setEnabled(true);
		}

		if (e.getSource() == cancelButton){
			frame.dispose();
		}

		if (e.getSource() == finishButton){
			//System.out.println(keckChosen);
			frame.dispose();
		}

		if (e.getSource() == backButton){
			backButton.setEnabled(false);
			nextButton.setEnabled(true);
			finishButton.setEnabled(false);
			CardLayout cl = (CardLayout)(cards.getLayout());
			cl.show(cards, TRUNKPANEL);
		}

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

		else if (e.getSource() == loadButton){

			int returnVal = fc.showOpenDialog(trunkSelection); //Open file chooser
			if (returnVal == JFileChooser.APPROVE_OPTION) {

				trunkPath = fc.getSelectedFile();
				log.setText("");
				log.append("Folder selected: " + trunkPath.getPath()+ newline);
				if (!errorPrintPathCheck(trunkPath.toPath())){
					nextButton.setEnabled(false);
					log.append("Please recheck your sirius trunk path");
					trunkLoaded = false;
				}
				else{
					trunkLoaded = true;
				}
			}
			else{
				log.setText("");
				log.append("No folder selected." + newline);
			}

			if(trunkLoaded){
				nextButton.setEnabled(true);
				log.setText("");
				log.append("Folder selected: " + trunkPath.getPath()+ newline);

				keckSelection = new JPanel(); //use FlowLayout
				keckSelection.setLayout(new BoxLayout(keckSelection, BoxLayout.Y_AXIS));

				JLabel keckSelectText = new JLabel("Select your keck_file_list:"+newline);
				keckSelectText.setAlignmentX(Component.CENTER_ALIGNMENT);
				keckSelection.add(keckSelectText);
				cb = new JComboBox(findKecks(trunkPath.toPath(), log).toArray());
				cb.setEditable(false);
				cb.addActionListener(this);
				//cb.addItemListener(this);
				keckSelection.add(cb);
				cards.add(keckSelection, KECKPANEL);
			}
		}

		if(e.getSource() == cb){
			keckChosen = (String) cb.getSelectedItem();
			System.out.println("selected:" + keckChosen);
			if (cb.getSelectedIndex() == 0){
				keckSelected = false;
			}
			else
				keckSelected = true;
			//System.out.println(keckSelected);
		}

	}


	public static boolean errorPrintPathCheck(Path trunk){
		String path = trunk+"/opkgbuild";
		File toCheck = new File(path);
		return toCheck.exists();
	}

	public File getKeckChosen(){
		File f = new File(keckChosen);
		return f;
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
			UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
		} catch (UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		} catch (IllegalAccessException ex) {
			ex.printStackTrace();
		} catch (InstantiationException ex) {
			ex.printStackTrace();
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		/* Turn off metal's use of bold fonts */
		UIManager.put("swing.boldMetal", Boolean.FALSE);

		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
				System.out.println("here" + keckChosen);
			}
		});
		System.out.println("here" + keckChosen);
	}

}