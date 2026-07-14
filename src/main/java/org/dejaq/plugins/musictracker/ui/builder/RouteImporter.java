package org.dejaq.plugins.musictracker.ui.builder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.dejaq.plugins.musictracker.MusicTrack;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.json.CustomTrackStore;
import org.dejaq.plugins.musictracker.track.Route;

public class RouteImporter
{
	private final Component parentComponent;
	private final MusicTrackManager musicTrackManager;
	private final CustomTrackStore customTrackStore;
	private final Runnable onImported;

	public RouteImporter(Component parentComponent, MusicTrackManager musicTrackManager, CustomTrackStore customTrackStore, Runnable onImported)
	{
		this.parentComponent = parentComponent;
		this.musicTrackManager = musicTrackManager;
		this.customTrackStore = customTrackStore;
		this.onImported = onImported;
	}

	public void openImportDialog()
	{
		JTextArea jsonTextArea = new JTextArea(18, 44);
		JScrollPane scrollPane = new JScrollPane(jsonTextArea);

		JButton pasteButton = new JButton("Paste from Clipboard");
		pasteButton.addActionListener(actionEvent -> {
			try
			{
				String clipboardText = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
				jsonTextArea.setText(clipboardText);
			}
			catch (Exception clipboardException)
			{
				JOptionPane.showMessageDialog(parentComponent, "Could not read the clipboard.", "Import", JOptionPane.ERROR_MESSAGE);
			}
		});

		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(pasteButton, BorderLayout.EAST);

		JPanel importPanel = new JPanel(new BorderLayout(0, 6));
		importPanel.add(topPanel, BorderLayout.NORTH);
		importPanel.add(scrollPane, BorderLayout.CENTER);

		int result = JOptionPane.showConfirmDialog(parentComponent, importPanel, "Import Route(s) from JSON",
			JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

		if (result != JOptionPane.OK_OPTION)
		{
			return;
		}

		String jsonText = jsonTextArea.getText();
		if (jsonText == null || jsonText.isBlank())
		{
			return;
		}

		performImport(jsonText);
	}

	private void performImport(String jsonText)
	{
		CustomTrackStore.ImportedContentType contentType = customTrackStore.detectContentType(jsonText);

		if (contentType != CustomTrackStore.ImportedContentType.ROUTE)
		{
			JOptionPane.showMessageDialog(parentComponent, "This doesn't look like a route (no recognizable route fields found).",
				"Import Failed", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try
		{
			MusicTrack targetTrack = resolveImportTargetTrack(jsonText);
			if (targetTrack == null)
			{
				return;
			}

			Route importedRoute = customTrackStore.importRouteFromJson(jsonText, targetTrack.getAllRoutes().size(), targetTrack.getTitle());
			musicTrackManager.addCustomRouteToTrack(targetTrack, importedRoute);

			if (onImported != null)
			{
				onImported.run();
			}
			JOptionPane.showMessageDialog(parentComponent, "Import successful.", "Import", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (Exception importException)
		{
			JOptionPane.showMessageDialog(parentComponent, "Failed to parse this JSON: " + importException.getMessage(),
				"Import Failed", JOptionPane.ERROR_MESSAGE);
		}
	}

	private MusicTrack resolveImportTargetTrack(String jsonText)
	{
		String embeddedTrackTitle = customTrackStore.peekRouteTrackTitle(jsonText);
		if (embeddedTrackTitle != null)
		{
			Optional<MusicTrack> matchingTrack = musicTrackManager.findTrackByTitle(embeddedTrackTitle);
			if (matchingTrack.isPresent())
			{
				return matchingTrack.get();
			}
			JOptionPane.showMessageDialog(parentComponent,
				"This route says it belongs to \"" + embeddedTrackTitle + "\", but no track with that title was found.\nPick a track to attach it to instead.",
				"Track Not Found", JOptionPane.WARNING_MESSAGE);
		}
		return promptForTargetTrack();
	}

	private MusicTrack promptForTargetTrack()
	{
		List<MusicTrack> sortedTracks = new ArrayList<>(musicTrackManager.getAllTracks());
		sortedTracks.sort(Comparator.comparing(musicTrack -> musicTrack.getTitle() == null ? "" : musicTrack.getTitle(), String.CASE_INSENSITIVE_ORDER));

		if (sortedTracks.isEmpty())
		{
			return null;
		}

		String[] trackTitles = sortedTracks.stream().map(MusicTrack::getTitle).toArray(String[]::new);

		String selectedTitle = (String) JOptionPane.showInputDialog(parentComponent, "Attach this route to which track?",
			"Select Track", JOptionPane.QUESTION_MESSAGE, null, trackTitles, trackTitles[0]);

		if (selectedTitle == null)
		{
			return null;
		}

		return sortedTracks.stream().filter(candidateTrack -> selectedTitle.equals(candidateTrack.getTitle())).findFirst().orElse(null);
	}
}