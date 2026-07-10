package org.dejaq.plugins.musictracker.ui.state;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;

public class SettingsRequiredPanel extends JPanel
{
	private static final int SETTINGS_LABEL_FONT_SIZE = 16;
	private static final int SETTINGS_PANEL_TOP_SPACING_PIXELS = 16;

	@Getter
	private final JLabel musicUnlockLabel;
	@Getter
	private final JLabel musicAreaTypeLabel;

	public SettingsRequiredPanel()
	{
		JTextArea warningTextArea = new JTextArea(
			"Cannot start Music Tracker.\nPlease enable these settings:\n(Wrench → All Settings)"
		);
		warningTextArea.setForeground(Color.LIGHT_GRAY);
		warningTextArea.setEditable(false);
		add(warningTextArea);

		JPanel requiredSettingsPanel = new JPanel(new GridLayout(0, 1));
		requiredSettingsPanel.add(Box.createVerticalStrut(SETTINGS_PANEL_TOP_SPACING_PIXELS));

		musicUnlockLabel = new JLabel("Music unlock message");
		musicUnlockLabel.setFont(musicUnlockLabel.getFont().deriveFont(Font.BOLD, (float) SETTINGS_LABEL_FONT_SIZE));
		musicUnlockLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		requiredSettingsPanel.add(musicUnlockLabel);

		musicAreaTypeLabel = new JLabel("Music area type: Modern");
		musicAreaTypeLabel.setFont(musicAreaTypeLabel.getFont().deriveFont(Font.BOLD, (float) SETTINGS_LABEL_FONT_SIZE));
		musicAreaTypeLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		requiredSettingsPanel.add(musicAreaTypeLabel);

		add(requiredSettingsPanel);
	}

	public void update(MusicTrackerPlugin musicTrackerPlugin)
	{
		boolean isMusicUnlockTextToggleEnabled = musicTrackerPlugin.getPlayerState().getVarbitData()
			.getOrDefault(VarbitID.MUSIC_UNLOCK_TEXT_TOGGLE, false);
		musicUnlockLabel.setForeground(isMusicUnlockTextToggleEnabled
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR);

		boolean isMusicAreaModeEnabled = musicTrackerPlugin.getPlayerState().getVarbitData()
			.getOrDefault(VarbitID.MUSIC_AREA_MODE, false);
		musicAreaTypeLabel.setForeground(isMusicAreaModeEnabled
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR);
	}
}