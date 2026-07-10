package org.dejaq.plugins.musictracker.ui.components;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.Route;

public class TrackRowPanel extends JPanel
{
	private static final int ROW_MAXIMUM_HEIGHT_PIXELS = 32;
	private static final int SKIP_CHECKBOX_SIZE_PIXELS = 16;
	private static final String WIKI_BASE_URL = "https://oldschool.runescape.wiki/w/";

	@Getter
	private final MusicTrack musicTrack;
	private final MusicTrackerPlugin musicTrackerPlugin;

	private JLabel titleLabel;
	private JCheckBox skipTrackCheckBox;
	private JPanel rightSideContainerPanel;

	public TrackRowPanel(MusicTrack musicTrack, MusicTrackerPlugin musicTrackerPlugin)
	{
		this.musicTrack = musicTrack;
		this.musicTrackerPlugin = musicTrackerPlugin;

		setLayout(new BorderLayout(8, 0));
		setBorder(new EmptyBorder(4, 8, 4, 8));
		setOpaque(true);
		setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_MAXIMUM_HEIGHT_PIXELS));
		setAlignmentX(Component.LEFT_ALIGNMENT);

		buildUserInterface();
		updateAppearance();
	}

	private void buildUserInterface()
	{
		titleLabel = new JLabel(musicTrack.getTitle());
		titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		add(titleLabel, BorderLayout.CENTER);

		rightSideContainerPanel = new JPanel(new BorderLayout());
		rightSideContainerPanel.setOpaque(false);
		add(rightSideContainerPanel, BorderLayout.EAST);

		rebuildRightSideContent();

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (SwingUtilities.isLeftMouseButton(mouseEvent))
				{
					musicTrackerPlugin.selectTrack(musicTrack);
				}
			}
		});

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent mouseEvent)
			{
				if (mouseEvent.isPopupTrigger())
				{
					showPopupMenu(mouseEvent);
				}
			}

			@Override
			public void mouseReleased(MouseEvent mouseEvent)
			{
				if (mouseEvent.isPopupTrigger())
				{
					showPopupMenu(mouseEvent);
				}
			}
		});
	}

	private void showPopupMenu(MouseEvent mouseEvent)
	{
		JPopupMenu popupMenu = new JPopupMenu();

		JMenuItem startTrackingMenuItem = new JMenuItem("Start Tracking");
		startTrackingMenuItem.addActionListener(actionEvent -> musicTrackerPlugin.selectTrack(musicTrack));
		popupMenu.add(startTrackingMenuItem);

		if (musicTrack.hasMultipleRoutes())
		{
			Route currentlySelectedRoute = musicTrackerPlugin.getTrackNavigator().getCurrentRoute();
			MusicTrack currentlyTrackedTrack = musicTrackerPlugin.getTrackNavigator().getCurrentTrack();

			for (Route candidateRoute : musicTrack.getAllRoutes())
			{
				JMenuItem routeMenuItem = new JMenuItem("Start " + candidateRoute.getName());

				boolean isCurrentlySelected = currentlyTrackedTrack != null
					&& currentlyTrackedTrack.getTitle() != null
					&& currentlyTrackedTrack.getTitle().equals(musicTrack.getTitle())
					&& currentlySelectedRoute != null
					&& currentlySelectedRoute.getName().equals(candidateRoute.getName());

				if (isCurrentlySelected)
				{
					routeMenuItem.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
				}

				routeMenuItem.addActionListener(actionEvent -> {
					musicTrackerPlugin.selectTrack(musicTrack);
					musicTrackerPlugin.getTrackNavigator().setCurrentRoute(candidateRoute);
				});

				popupMenu.add(routeMenuItem);
			}
		}

		popupMenu.addSeparator();

		JMenuItem openWikiMenuItem = new JMenuItem("Open Wiki");
		openWikiMenuItem.addActionListener(actionEvent -> {
			try
			{
				String wikiUrl = musicTrack.getWikiUrl();
				if (wikiUrl == null || wikiUrl.isBlank())
				{
					wikiUrl = WIKI_BASE_URL + musicTrack.getTitle().replace(" ", "_");
				}
				LinkBrowser.browse(wikiUrl);
			}
			catch (Exception ignored)
			{
			}
		});
		popupMenu.add(openWikiMenuItem);

		popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
	}

	private void rebuildRightSideContent()
	{
		rightSideContainerPanel.removeAll();

		boolean isTrackUnlocked = musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle());

		if (!isTrackUnlocked)
		{
			skipTrackCheckBox = new JCheckBox();
			skipTrackCheckBox.setSelected(musicTrackerPlugin.isSkipped(musicTrack.getTitle()));
			skipTrackCheckBox.setFocusPainted(false);
			skipTrackCheckBox.setOpaque(false);
			skipTrackCheckBox.setPreferredSize(new Dimension(SKIP_CHECKBOX_SIZE_PIXELS, SKIP_CHECKBOX_SIZE_PIXELS));

			skipTrackCheckBox.addActionListener(actionEvent ->
				musicTrackerPlugin.setSkipped(musicTrack.getTitle(), skipTrackCheckBox.isSelected()));

			rightSideContainerPanel.add(skipTrackCheckBox, BorderLayout.CENTER);
		}
		else
		{
			skipTrackCheckBox = null;
		}

		rightSideContainerPanel.revalidate();
		rightSideContainerPanel.repaint();
	}

	public void updateAppearance()
	{
		boolean isTrackUnlocked = musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle());
		boolean isTrackSkipped = musicTrackerPlugin.isSkipped(musicTrack.getTitle());
		boolean isCurrentlyTrackedTrack = musicTrackerPlugin.getTrackNavigator().getCurrentTrack() != null
			&& musicTrackerPlugin.getTrackNavigator().getCurrentTrack().equals(musicTrack);

		titleLabel.setText(musicTrack.getTitle());

		if (isTrackSkipped)
		{
			titleLabel.setForeground(ColorScheme.MEDIUM_GRAY_COLOR);
		}
		else if (isCurrentlyTrackedTrack)
		{
			titleLabel.setForeground(ColorScheme.PROGRESS_INPROGRESS_COLOR);
		}
		else if (isTrackUnlocked)
		{
			titleLabel.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
		}
		else
		{
			titleLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
		}

		boolean shouldHaveSkipCheckbox = !isTrackUnlocked;
		boolean currentlyHasSkipCheckbox = (skipTrackCheckBox != null);

		if (shouldHaveSkipCheckbox != currentlyHasSkipCheckbox)
		{
			rebuildRightSideContent();
		}
		else if (skipTrackCheckBox != null)
		{
			skipTrackCheckBox.setSelected(isTrackSkipped);
		}
	}
}