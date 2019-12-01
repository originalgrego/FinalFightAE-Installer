package com.feverbrainstudios.ffae_installer;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

public class FFAEInstaller {
	public static void main(String[] arg) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e3) {
			e3.printStackTrace();
		} catch (InstantiationException e3) {
			e3.printStackTrace();
		} catch (IllegalAccessException e3) {
			e3.printStackTrace();
		} catch (UnsupportedLookAndFeelException e3) {
			e3.printStackTrace();
		}
		
		JFrame window = new JFrame("FFAE Installer");
		window.setLayout(new GridLayout(1,2));
		
		JPanel leftPanel = new JPanel();
		leftPanel.setLayout(new GridLayout(1,2));
		window.add(leftPanel);

		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new GridLayout(1,2));
		window.add(rightPanel);

		JTextField zipPathTextField = new JTextField();
		zipPathTextField.setSize(460, 24);
		leftPanel.add(zipPathTextField);

		JButton selectZipButton = new JButton("Select Zip");
		rightPanel.add(selectZipButton);
		
		JButton patchButton = new JButton("Patch");
		rightPanel.add(patchButton);

		selectZipButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser zipChooser = new JFileChooser();
				zipChooser.setFileFilter(ZIP_FILTER);
				int returnVal = zipChooser.showOpenDialog(window);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
				  File file = zipChooser.getSelectedFile();
				  zipPathTextField.setText(file.getAbsolutePath());
				}
			}
		});	
		
		patchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				File file = new File(zipPathTextField.getText());
				if (!ZIP_FILTER.accept(file)) {
					JOptionPane.showMessageDialog(window, "Please select a valid zip file.");
					return;
				} else if (!file.exists()) {
					JOptionPane.showMessageDialog(window, "File does not exist.");
					return;
				}
				
				try {
					execAndPrintToConsole("make_work_directory.bat");
					
					String workDirString = System.getProperty("user.dir") + "\\build1234abcd\\";
					
					execAndPrintToConsole("java -jar RomMangler.jar unzip " + file.getAbsolutePath() + " " + workDirString);
					
					String invalidCRCs = checkCRCs(workDirString);
					
					if (!invalidCRCs.isEmpty()) {
						JOptionPane.showMessageDialog(window, "The following CRCs were incorrect:\r\n" + invalidCRCs);
						execAndPrintToConsole("delete_work_directory.bat");
						return;
					}
					
					execAndPrintToConsole("java -jar RomMangler.jar combine final_fight_split.cfg " + workDirString + "ffight.bin");
					execAndPrintToConsole("java -jar RomMangler.jar combine final_fight_gfx_split.cfg " + workDirString + "ffight_gfx.bin");

					execAndPrintToConsole("liteips.exe ffight_hack.ips " + workDirString + "ffight.bin");
					execAndPrintToConsole("liteips.exe ffight_gfx_new.ips " + workDirString + "ffight_gfx.bin");
					
					execAndPrintToConsole("java -jar RomMangler.jar split final_fight_out_split.cfg " + workDirString + "ffight.bin");
					execAndPrintToConsole("java -jar RomMangler.jar split final_fight_gfx_split.cfg " + workDirString + "ffight_gfx.bin");
					
					execAndPrintToConsole("delete_left_overs.bat");
					
					execAndPrintToConsole("java -jar RomMangler.jar zipdir " + workDirString + " " + file.getParent() + "\\ffightae.zip");
					
					execAndPrintToConsole("delete_work_directory.bat");
					
					JOptionPane.showMessageDialog(window, "Patch created successfully!\r\n\r\nThe patch is located:\r\n\r\n" + file.getParent() + "\\ffightae.zip"); 
				} catch (IOException e1) {
					e1.printStackTrace();
					JOptionPane.showMessageDialog(window, "There was an error, please check the console.");
					try {
						execAndPrintToConsole("delete_work_directory.bat");
					} catch (IOException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
				}
			}
		});
		  
		window.addWindowListener(new WindowListener() {
			@Override
			public void windowOpened(WindowEvent e) {}
			@Override
			public void windowIconified(WindowEvent e) {}
			@Override
			public void windowDeiconified(WindowEvent e) {}
			@Override
			public void windowDeactivated(WindowEvent e) {}
			@Override
			public void windowClosing(WindowEvent e) { System.exit(0); }
			@Override
			public void windowClosed(WindowEvent e) {}
			@Override
			public void windowActivated(WindowEvent e) {}
		});

		window.setVisible(true);
		window.setSize(600, 56);
		window.setResizable(false);
	}
	
	private static String checkCRCs(String workDirString) throws IOException {
		String results = "";
		
		for (Map.Entry<String, String> entry: ROM_CRCS.entrySet()) {
			String crcResults = execAndReturn("crc32.exe " + workDirString + entry.getKey());
			
			if (!crcResults.contains(entry.getValue())) {
				results += entry.getKey() + " had CRC " + crcResults.substring(0, 10) + " should be " + entry.getValue() + "\r\n";
			}
		}
		
		return results;
	}
	
	private static void execAndPrintToConsole(String command) throws IOException {
		Process exec = Runtime.getRuntime().exec(command);
		InputStreamReader reader = new InputStreamReader(exec.getInputStream());
		BufferedReader buffReader = new BufferedReader(reader);
		String read = buffReader.readLine();
		while(read != null) {
			System.out.println(read);
			read = buffReader.readLine();
		}
	}
	
	private static String execAndReturn(String command) throws IOException {
		String results = "";
		Process exec = Runtime.getRuntime().exec(command);
		InputStreamReader reader = new InputStreamReader(exec.getInputStream());
		BufferedReader buffReader = new BufferedReader(reader);
		String read = buffReader.readLine();
		while(read != null) {
			results += read;
			read = buffReader.readLine();
		}
		return results;
	}
	
	private static final Map<String, String> ROM_CRCS = new HashMap<String, String>() {{
        // Program
		put("ff_36.11f",  "0xF9A5CE83");
        put("ff_42.11h",  "0x65F11215");
        put("ff_37.12f",  "0xE1033784");
        put("ffe_43.12h", "0x995E968A");
        put("ff-32m.8h",  "0xC747696E");

        // Graphics
        put("ff-5m.7a", "0x9C284108");
        put("ff-7m.9a", "0xA7584DFB");
        put("ff-1m.3a", "0x0B605E44");
        put("ff-3m.5a", "0x52291CD2");
	}};
	
	
	private static FileFilter ZIP_FILTER = new FileFilter() {
		
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return "Zip File";
		}
		
		@Override
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().contains(".zip"); 
		}
	};
}
