package org.dejaq.plugins.musictracker.ui.builder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.track.Route;

public class RouteEditorDialog extends JDialog
{
	private final RouteEditorPanel routeEditorPanel;

	private Runnable externalOnClosedCallback;

	public RouteEditorDialog(Window owner, String dialogTitle, Route routeToEdit, MusicTrackerPlugin musicTrackerPlugin)
	{
		super(owner, dialogTitle, ModalityType.MODELESS);

		routeEditorPanel = new RouteEditorPanel(routeToEdit, musicTrackerPlugin);
		routeEditorPanel.setOnClosed(() -> {
			closeDialog();
			if (externalOnClosedCallback != null)
			{
				externalOnClosedCallback.run();
			}
		});

		setLayout(new BorderLayout());
		add(routeEditorPanel, BorderLayout.CENTER);

		addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent windowEvent)
			{
				routeEditorPanel.stopLiveRefresh();
				if (externalOnClosedCallback != null)
				{
					externalOnClosedCallback.run();
				}
			}
		});

		setMinimumSize(new Dimension(500, 640));
		pack();
		setLocationRelativeTo(owner);
	}

	public void setOnSaved(Runnable onSavedCallback)
	{
		routeEditorPanel.setOnSaved(onSavedCallback);
	}

	public void setOnClosed(Runnable onClosedCallback)
	{
		this.externalOnClosedCallback = onClosedCallback;
	}

	private void closeDialog()
	{
		setVisible(false);
		dispose();
	}
}