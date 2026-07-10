package org.dejaq.plugins.musictracker.ui.state;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class LoggedOutPanel extends JPanel
{

	public LoggedOutPanel()
	{
		JLabel label = new JLabel("Please log in to use Music Tracker.");
		label.setForeground(Color.LIGHT_GRAY);
		add(label);
	}
}