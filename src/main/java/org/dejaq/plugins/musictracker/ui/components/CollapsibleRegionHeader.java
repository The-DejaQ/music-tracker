package org.dejaq.plugins.musictracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;
import net.runelite.client.ui.ColorScheme;

public class CollapsibleRegionHeader extends JPanel
{
	private static final int PERCENTAGE_LABEL_WIDTH_PIXELS = 36;
	private static final int PROGRESS_SECTION_GAP_PIXELS = 6;

	private final String regionName;
	private final Runnable toggleAction;

	@Setter
	@Getter
	private boolean expanded;

	private final JPanel progressSectionPanel;
	private final RegionProgressBar regionProgressBar = new RegionProgressBar();
	private final JLabel percentageLabel = new JLabel("0%", SwingConstants.RIGHT);

	public CollapsibleRegionHeader(String regionName, boolean initiallyExpanded, Runnable toggleAction)
	{
		this.regionName = regionName;
		this.toggleAction = toggleAction;
		this.expanded = initiallyExpanded;

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARKER_GRAY_COLOR);
		setBorder(new EmptyBorder(7, 10, 7, 10));
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel titleLabel = new JLabel(regionName);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
		titleLabel.setForeground(Color.WHITE);
		add(titleLabel, BorderLayout.WEST);

		progressSectionPanel = buildProgressSection();
		add(progressSectionPanel, BorderLayout.EAST);

		MouseAdapter toggleExpandedListener = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				expanded = !expanded;
				if (toggleAction != null)
				{
					toggleAction.run();
				}
			}
		};

		addMouseListener(toggleExpandedListener);
		titleLabel.addMouseListener(toggleExpandedListener);
		progressSectionPanel.addMouseListener(toggleExpandedListener);
		regionProgressBar.addMouseListener(toggleExpandedListener);
		percentageLabel.addMouseListener(toggleExpandedListener);
	}

	private JPanel buildProgressSection()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);
		panel.setAlignmentY(Component.CENTER_ALIGNMENT);

		percentageLabel.setFont(percentageLabel.getFont().deriveFont(Font.PLAIN, 12f));
		percentageLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

		Dimension size = new Dimension(PERCENTAGE_LABEL_WIDTH_PIXELS, percentageLabel.getPreferredSize().height);
		percentageLabel.setPreferredSize(size);
		percentageLabel.setMinimumSize(size);
		percentageLabel.setMaximumSize(size);

		regionProgressBar.setAlignmentY(Component.CENTER_ALIGNMENT);
		percentageLabel.setAlignmentY(Component.CENTER_ALIGNMENT);

		panel.add(regionProgressBar);
		panel.add(Box.createHorizontalStrut(PROGRESS_SECTION_GAP_PIXELS));
		panel.add(percentageLabel);

		return panel;
	}

	public void setShowProgress(boolean show)
	{
		if (progressSectionPanel != null)
		{
			progressSectionPanel.setVisible(show);
		}
	}

	public void setProgress(int unlockedTrackCount, int totalTrackCount)
	{
		int clampedTotal = Math.max(totalTrackCount, 0);
		int clampedUnlocked = Math.max(0, Math.min(unlockedTrackCount, clampedTotal));

		int percent = clampedTotal == 0
			? 100
			: (int) Math.round((clampedUnlocked * 100d) / clampedTotal);

		regionProgressBar.setPercentCompleted(percent);
		percentageLabel.setText(percent + "%");

		String tooltip = clampedTotal == 0
			? "No applicable tracks in this region"
			: clampedUnlocked + " / " + clampedTotal + " tracks unlocked (" + percent + "%)";

		regionProgressBar.setToolTipText(tooltip);
		percentageLabel.setToolTipText(tooltip);
	}
}