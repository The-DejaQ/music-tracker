package org.dejaq.plugins.musictracker.ui.builder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.LinkBrowser;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.json.CustomTrackStore;
import org.dejaq.plugins.musictracker.json.RegionLoader;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.UnlockType;
import org.dejaq.plugins.musictracker.ui.components.CollapsibleRegionHeader;

public class TrackBuilderPanel extends JPanel
{
	private static final boolean DEVELOPER_MODE = true;
	private static final int SCROLLBAR_UNIT_INCREMENT_PIXELS = 16;

	private final MusicTrackerPlugin musicTrackerPlugin;
	private final MusicTrackManager musicTrackManager;
	private final CustomTrackStore customTrackStore;

	private final JPanel tracksContainer = new VerticallyScrollableTracksPanel();
	private final Map<String, JPanel> regionContentPanels = new HashMap<>();
	private final Map<String, Boolean> regionExpandedStates = new HashMap<>();
	private String currentSearchText = "";

	private static final String WIKI_BASE_URL = "https://oldschool.runescape.wiki/w/";

	public TrackBuilderPanel(MusicTrackerPlugin musicTrackerPlugin, MusicTrackManager musicTrackManager)
	{
		super();
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.musicTrackManager = musicTrackManager;
		this.customTrackStore = musicTrackerPlugin.getCustomTrackStore();

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(6, 0, 0, 0));

		add(buildTopControlsPanel(), BorderLayout.NORTH);

		tracksContainer.setLayout(new BoxLayout(tracksContainer, BoxLayout.Y_AXIS));
		tracksContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
		add(tracksContainer, BorderLayout.CENTER);

		refresh();
	}

	private JPanel buildTopControlsPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JButton newTrackButton = new JButton("New Track");
		newTrackButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		newTrackButton.addActionListener(actionEvent -> openNewTrackDialog());
		panel.add(newTrackButton);
		panel.add(Box.createVerticalStrut(6));

		JButton refreshButton = new JButton("Refresh");
		refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		refreshButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.reloadRegions();
			musicTrackerPlugin.refreshAll();
			refresh();
		});
		panel.add(refreshButton);
		panel.add(Box.createVerticalStrut(8));

		JButton clearUnlockedButton = new JButton("Clear Unlocked");
		clearUnlockedButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		clearUnlockedButton.addActionListener(actionEvent -> {
			musicTrackerPlugin.clearUnlockedTracks();
			musicTrackerPlugin.reloadRegions();
			musicTrackerPlugin.refreshAll();
			refresh();
		});
		panel.add(clearUnlockedButton);
		panel.add(Box.createVerticalStrut(8));

		JTextField searchTextField = new JTextField();
		searchTextField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
		searchTextField.putClientProperty("JTextField.placeholderText", "Search tracks...");
		searchTextField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent documentEvent)
			{
				onSearchTextChanged(searchTextField.getText());
			}

			@Override
			public void removeUpdate(DocumentEvent documentEvent)
			{
				onSearchTextChanged(searchTextField.getText());
			}

			@Override
			public void changedUpdate(DocumentEvent documentEvent)
			{
				onSearchTextChanged(searchTextField.getText());
			}
		});
		panel.add(searchTextField);
		panel.add(Box.createVerticalStrut(8));

		return panel;
	}

	private void onSearchTextChanged(String searchText)
	{
		currentSearchText = searchText == null ? "" : searchText.trim().toLowerCase();
		rebuildTracksListByRegion();
	}

	public void refresh()
	{
		rebuildTracksListByRegion();
	}

	public void refreshPreservingExpandedState()
	{
		Set<String> expandedRegions = regionExpandedStates.entrySet().stream()
			.filter(Map.Entry::getValue)
			.map(Map.Entry::getKey)
			.collect(Collectors.toSet());

		musicTrackerPlugin.reloadRegions();
		musicTrackerPlugin.refreshAll();
		rebuildTracksListByRegion();

		for (String regionName : regionContentPanels.keySet())
		{
			boolean shouldExpand = expandedRegions.contains(regionName) &&
				!musicTrackManager.getTracksForRegion(regionName).isEmpty();

			regionExpandedStates.put(regionName, shouldExpand);

			JPanel contentPanel = regionContentPanels.get(regionName);
			if (contentPanel != null)
			{
				contentPanel.setVisible(shouldExpand);
			}
		}

		refresh();
		tracksContainer.revalidate();
		tracksContainer.repaint();
	}

	private void rebuildTracksListByRegion()
	{
		tracksContainer.removeAll();
		regionContentPanels.clear();

		boolean isSearching = !currentSearchText.isEmpty();
		boolean isFirstRegion = true;

		for (String regionName : musicTrackManager.getRegionNames())
		{
			List<MusicTrack> matchingTracks = filterTracksBySearch(musicTrackManager.getTracksForRegion(regionName));
			if (matchingTracks.isEmpty())
			{
				continue;
			}

			boolean isRegionExpanded = isSearching || regionExpandedStates.getOrDefault(regionName, false);

			if (!isFirstRegion)
			{
				tracksContainer.add(Box.createVerticalStrut(4));
			}
			isFirstRegion = false;

			CollapsibleRegionHeader regionHeader = new CollapsibleRegionHeader(regionName, isRegionExpanded, () -> toggleRegionExpanded(regionName));
			regionHeader.setShowProgress(false);

			tracksContainer.add(regionHeader);

			JPanel regionContentPanel = new JPanel();
			regionContentPanel.setLayout(new BoxLayout(regionContentPanel, BoxLayout.Y_AXIS));
			regionContentPanel.setBorder(new EmptyBorder(4, 12, 4, 4));
			regionContentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			regionContentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

			regionContentPanels.put(regionName, regionContentPanel);
			regionContentPanel.setVisible(isRegionExpanded);

			boolean isFirstTrackInRegion = true;
			for (MusicTrack musicTrack : matchingTracks)
			{
				if (!isFirstTrackInRegion)
				{
					regionContentPanel.add(Box.createVerticalStrut(6));
				}
				isFirstTrackInRegion = false;
				regionContentPanel.add(buildTrackRow(musicTrack));
			}

			tracksContainer.add(regionContentPanel);
		}

		tracksContainer.revalidate();
		tracksContainer.repaint();
	}

	private List<MusicTrack> filterTracksBySearch(List<MusicTrack> musicTracks)
	{
		if (currentSearchText.isEmpty())
		{
			return musicTracks;
		}
		List<MusicTrack> matchingTracks = new ArrayList<>();
		for (MusicTrack musicTrack : musicTracks)
		{
			if (musicTrack.getTitle() != null && musicTrack.getTitle().toLowerCase().contains(currentSearchText))
			{
				matchingTracks.add(musicTrack);
			}
		}
		return matchingTracks;
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

	private JPanel buildTrackRow(MusicTrack musicTrack)
	{
		JPanel trackPanel = new JPanel();
		trackPanel.setLayout(new BoxLayout(trackPanel, BoxLayout.Y_AXIS));
		trackPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		trackPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
		trackPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		trackPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

		trackPanel.add(buildTrackHeaderRow(musicTrack));
		trackPanel.add(Box.createVerticalStrut(6));

		List<Route> allRoutes = musicTrack.getAllRoutes();
		if (allRoutes.isEmpty())
		{
			JLabel noRoutesLabel = new JLabel("No routes yet");
			noRoutesLabel.setForeground(Color.GRAY);
			noRoutesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
			trackPanel.add(noRoutesLabel);
		}
		else
		{
			for (Route route : allRoutes)
			{
				trackPanel.add(buildRouteRow(musicTrack, route));
				trackPanel.add(Box.createVerticalStrut(3));
			}
		}

		return trackPanel;
	}

	private JPanel buildTrackHeaderRow(MusicTrack musicTrack)
	{
		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setOpaque(false);
		headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

		JPanel titleAndSubtitlePanel = new JPanel();
		titleAndSubtitlePanel.setLayout(new BoxLayout(titleAndSubtitlePanel, BoxLayout.Y_AXIS));
		titleAndSubtitlePanel.setOpaque(false);

		JLabel titleLabel = new JLabel(musicTrack.getTitle() != null ? musicTrack.getTitle() : "(untitled)");
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
		titleLabel.setForeground(Color.WHITE);
		titleAndSubtitlePanel.add(titleLabel);

		String subtitleText = musicTrack.isCustom() ? "Custom Track" : "Default Track";
		JLabel subtitleLabel = new JLabel(subtitleText);
		subtitleLabel.setFont(subtitleLabel.getFont().deriveFont(11f));
		subtitleLabel.setForeground(musicTrack.isCustom() ? ColorScheme.PROGRESS_INPROGRESS_COLOR : Color.LIGHT_GRAY);
		titleAndSubtitlePanel.add(subtitleLabel);

		headerPanel.add(titleAndSubtitlePanel, BorderLayout.WEST);

		titleLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
		titleLabel.addMouseListener(new java.awt.event.MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (SwingUtilities.isRightMouseButton(e))
				{
					showTrackHeaderPopup(e, musicTrack);
				}
			}
		});

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.setOpaque(false);

		JButton addRouteButton = createSmallButton("Route");
		addRouteButton.addActionListener(actionEvent -> openNewRouteEditor(musicTrack));
		buttonsPanel.add(addRouteButton);

		boolean canEditOrExportThisTrack = musicTrack.isCustom() || DEVELOPER_MODE;

		if (canEditOrExportThisTrack)
		{
			buttonsPanel.add(Box.createHorizontalStrut(3));
			JButton editTrackButton = createSmallButton("Edit");
			editTrackButton.addActionListener(actionEvent -> openEditTrackDialog(musicTrack));
			buttonsPanel.add(editTrackButton);

			buttonsPanel.add(Box.createHorizontalStrut(3));
			JButton exportTrackButton = createSmallButton("Export");
			exportTrackButton.addActionListener(actionEvent -> openExportDialog(musicTrack.getTitle() + " (full track)", customTrackStore.exportTrackToJson(musicTrack)));
			buttonsPanel.add(exportTrackButton);
		}

		if (musicTrack.isCustom())
		{
			buttonsPanel.add(Box.createHorizontalStrut(3));
			JButton deleteTrackButton = createSmallButton("Delete");
			deleteTrackButton.addActionListener(actionEvent -> confirmAndDeleteTrack(musicTrack));
			buttonsPanel.add(deleteTrackButton);
		}

		headerPanel.add(buttonsPanel, BorderLayout.EAST);

		return headerPanel;
	}

	private void showTrackHeaderPopup(MouseEvent mouseEvent, MusicTrack musicTrack)
	{
		JPopupMenu popup = new JPopupMenu();

		boolean isCustomTrack = musicTrack.isCustom();
		boolean canEditOrExportThisTrack = DEVELOPER_MODE || isCustomTrack;

		if (canEditOrExportThisTrack)
		{
			JMenuItem editTrackItem = new JMenuItem("Edit Track");
			editTrackItem.addActionListener(actionEvent -> openEditTrackDialog(musicTrack));
			popup.add(editTrackItem);

			JMenuItem exportTrackItem = new JMenuItem("Export Track");
			exportTrackItem.addActionListener(actionEvent ->
				openExportDialog(musicTrack.getTitle() + " (full track)", customTrackStore.exportTrackToJson(musicTrack)));
			popup.add(exportTrackItem);

			if (isCustomTrack)
			{
				JMenuItem deleteTrackItem = new JMenuItem("Delete Track");
				deleteTrackItem.addActionListener(actionEvent -> confirmAndDeleteTrack(musicTrack));
				popup.add(deleteTrackItem);
			}

			popup.addSeparator();
		}

		JMenuItem setWorldMapPoint = new JMenuItem("Set Path");
		setWorldMapPoint.addActionListener(actionEvent -> {
			musicTrackerPlugin
				.getTrackNavigator()
				.getNavigationCoordinator()
				.updateWorldMapPointForTrack(musicTrack, null);
			Route defaultRoute = musicTrack.getDefaultRoute();

			if (defaultRoute == null)
			{
				musicTrackerPlugin.sendGameMessage(
					"[BUILDER] Could not set path for " + musicTrack.getTitle() + " - no default route");
			}
			else if (defaultRoute.getFinalDestination() == null)
			{
				musicTrackerPlugin.sendGameMessage(
					"[BUILDER] Could not set path for " + musicTrack.getTitle() + " - no final destination");
			}
			else
			{
				WorldPoint destination = defaultRoute.getFinalDestination();
				musicTrackerPlugin.getTrackNavigator()
					.getNavigationCoordinator()
					.updateWorldMapPointForTrack(musicTrack, destination);
				musicTrackerPlugin.getTrackNavigator().getNavigationCoordinator().requestShortestPathTo(destination);

				musicTrackerPlugin.sendGameMessage(
					"[BUILDER] Set path for " + musicTrack.getTitle() + " to " + destination);
			}
		});
		popup.add(setWorldMapPoint);

		JMenuItem newRouteItem = new JMenuItem("New Route");
		newRouteItem.addActionListener(actionEvent -> openNewRouteEditor(musicTrack));
		popup.add(newRouteItem);

		popup.addSeparator();

		JMenuItem openWikiItem = new JMenuItem("Open Wiki");
		openWikiItem.addActionListener(actionEvent -> {
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
		popup.add(openWikiItem);

		popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
	}

	private JPanel buildRouteRow(MusicTrack musicTrack, Route route)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.Y_AXIS));
		rowPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		rowPanel.setBorder(new EmptyBorder(6, 10, 6, 6));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Short.MAX_VALUE));

		StringBuilder labelBuilder = new StringBuilder(route.getName() != null ? route.getName() : "(unnamed route)");
		labelBuilder.append(route.isCustom() ? "  [Custom]" : "  [Default]");
		if (route.isDefaultRoute())
		{
			labelBuilder.append(" \u2605");
		}

		JLabel nameLabel = new JLabel(labelBuilder.toString());
		nameLabel.setForeground(route.isCustom() ? Color.WHITE : Color.LIGHT_GRAY);
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.add(nameLabel);
		rowPanel.add(Box.createVerticalStrut(4));

		JPanel buttonsRow = new JPanel(new BorderLayout());
		buttonsRow.setOpaque(false);
		buttonsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
		buttonsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.X_AXIS));
		buttonsPanel.setOpaque(false);

		if (route.isCustom())
		{
			JButton editButton = createSmallButton("Edit");
			editButton.addActionListener(actionEvent -> openEditRouteEditor(musicTrack, route));
			buttonsPanel.add(editButton);

			buttonsPanel.add(Box.createHorizontalStrut(3));
			JButton exportButton = createSmallButton("Export");
			exportButton.addActionListener(actionEvent -> openExportDialog(route.getName() + " (route)", customTrackStore.exportRouteToJson(route, musicTrack.getTitle())));
			buttonsPanel.add(exportButton);

			buttonsPanel.add(Box.createHorizontalStrut(3));
			JButton deleteButton = createSmallButton("Delete");
			deleteButton.addActionListener(actionEvent -> confirmAndDeleteRoute(musicTrack, route));
			buttonsPanel.add(deleteButton);
		}
		else if (DEVELOPER_MODE)
		{
			JButton editButton = createSmallButton("Edit");
			editButton.addActionListener(actionEvent -> openEditRouteEditor(musicTrack, route));
			buttonsPanel.add(editButton);
		}
		else
		{
			JLabel readOnlyLabel = new JLabel("Read-only");
			readOnlyLabel.setForeground(Color.GRAY);
			readOnlyLabel.setFont(readOnlyLabel.getFont().deriveFont(10f));
			buttonsPanel.add(readOnlyLabel);
		}

		buttonsRow.add(buttonsPanel, BorderLayout.EAST);
		rowPanel.add(buttonsRow);

		return rowPanel;
	}

	private JButton createSmallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(button.getFont().deriveFont(10f));
		button.setMargin(new Insets(2, 4, 2, 4));
		return button;
	}

	private void openNewTrackDialog()
	{
		TrackMetadataDialog dialog = new TrackMetadataDialog(SwingUtilities.getWindowAncestor(this), "New Custom Track", null);
		dialog.setVisible(true);

		if (dialog.wasConfirmed())
		{
			musicTrackManager.createCustomTrack(
				dialog.getTitleValue(), dialog.getRegionValue(), dialog.getLocationValue(), dialog.getWikiUrlValue(),
				dialog.getUnlockTypeValue(), dialog.getUnlockQuestValue(), dialog.getUnlockMessageValue(), dialog.getUnlockHintValue());

			refresh();

		}
	}

	private void openEditTrackDialog(MusicTrack musicTrack)
	{
		TrackMetadataDialog dialog = new TrackMetadataDialog(SwingUtilities.getWindowAncestor(this), "Edit Track", musicTrack);
		dialog.setVisible(true);

		if (!dialog.wasConfirmed())
		{
			return;
		}

		if (musicTrack.isCustom())
		{
			musicTrackManager.updateCustomTrackMetadata(musicTrack,
				dialog.getTitleValue(), dialog.getRegionValue(), dialog.getLocationValue(), dialog.getWikiUrlValue(),
				dialog.getUnlockTypeValue(), dialog.getUnlockQuestValue(), dialog.getUnlockMessageValue(), dialog.getUnlockHintValue());
		}
		else if (DEVELOPER_MODE)
		{
			saveDefaultTrackEdit(musicTrack, dialog);
			refreshPreservingExpandedState();
		}
		else
		{
			return;
		}

		refresh();
	}

	private void confirmAndDeleteTrack(MusicTrack musicTrack)
	{
		int confirmation = JOptionPane.showConfirmDialog(this,
			"Delete custom track \"" + musicTrack.getTitle() + "\" and all its routes?\nThis cannot be undone.",
			"Delete Track", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirmation == JOptionPane.YES_OPTION)
		{
			musicTrackManager.deleteCustomTrack(musicTrack);
			refresh();
		}
	}

	private void openNewRouteEditor(MusicTrack musicTrack)
	{
		Route newRoute = Route.builder().name("New Route").trackSteps(new ArrayList<>()).build();
		openRouteEditor(musicTrack, newRoute, "Add Route to \"" + musicTrack.getTitle() + "\"",
			() -> musicTrackManager.addCustomRouteToTrack(musicTrack, newRoute));
	}

	private void openEditRouteEditor(MusicTrack track, Route route)
	{
		boolean isDeveloperModeDefaultRouteEdit = DEVELOPER_MODE && !route.isCustom();

		Runnable persistOnSave = isDeveloperModeDefaultRouteEdit
			? () -> saveDefaultRouteEdit(track, route)
			: () -> musicTrackManager.saveCustomRouteEdits(track);

		openRouteEditor(track, route, "Edit Route \"" + route.getName() + "\"", persistOnSave);
	}

	private void saveDefaultRouteEdit(MusicTrack track, Route editedRoute)
	{
		String exportedRouteJson = customTrackStore.exportRouteToJson(editedRoute);
		openExportDialog("Shipped route \"" + editedRoute.getName() + "\" on \"" + track.getTitle()
			+ "\" (region: " + track.getRegion() + ") - paste into its entry's \"routes\" array", exportedRouteJson);
		refresh();
	}

	private void saveDefaultTrackEdit(MusicTrack musicTrack, TrackMetadataDialog dialog)
	{
		UnlockType newUnlockType = dialog.getUnlockTypeValue();
		Quest newUnlockQuest = dialog.getUnlockQuestValue();

		musicTrackManager.applyTrackMetadataInMemory(musicTrack,
			dialog.getTitleValue(), dialog.getRegionValue(), dialog.getLocationValue(), dialog.getWikiUrlValue(),
			newUnlockType, newUnlockQuest, dialog.getUnlockMessageValue(), dialog.getUnlockHintValue());

		String exportedTrackJson = customTrackStore.exportTrackToJson(musicTrack);
		openExportDialog("Shipped track \"" + musicTrack.getTitle() + "\" (region: " + musicTrack.getRegion()
			+ ") - paste into its region JSON file", exportedTrackJson);
	}

	private void openRouteEditor(MusicTrack musicTrack, Route route, String editorTitle, Runnable persistOnSave)
	{
		musicTrackerPlugin.getRouteBuilderSession().start(musicTrack, route);

		RouteEditorDialog routeEditorDialog = new RouteEditorDialog(SwingUtilities.getWindowAncestor(this), editorTitle, route, musicTrackerPlugin);
		routeEditorDialog.setOnSaved(() -> {
			persistOnSave.run();
			refresh();
		});
		routeEditorDialog.setOnClosed(() -> musicTrackerPlugin.getRouteBuilderSession().stop());
		routeEditorDialog.setVisible(true);
	}

	private void confirmAndDeleteRoute(MusicTrack musicTrack, Route route)
	{
		int confirmation = JOptionPane.showConfirmDialog(this,
			"Delete custom route \"" + route.getName() + "\"?\nThis cannot be undone.",
			"Delete Route", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

		if (confirmation == JOptionPane.YES_OPTION)
		{
			musicTrackManager.deleteCustomRoute(musicTrack, route);
			refresh();
		}
	}

	private void openExportDialog(String exportLabel, String jsonText)
	{
		JTextArea textArea = new JTextArea(jsonText, 18, 44);
		textArea.setEditable(true);
		textArea.setCaretPosition(0);
		JScrollPane scrollPane = new JScrollPane(textArea);

		JButton copyButton = new JButton("Copy to Clipboard");
		copyButton.addActionListener(actionEvent -> {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(jsonText), null);
			JOptionPane.showMessageDialog(this, "Copied to clipboard.", "Export", JOptionPane.INFORMATION_MESSAGE);
		});

		JPanel exportPanel = new JPanel(new BorderLayout(0, 6));
		exportPanel.add(scrollPane, BorderLayout.CENTER);
		exportPanel.add(copyButton, BorderLayout.SOUTH);

		JOptionPane.showMessageDialog(this, exportPanel, "Export: " + exportLabel, JOptionPane.PLAIN_MESSAGE);
	}

	private static final class TrackMetadataDialog extends JDialog
	{
		private final JTextField titleField = new JTextField();
		private final JComboBox<String> regionComboBox = new JComboBox<>(RegionLoader.getKnownRegionNames());
		private final JTextField locationField = new JTextField();
		private final JTextField wikiUrlField = new JTextField();
		private final JComboBox<UnlockType> unlockTypeComboBox = new JComboBox<>(UnlockType.values());
		private final JComboBox<Quest> unlockQuestComboBox = new JComboBox<>(buildQuestDropdownOptions());
		private final JTextField unlockMessageField = new JTextField();
		private final JTextField unlockHintField = new JTextField();

		private boolean confirmed = false;

		private TrackMetadataDialog(Window owner, String dialogTitle, MusicTrack existingTrack)
		{
			super(owner, dialogTitle, ModalityType.APPLICATION_MODAL);

			unlockQuestComboBox.setRenderer(new DefaultListCellRenderer()
			{
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
				{
					String displayText = (value == null) ? "(None)" : ((Quest) value).getName();
					return super.getListCellRendererComponent(list, displayText, index, isSelected, cellHasFocus);
				}
			});

			if (existingTrack != null)
			{
				titleField.setText(existingTrack.getTitle());
				regionComboBox.setSelectedItem(existingTrack.getRegion());
				locationField.setText(existingTrack.getLocation());
				wikiUrlField.setText(existingTrack.getWikiUrl());
				if (existingTrack.getUnlockType() != null)
				{
					unlockTypeComboBox.setSelectedItem(existingTrack.getUnlockType());
				}
				unlockQuestComboBox.setSelectedItem(existingTrack.getUnlockQuest());
				unlockMessageField.setText(existingTrack.getUnlockMessage());
				unlockHintField.setText(existingTrack.getUnlockHint());
			}
			else
			{
				unlockTypeComboBox.setSelectedItem(UnlockType.NORMAL);
			}

			JPanel formPanel = new JPanel(new GridLayout(0, 2, 6, 6));
			formPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
			formPanel.add(new JLabel("Title:"));
			formPanel.add(titleField);
			formPanel.add(new JLabel("Region:"));
			formPanel.add(regionComboBox);
			formPanel.add(new JLabel("Location:"));
			formPanel.add(locationField);
			formPanel.add(new JLabel("Wiki URL:"));
			formPanel.add(wikiUrlField);
			formPanel.add(new JLabel("Unlock Type:"));
			formPanel.add(unlockTypeComboBox);
			formPanel.add(new JLabel("Unlock Quest:"));
			formPanel.add(unlockQuestComboBox);
			formPanel.add(new JLabel("Unlock Message:"));
			formPanel.add(unlockMessageField);
			formPanel.add(new JLabel("Unlock Hint:"));
			formPanel.add(unlockHintField);

			JButton saveButton = new JButton("Save");
			saveButton.addActionListener(actionEvent -> {
				if (titleField.getText() == null || titleField.getText().isBlank())
				{
					JOptionPane.showMessageDialog(this, "Title is required.", "Validation", JOptionPane.WARNING_MESSAGE);
					return;
				}
				confirmed = true;
				setVisible(false);
			});

			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(actionEvent -> setVisible(false));

			JPanel buttonPanel = new JPanel();
			buttonPanel.add(saveButton);
			buttonPanel.add(cancelButton);

			setLayout(new BorderLayout());
			add(formPanel, BorderLayout.CENTER);
			add(buttonPanel, BorderLayout.SOUTH);

			pack();
			setLocationRelativeTo(owner);
		}

		private static Quest[] buildQuestDropdownOptions()
		{
			List<Quest> allQuestsSortedByName = new ArrayList<>(Arrays.asList(Quest.values()));
			allQuestsSortedByName.sort(Comparator.comparing(Quest::getName, String.CASE_INSENSITIVE_ORDER));

			Quest[] questOptionsWithNoneFirst = new Quest[allQuestsSortedByName.size() + 1];
			questOptionsWithNoneFirst[0] = null;
			for (int questIndex = 0; questIndex < allQuestsSortedByName.size(); questIndex++)
			{
				questOptionsWithNoneFirst[questIndex + 1] = allQuestsSortedByName.get(questIndex);
			}
			return questOptionsWithNoneFirst;
		}

		private boolean wasConfirmed()
		{
			return confirmed;
		}

		private String getTitleValue()
		{
			return titleField.getText();
		}

		private String getRegionValue()
		{
			return (String) regionComboBox.getSelectedItem();
		}

		private String getLocationValue()
		{
			return locationField.getText();
		}

		private String getWikiUrlValue()
		{
			return wikiUrlField.getText();
		}

		private UnlockType getUnlockTypeValue()
		{
			return (UnlockType) unlockTypeComboBox.getSelectedItem();
		}

		private Quest getUnlockQuestValue()
		{
			return (Quest) unlockQuestComboBox.getSelectedItem();
		}

		private String getUnlockMessageValue()
		{
			return unlockMessageField.getText();
		}

		private String getUnlockHintValue()
		{
			return unlockHintField.getText();
		}
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