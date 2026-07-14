package org.dejaq.plugins.musictracker.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.ui.builder.RouteImporter;
import org.dejaq.plugins.musictracker.ui.components.CollapsibleRegionHeader;
import org.dejaq.plugins.musictracker.ui.components.TrackRowPanel;
import org.dejaq.plugins.musictracker.ui.filter.MembersFilterOption;
import org.dejaq.plugins.musictracker.ui.filter.QuestFilterOption;
import org.dejaq.plugins.musictracker.ui.filter.StatusFilterOption;

public class TrackerContentPanel extends JPanel
{
	private static final int SCROLLBAR_UNIT_INCREMENT_PIXELS = 16;
	private static final String CONFIG_GROUP_NAME = "music-tracker";
	private static final String STATUS_FILTER_CONFIG_KEY = "statusFilter";
	private static final String MEMBERS_FILTER_CONFIG_KEY = "membersFilter";
	private static final String QUESTS_FILTER_CONFIG_KEY = "questsFilter";
	private static final String HIDE_MISSING_LEVEL_CONFIG_KEY = "hideMissingLevel";
	private static final String FILTERS_COLLAPSED_CONFIG_KEY = "filtersCollapsed";
	private static final String TRACKS_CARD_NAME = "TRACKS";
	private static final String EMPTY_STATE_CARD_NAME = "EMPTY_STATE";

	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackManager musicTrackManager;
	private final MusicTrackerConfig musicTrackerConfig;
	private final ConfigManager configManager;

	private final JPanel tracksContainer = new VerticallyScrollableTracksPanel();
	private final JPanel centerCardPanel = new JPanel(new CardLayout());
	private final Map<String, JPanel> regionContentPanels = new HashMap<>();
	private final Map<String, Boolean> regionExpandedStates = new HashMap<>();
	private final Map<String, CollapsibleRegionHeader> regionHeaders = new HashMap<>();

	private JPanel topControlsPanel;
	private JButton trackingToggleButton;
	private JTextField searchTextField;
	private String currentSearchText = "";

	private JComboBox<StatusFilterOption> statusFilterComboBox;
	private JComboBox<MembersFilterOption> membersFilterComboBox;
	private JComboBox<QuestFilterOption> questFilterComboBox;
	private JCheckBox hideMissingLevelsCheckBox;

	private StatusFilterOption currentStatusFilter;
	private MembersFilterOption currentMembersFilter;
	private QuestFilterOption currentQuestFilter;

	private boolean filtersCollapsed;
	private JLabel filtersHeaderLabel;
	private JPanel filtersContentPanel;

	public TrackerContentPanel(MusicTrackerPlugin musicTrackerPlugin, MusicTrackManager musicTrackManager, MusicTrackerConfig musicTrackerConfig, ConfigManager configManager)
	{
		super();
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackManager = musicTrackManager;
		this.musicTrackerConfig = musicTrackerConfig;
		this.configManager = configManager;

		this.currentStatusFilter = parseStatusFilter(musicTrackerConfig.statusFilter());
		this.currentMembersFilter = parseMembersFilter(musicTrackerConfig.membersFilter());
		this.currentQuestFilter = parseQuestFilter(musicTrackerConfig.questsFilter());
		this.filtersCollapsed = musicTrackerConfig.filtersCollapsed();

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
		trackingToggleButton.addActionListener(actionEvent -> musicTrackerPlugin.toggleTracking());
		topControlsPanel.add(wrapCentered(trackingToggleButton));
		topControlsPanel.add(Box.createVerticalStrut(8));

		JButton clearSkippedTracksButton = new JButton("Clear Skipped Tracks");
		clearSkippedTracksButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.clearSkippedTracks();
			refreshVisibleTracks();
		});
		topControlsPanel.add(wrapCentered(clearSkippedTracksButton));
		topControlsPanel.add(Box.createVerticalStrut(8));

		JButton refreshButton = new JButton("Refresh");
		refreshButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.reloadRegions();
			musicTrackerPlugin.refreshAll();
		});
		topControlsPanel.add(wrapCentered(refreshButton));
		topControlsPanel.add(Box.createVerticalStrut(8));

		JButton importRouteButton = new JButton("Import Route");
		importRouteButton.addActionListener(actionEvent -> openImportRouteDialog());
		topControlsPanel.add(wrapCentered(importRouteButton));
		topControlsPanel.add(Box.createVerticalStrut(8));

		topControlsPanel.add(buildFiltersSection());
		topControlsPanel.add(Box.createVerticalStrut(8));

		searchTextField = new JTextField();
		searchTextField.setAlignmentX(Component.LEFT_ALIGNMENT);
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

	private JPanel wrapCentered(JButton button)
	{
		JPanel wrapperPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		wrapperPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		wrapperPanel.add(button);
		wrapperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, wrapperPanel.getPreferredSize().height));
		return wrapperPanel;
	}

	private void openImportRouteDialog()
	{
		RouteImporter routeImporter = new RouteImporter(this, musicTrackManager, musicTrackerPlugin.getCustomTrackStore(), () -> {
			musicTrackerPlugin.refreshAll();
			refreshVisibleTracks();
		});
		routeImporter.openImportDialog();
	}

	private JPanel buildFiltersSection()
	{
		JPanel sectionPanel = new JPanel();
		sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
		sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

		sectionPanel.add(buildFiltersHeaderPanel());
		sectionPanel.add(Box.createVerticalStrut(4));

		filtersContentPanel = buildFiltersPanel();
		filtersContentPanel.setVisible(!filtersCollapsed);
		sectionPanel.add(filtersContentPanel);

		updateFiltersHeaderText();

		return sectionPanel;
	}

	private JPanel buildFiltersHeaderPanel()
	{
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		headerPanel.setBorder(new EmptyBorder(6, 8, 6, 8));
		headerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

		filtersHeaderLabel = new JLabel();
		filtersHeaderLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
		filtersHeaderLabel.setForeground(Color.WHITE);
		headerPanel.add(filtersHeaderLabel, BorderLayout.WEST);

		MouseAdapter toggleFiltersCollapsedListener = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				onFiltersCollapsedToggled();
			}
		};
		headerPanel.addMouseListener(toggleFiltersCollapsedListener);
		filtersHeaderLabel.addMouseListener(toggleFiltersCollapsedListener);

		updateFiltersHeaderText();

		return headerPanel;
	}

	private void onFiltersCollapsedToggled()
	{
		filtersCollapsed = !filtersCollapsed;
		configManager.setConfiguration(CONFIG_GROUP_NAME, FILTERS_COLLAPSED_CONFIG_KEY, filtersCollapsed);
		filtersContentPanel.setVisible(!filtersCollapsed);
		updateFiltersHeaderText();
		revalidate();
		repaint();
	}

	private void updateFiltersHeaderText()
	{
		if (filtersHeaderLabel == null)
		{
			return;
		}
		String arrowGlyph = filtersCollapsed ? "\u25B6" : "\u25BC";
		filtersHeaderLabel.setText("Filters (" + computeActiveFilterCount() + " active) " + arrowGlyph);
	}

	private int computeActiveFilterCount()
	{
		int activeFilterCount = 0;
		if (currentStatusFilter != StatusFilterOption.ALL)
		{
			activeFilterCount++;
		}
		if (currentMembersFilter != MembersFilterOption.ALL)
		{
			activeFilterCount++;
		}
		if (currentQuestFilter != QuestFilterOption.ALL)
		{
			activeFilterCount++;
		}
		if (hideMissingLevelsCheckBox != null && hideMissingLevelsCheckBox.isSelected())
		{
			activeFilterCount++;
		}
		return activeFilterCount;
	}

	private JPanel buildFiltersPanel()
	{
		JPanel filtersPanel = new JPanel();
		filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));
		filtersPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		filtersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

		statusFilterComboBox = new JComboBox<>(StatusFilterOption.values());
		statusFilterComboBox.setSelectedItem(currentStatusFilter);
		statusFilterComboBox.addActionListener(actionEvent -> onStatusFilterChanged());
		filtersPanel.add(buildFilterRow("Status:", statusFilterComboBox));
		filtersPanel.add(Box.createVerticalStrut(4));

		membersFilterComboBox = new JComboBox<>(MembersFilterOption.values());
		membersFilterComboBox.setSelectedItem(currentMembersFilter);
		membersFilterComboBox.addActionListener(actionEvent -> onMembersFilterChanged());
		filtersPanel.add(buildFilterRow("Members:", membersFilterComboBox));
		filtersPanel.add(Box.createVerticalStrut(4));

		questFilterComboBox = new JComboBox<>(QuestFilterOption.values());
		questFilterComboBox.setSelectedItem(currentQuestFilter);
		questFilterComboBox.addActionListener(actionEvent -> onQuestFilterChanged());
		filtersPanel.add(buildFilterRow("Quests:", questFilterComboBox));
		filtersPanel.add(Box.createVerticalStrut(4));

		hideMissingLevelsCheckBox = new JCheckBox();
		hideMissingLevelsCheckBox.setSelected(musicTrackerConfig.hideMissingLevel());
		hideMissingLevelsCheckBox.setOpaque(false);
		hideMissingLevelsCheckBox.addActionListener(actionEvent -> onHideMissingLevelsChanged());
		filtersPanel.add(buildFilterRow("Hide Missing Levels:", hideMissingLevelsCheckBox));

		return filtersPanel;
	}

	private JPanel buildFilterRow(String labelText, JComponent control)
	{
		JPanel rowPanel = new JPanel(new BorderLayout(6, 0));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JLabel rowLabel = new JLabel(labelText);
		rowPanel.add(rowLabel, BorderLayout.CENTER);
		rowPanel.add(control, BorderLayout.EAST);

		return rowPanel;
	}

	private void onStatusFilterChanged()
	{
		StatusFilterOption selectedFilter = (StatusFilterOption) statusFilterComboBox.getSelectedItem();
		currentStatusFilter = selectedFilter != null ? selectedFilter : StatusFilterOption.ALL;
		configManager.setConfiguration(CONFIG_GROUP_NAME, STATUS_FILTER_CONFIG_KEY, currentStatusFilter.name());
		updateFiltersHeaderText();
		refreshVisibleTracks();
	}

	private void onMembersFilterChanged()
	{
		MembersFilterOption selectedFilter = (MembersFilterOption) membersFilterComboBox.getSelectedItem();
		currentMembersFilter = selectedFilter != null ? selectedFilter : MembersFilterOption.ALL;
		configManager.setConfiguration(CONFIG_GROUP_NAME, MEMBERS_FILTER_CONFIG_KEY, currentMembersFilter.name());
		updateFiltersHeaderText();
		refreshVisibleTracks();
	}

	private void onQuestFilterChanged()
	{
		QuestFilterOption selectedFilter = (QuestFilterOption) questFilterComboBox.getSelectedItem();
		currentQuestFilter = selectedFilter != null ? selectedFilter : QuestFilterOption.ALL;
		configManager.setConfiguration(CONFIG_GROUP_NAME, QUESTS_FILTER_CONFIG_KEY, currentQuestFilter.name());
		updateFiltersHeaderText();
		refreshVisibleTracks();
	}

	private void onHideMissingLevelsChanged()
	{
		configManager.setConfiguration(CONFIG_GROUP_NAME, HIDE_MISSING_LEVEL_CONFIG_KEY, hideMissingLevelsCheckBox.isSelected());
		updateFiltersHeaderText();
		refreshVisibleTracks();
	}

	private StatusFilterOption parseStatusFilter(String rawValue)
	{
		try
		{
			return StatusFilterOption.valueOf(rawValue);
		}
		catch (Exception exception)
		{
			return StatusFilterOption.ALL;
		}
	}

	private MembersFilterOption parseMembersFilter(String rawValue)
	{
		try
		{
			return MembersFilterOption.valueOf(rawValue);
		}
		catch (Exception exception)
		{
			return MembersFilterOption.ALL;
		}
	}

	private QuestFilterOption parseQuestFilter(String rawValue)
	{
		try
		{
			return QuestFilterOption.valueOf(rawValue);
		}
		catch (Exception exception)
		{
			return QuestFilterOption.ALL;
		}
	}

	public void buildTracksList()
	{
		tracksContainer.removeAll();
		regionContentPanels.clear();
		regionHeaders.clear();

		tracksContainer.setLayout(new BoxLayout(tracksContainer, BoxLayout.Y_AXIS));
		tracksContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

		for (String regionName : musicTrackManager.getRegionNames())
		{
			boolean isRegionExpanded = regionExpandedStates.getOrDefault(regionName, false);

			CollapsibleRegionHeader regionHeader = new CollapsibleRegionHeader(regionName, isRegionExpanded, () -> toggleRegionExpanded(regionName));
			regionHeader.setShowProgress(musicTrackerPlugin.getMusicTrackerConfig().showProgress());
			regionHeader.setBorder(new CompoundBorder(new EmptyBorder(4, 0, 0, 0), regionHeader.getBorder()));

			regionHeaders.put(regionName, regionHeader);
			tracksContainer.add(regionHeader);

			JPanel regionContentPanel = new JPanel()
			{
				@Override
				public Dimension getMaximumSize()
				{
					return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
				}
			};
			regionContentPanel.setLayout(new BoxLayout(regionContentPanel, BoxLayout.Y_AXIS));
			regionContentPanel.setBorder(new EmptyBorder(0, 12, 4, 4));
			regionContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

			regionContentPanels.put(regionName, regionContentPanel);

			tracksContainer.add(regionContentPanel);
		}

		centerCardPanel.removeAll();
		centerCardPanel.add(tracksContainer, TRACKS_CARD_NAME);
		centerCardPanel.add(buildEmptyStateLabel(), EMPTY_STATE_CARD_NAME);

		removeAll();
		add(topControlsPanel, BorderLayout.NORTH);
		add(centerCardPanel, BorderLayout.CENTER);

		filterTracks(currentSearchText);
		updateAllRegionProgress();

		revalidate();
		repaint();
	}

	private JLabel buildEmptyStateLabel()
	{
		JLabel emptyStateLabel = new JLabel(
			"<html><div style='text-align:center;width:160px;'>No Music Tracks found. Please adjust your filters to show available tracks.</div></html>",
			SwingConstants.CENTER);
		emptyStateLabel.setHorizontalAlignment(SwingConstants.CENTER);
		emptyStateLabel.setVerticalAlignment(SwingConstants.TOP);
		emptyStateLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		emptyStateLabel.setBorder(new EmptyBorder(24, 12, 24, 12));
		return emptyStateLabel;
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

		boolean anyTrackVisibleAcrossAllRegions = false;

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
				if (matchesSearchText && passesFilters(musicTrack))
				{
					regionHasVisibleMatch = true;
					regionContentPanel.add(new TrackRowPanel(musicTrack, musicTrackerPlugin));
				}
			}

			if (regionHasVisibleMatch)
			{
				anyTrackVisibleAcrossAllRegions = true;
			}

			if (regionHeader != null)
			{
				regionHeader.setVisible(regionHasVisibleMatch);
			}

			if (!regionHasVisibleMatch)
			{
				regionContentPanel.setVisible(false);
			}
			else if (isSearching)
			{
				regionContentPanel.setVisible(true);
			}
			else
			{
				boolean wasRegionExpanded = regionExpandedStates.getOrDefault(regionName, false);
				regionContentPanel.setVisible(wasRegionExpanded);
			}

			regionContentPanel.revalidate();
		}

		CardLayout centerCardLayout = (CardLayout) centerCardPanel.getLayout();
		centerCardLayout.show(centerCardPanel, anyTrackVisibleAcrossAllRegions ? TRACKS_CARD_NAME : EMPTY_STATE_CARD_NAME);

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

	private boolean passesFilters(MusicTrack musicTrack)
	{
		if (!passesStatusFilter(musicTrack))
		{
			return false;
		}
		if (!passesMembersFilter(musicTrack))
		{
			return false;
		}
		if (!passesQuestFilter(musicTrack))
		{
			return false;
		}
		if (hideMissingLevelsCheckBox.isSelected() && isMissingRequiredLevel(musicTrack))
		{
			return false;
		}
		return true;
	}

	private boolean passesStatusFilter(MusicTrack musicTrack)
	{
		switch (currentStatusFilter)
		{
			case UNLOCKED:
				return musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle());
			case LOCKED:
				return !musicTrackerPlugin.isTrackUnlocked(musicTrack.getTitle());
			default:
				return true;
		}
	}

	private boolean passesMembersFilter(MusicTrack musicTrack)
	{
		switch (currentMembersFilter)
		{
			case MEMBERS:
				return musicTrack.isMembers();
			case FREE:
				return !musicTrack.isMembers();
			default:
				return true;
		}
	}

	private boolean passesQuestFilter(MusicTrack musicTrack)
	{
		switch (currentQuestFilter)
		{
			case UNLOCKED_BY:
				return musicTrack.getUnlockType() == UnlockType.NORMAL && musicTrack.getUnlockQuest() != null;
			case REQUIRED:
				return musicTrack.getUnlockType() == UnlockType.QUEST;
			case NONE:
				return musicTrack.getUnlockQuest() == null;
			default:
				return true;
		}
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