package org.dejaq.plugins.musictracker.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.quest.QuestState;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.ui.components.CollapsibleRegionHeader;
import org.dejaq.plugins.musictracker.ui.components.TrackRowPanel;

public class TrackerContentPanel extends JPanel
{
	private static final int SCROLLBAR_UNIT_INCREMENT_PIXELS = 16;

	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackManager musicTrackManager;
	private final MusicTrackerConfig musicTrackerConfig;

	private final JPanel tracksContainer = new VerticallyScrollableTracksPanel();
	private final Map<String, JPanel> regionContentPanels = new HashMap<>();
	private final Map<String, Boolean> regionExpandedStates = new HashMap<>();
	private final Map<String, CollapsibleRegionHeader> regionHeaders = new HashMap<>();

	private JPanel topControlsPanel;
	private JButton trackingToggleButton;
	private JTextField searchTextField;
	private String currentSearchText = "";

	public TrackerContentPanel(MusicTrackerPlugin musicTrackerPlugin, MusicTrackManager musicTrackManager, MusicTrackerConfig musicTrackerConfig)
	{
		super();
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackManager = musicTrackManager;
		this.musicTrackerConfig = musicTrackerConfig;

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(6, 0, 6, 6));

		buildTopControlsPanel();
		buildTracksList();
	}

	private void buildTopControlsPanel()
	{
		topControlsPanel = new JPanel();
		topControlsPanel.setLayout(new BoxLayout(topControlsPanel, BoxLayout.Y_AXIS));
		topControlsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		trackingToggleButton = new JButton("Start Tracking");
		trackingToggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		trackingToggleButton.addActionListener(actionEvent -> musicTrackerPlugin.toggleTracking());
		topControlsPanel.add(trackingToggleButton);
		topControlsPanel.add(Box.createVerticalStrut(8));

		JButton clearSkippedTracksButton = new JButton("Clear Skipped Tracks");
		clearSkippedTracksButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		clearSkippedTracksButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.clearSkippedTracks();
			refreshVisibleTracks();
		});
		topControlsPanel.add(clearSkippedTracksButton);
		topControlsPanel.add(Box.createVerticalStrut(8));

		JButton refreshButton = new JButton("Refresh");
		refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		refreshButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.reloadRegions();
			musicTrackerPlugin.refreshAll();
		});
		topControlsPanel.add(refreshButton);
		topControlsPanel.add(Box.createVerticalStrut(8));

		searchTextField = new JTextField();
		searchTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		searchTextField.putClientProperty("JTextField.placeholderText", "Search tracks...");

		searchTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				filterTracks(searchTextField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				filterTracks(searchTextField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				filterTracks(searchTextField.getText());
			}
		});

		topControlsPanel.add(searchTextField);
		topControlsPanel.add(Box.createVerticalStrut(12));

		add(topControlsPanel, BorderLayout.NORTH);
	}

	public void buildTracksList()
	{
		tracksContainer.removeAll();
		regionContentPanels.clear();
		regionHeaders.clear();

		tracksContainer.setLayout(new BoxLayout(tracksContainer, BoxLayout.Y_AXIS));
		tracksContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

		boolean isFirstRegion = true;

		for (String regionName : musicTrackManager.getRegionNames())
		{
			if (!isFirstRegion)
			{
				tracksContainer.add(Box.createVerticalStrut(4));
			}
			isFirstRegion = false;

			boolean isRegionExpanded = regionExpandedStates.getOrDefault(regionName, false);

			CollapsibleRegionHeader regionHeader = new CollapsibleRegionHeader(regionName, isRegionExpanded, () -> toggleRegionExpanded(regionName));
			regionHeader.setShowProgress(musicTrackerPlugin.getMusicTrackerConfig().showProgress());

			regionHeaders.put(regionName, regionHeader);
			tracksContainer.add(regionHeader);

			JPanel regionContentPanel = new JPanel();
			regionContentPanel.setLayout(new BoxLayout(regionContentPanel, BoxLayout.Y_AXIS));
			regionContentPanel.setBorder(new EmptyBorder(0, 12, 4, 4));
			regionContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			regionContentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

			regionContentPanels.put(regionName, regionContentPanel);

			tracksContainer.add(regionContentPanel);
		}

		removeAll();
		add(topControlsPanel, BorderLayout.NORTH);
		add(tracksContainer, BorderLayout.CENTER);

		filterTracks(currentSearchText);
		updateAllRegionProgress();

		revalidate();
		repaint();
	}

	private void toggleRegionExpanded(String regionName)
	{
		JPanel regionContentPanel = regionContentPanels.get(regionName);
		if (regionContentPanel == null)
		{
			return;
		}

		boolean newExpandedState = !regionExpandedStates.getOrDefault(regionName, false);
		regionExpandedStates.put(regionName, newExpandedState);

		if (newExpandedState)
		{
			collapseAllRegionsExcept(regionName);
		}

		regionContentPanel.setVisible(newExpandedState);
		tracksContainer.revalidate();
		tracksContainer.repaint();
	}

	private void collapseAllRegionsExcept(String regionNameToKeepExpanded)
	{
		for (Map.Entry<String, JPanel> regionContentPanelEntry : regionContentPanels.entrySet())
		{
			if (!regionContentPanelEntry.getKey().equals(regionNameToKeepExpanded))
			{
				regionExpandedStates.put(regionContentPanelEntry.getKey(), false);
				regionContentPanelEntry.getValue().setVisible(false);
			}
		}
	}

	public void updateTrackingButton(boolean trackingActive)
	{
		if (trackingToggleButton != null)
		{
			trackingToggleButton.setText(trackingActive ? "Stop Tracking" : "Start Tracking");
		}
	}

	public void filterTracks(String searchText)
	{
		currentSearchText = searchText == null ? "" : searchText;
		String lowerCaseSearchText = currentSearchText.toLowerCase().trim();
		boolean isSearching = !lowerCaseSearchText.isEmpty();

		for (String regionName : musicTrackManager.getRegionNames())
		{
			JPanel regionContentPanel = regionContentPanels.get(regionName);
			CollapsibleRegionHeader regionHeader = regionHeaders.get(regionName);
			if (regionContentPanel == null)
			{
				continue;
			}

			regionContentPanel.removeAll();
			boolean regionHasVisibleMatch = false;

			for (MusicTrack musicTrack : musicTrackManager.getTracksForRegion(regionName))
			{
				boolean matchesSearchText = !isSearching || musicTrack.getTitle().toLowerCase().contains(lowerCaseSearchText);
				if (matchesSearchText && passesConfigFilters(musicTrack))
				{
					regionHasVisibleMatch = true;
					regionContentPanel.add(new TrackRowPanel(musicTrack, musicTrackerPlugin));
				}
			}

			boolean shouldHideRegionHeader = !regionHasVisibleMatch && musicTrackerConfig.hideFilteredHeaders();

			if (regionHeader != null)
			{
				regionHeader.setVisible(!shouldHideRegionHeader);
			}

			if (shouldHideRegionHeader)
			{
				regionContentPanel.setVisible(false);
			}
			else if (isSearching)
			{
				regionContentPanel.setVisible(regionHasVisibleMatch);
			}
			else
			{
				boolean wasRegionExpanded = regionExpandedStates.getOrDefault(regionName, false);
				regionContentPanel.setVisible(wasRegionExpanded);
			}

			regionContentPanel.revalidate();
		}

		tracksContainer.revalidate();
		tracksContainer.repaint();
	}

	public void clearSearch()
	{
		searchTextField.setText("");
	}

	public void expandRegion(String regionName)
	{
		JPanel regionContentPanel = regionContentPanels.get(regionName);
		if (regionContentPanel == null)
		{
			return;
		}

		boolean regionExpanded = regionExpandedStates.getOrDefault(regionName, false);

		if (!regionExpanded)
		{
			regionExpandedStates.put(regionName, true);
			collapseAllRegionsExcept(regionName);

			regionContentPanel.setVisible(true);
			tracksContainer.revalidate();
			tracksContainer.repaint();
		}
	}

	public void refreshVisibleTracks()
	{
		filterTracks(currentSearchText);
		updateAllRegionProgress();
	}

	private void updateAllRegionProgress()
	{
		for (String regionName : musicTrackManager.getRegionNames())
		{
			updateRegionProgress(regionName);
		}
	}

	private void updateRegionProgress(String regionName)
	{
		CollapsibleRegionHeader regionHeader = regionHeaders.get(regionName);
		if (regionHeader == null)
		{
			return;
		}

		List<MusicTrack> tracksInRegion = musicTrackManager.getTracksForRegion(regionName);
		int totalTrackCount = tracksInRegion.size();

		int unlockedTrackCount = 0;
		for (MusicTrack musicTrack : tracksInRegion)
		{
			if (musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle()))
			{
				unlockedTrackCount++;
			}
		}

		regionHeader.setProgress(unlockedTrackCount, totalTrackCount);
	}

	public void setShowProgress(boolean showProgress)
	{
		for (CollapsibleRegionHeader header : regionHeaders.values())
		{
			if (header != null)
			{
				header.setShowProgress(showProgress);
			}
		}
	}

	private boolean passesConfigFilters(MusicTrack musicTrack)
	{
		if (musicTrackerConfig.hideUnlockedTracks() && musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle()))
		{
			return false;
		}
		if (musicTrackerConfig.hideMemberTracks() && musicTrack.isMembers())
		{
			return false;
		}
		if (musicTrackerConfig.hideMissingQuest() && requiresIncompleteQuest(musicTrack))
		{
			return false;
		}
		if (musicTrackerConfig.hideMissingLevel() && isMissingRequiredLevel(musicTrack))
		{
			return false;
		}
		return true;
	}

	private boolean requiresIncompleteQuest(MusicTrack musicTrack)
	{
		Route defaultRoute = musicTrack.getDefaultRoute();
		if (defaultRoute == null || defaultRoute.getQuest() == null)
		{
			return false;
		}

		QuestState questState = musicTrackerPlugin.getPlayerState().getCachedQuestState(defaultRoute.getQuest(), this::refreshVisibleTracks);
		return questState != QuestState.FINISHED;
	}

	private boolean isMissingRequiredLevel(MusicTrack musicTrack)
	{
		Route defaultRoute = musicTrack.getDefaultRoute();
		if (defaultRoute == null || defaultRoute.getLevels() == null || defaultRoute.getLevels().isEmpty())
		{
			return false;
		}

		for (LevelRequirement levelRequirement : defaultRoute.getLevels())
		{
			int currentSkillLevel = musicTrackerPlugin.getPlayerState().getCachedRealSkillLevel(levelRequirement.getSkill());
			if (currentSkillLevel < levelRequirement.getLevel())
			{
				return true;
			}
		}
		return false;
	}

	private static final class VerticallyScrollableTracksPanel extends JPanel implements Scrollable
	{
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRectangle, int scrollOrientation, int scrollDirection)
		{
			return SCROLLBAR_UNIT_INCREMENT_PIXELS;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRectangle, int scrollOrientation, int scrollDirection)
		{
			return visibleRectangle.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}