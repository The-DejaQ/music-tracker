package org.dejaq.plugins.musictracker.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import org.dejaq.plugins.musictracker.MusicTrackManager;
import org.dejaq.plugins.musictracker.MusicTrackerConfig;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.ui.builder.TrackBuilderPanel;
import org.dejaq.plugins.musictracker.ui.state.LoggedOutPanel;
import org.dejaq.plugins.musictracker.ui.state.SettingsRequiredPanel;

public class MusicTrackPanel extends PluginPanel
{
	private static final String LOGGED_OUT_CARD_NAME = "LOGGED_OUT";
	private static final String SETTINGS_REQUIRED_CARD_NAME = "SETTINGS_REQUIRED";
	private static final String TRACKER_CARD_NAME = "TRACKER";

	@Getter
	private final MusicTrackerPlugin musicTrackerPlugin;
	private final Client client;
	private final ClientThread clientThread;
	@Getter
	private final MusicTrackerConfig musicTrackerConfig;
	private final MusicTrackManager musicTrackManager;

	private CardLayout innerCardLayout;
	private JPanel innerContentPanel;
	@Getter
	private TrackerContentPanel trackerContentPanel;

	@Getter
	private SettingsRequiredPanel settingsRequiredPanel;

	private JPanel outerContentPanel;
	private JPanel tabBarPanel;
	private JLabel trackerTabLabel;
	private JLabel builderTabLabel;

	@Getter
	private TrackBuilderPanel trackBuilderPanel;

	public MusicTrackPanel(MusicTrackerPlugin musicTrackerPlugin, Client client, ClientThread clientThread,
						   MusicTrackerConfig musicTrackerConfig, MusicTrackManager musicTrackManager)
	{
		super();
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.client = client;
		this.clientThread = clientThread;
		this.musicTrackerConfig = musicTrackerConfig;
		this.musicTrackManager = musicTrackManager;

		setBorder(new EmptyBorder(6, 0, 6, 6));
		setLayout(new BorderLayout());
		buildUserInterface();
	}

	private void buildUserInterface()
	{
		JLabel titleLabel = new JLabel("Music Tracker", SwingConstants.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
		northPanel.add(titleLabel);

		buildTabBar();
		northPanel.add(tabBarPanel);

		add(northPanel, BorderLayout.NORTH);

		buildInnerTrackerCards();

		outerContentPanel = new JPanel(new BorderLayout());

		trackBuilderPanel = new TrackBuilderPanel(musicTrackerPlugin, musicTrackManager);

		add(outerContentPanel, BorderLayout.CENTER);

		refreshBuilderTabVisibility();
		refreshState();
	}

	private void buildInnerTrackerCards()
	{
		innerCardLayout = new CardLayout();
		innerContentPanel = new JPanel(innerCardLayout);

		addInnerCard(new LoggedOutPanel(), LOGGED_OUT_CARD_NAME);

		settingsRequiredPanel = new SettingsRequiredPanel();
		addInnerCard(settingsRequiredPanel, SETTINGS_REQUIRED_CARD_NAME);

		trackerContentPanel = new TrackerContentPanel(musicTrackerPlugin, musicTrackManager, musicTrackerConfig);
		addInnerCard(trackerContentPanel, TRACKER_CARD_NAME);
	}

	private void addInnerCard(JPanel panel, String name)
	{
		innerContentPanel.add(panel, name);
		panel.setName(name);
	}

	private void buildTabBar()
	{
		tabBarPanel = new JPanel(new GridLayout(1, 2, 4, 0));
		tabBarPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

		trackerTabLabel = createTabLabel("Tracker");
		builderTabLabel = createTabLabel("Builder");

		trackerTabLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				showTrackerTab();
			}
		});
		builderTabLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (builderTabLabel.isEnabled())
				{
					showBuilderTab();
				}
			}
		});

		tabBarPanel.add(trackerTabLabel);
		tabBarPanel.add(builderTabLabel);
	}

	private JLabel createTabLabel(String text)
	{
		JLabel tabLabel = new JLabel(text, SwingConstants.CENTER);
		tabLabel.setOpaque(true);
		tabLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tabLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		tabLabel.setFont(tabLabel.getFont().deriveFont(Font.BOLD, 12f));
		return tabLabel;
	}

	public void showTrackerTab()
	{
		outerContentPanel.removeAll();
		outerContentPanel.add(innerContentPanel, BorderLayout.CENTER);
		outerContentPanel.revalidate();
		outerContentPanel.repaint();
		updateTabLabelStyles(true);
	}

	public void showBuilderTab()
	{
		if (!musicTrackerConfig.enableTrackBuilder() || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		outerContentPanel.removeAll();
		outerContentPanel.add(trackBuilderPanel, BorderLayout.CENTER);
		outerContentPanel.revalidate();
		outerContentPanel.repaint();
		trackBuilderPanel.refresh();
		updateTabLabelStyles(false);
	}

	private void updateTabLabelStyles(boolean trackerActive)
	{
		Color activeBackground = ColorScheme.DARKER_GRAY_COLOR;
		Color inactiveBackground = ColorScheme.DARK_GRAY_COLOR;
		Color activeForeground = Color.WHITE;
		Color inactiveForeground = Color.LIGHT_GRAY;

		trackerTabLabel.setBackground(trackerActive ? activeBackground : inactiveBackground);
		trackerTabLabel.setForeground(trackerActive ? activeForeground : inactiveForeground);
		builderTabLabel.setBackground(!trackerActive ? activeBackground : inactiveBackground);
		builderTabLabel.setForeground(!trackerActive ? activeForeground : inactiveForeground);
	}

	public void refreshBuilderTabVisibility()
	{
		boolean builderEnabled = musicTrackerConfig.enableTrackBuilder();
		tabBarPanel.setVisible(builderEnabled);
		showTrackerTab();

		revalidate();
		repaint();
	}

	public void refreshState()
	{
		boolean isLoggedIn = client.getGameState() == GameState.LOGGED_IN;

		updateBuilderTabAvailability(isLoggedIn);

		String targetCard;

		if (!isLoggedIn)
		{
			targetCard = LOGGED_OUT_CARD_NAME;
		}
		else if (!musicTrackerPlugin.getPlayerState().canTrackMusic())
		{
			targetCard = SETTINGS_REQUIRED_CARD_NAME;
		}
		else
		{
			targetCard = TRACKER_CARD_NAME;
		}
		if (getCurrentInnerCardName() != null && !targetCard.equals(getCurrentInnerCardName()))
		{
			innerCardLayout.show(innerContentPanel, targetCard);
		}
	}

	private void updateBuilderTabAvailability(boolean isLoggedIn)
	{
		builderTabLabel.setEnabled(isLoggedIn);
		builderTabLabel.setCursor(Cursor.getPredefinedCursor(isLoggedIn ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		builderTabLabel.setToolTipText(isLoggedIn ? null : "Log in to use the Builder");

		if (!isLoggedIn && isShowingBuilderTab())
		{
			showTrackerTab();
		}
	}

	private boolean isShowingBuilderTab()
	{
		return outerContentPanel.getComponentCount() > 0 && outerContentPanel.getComponent(0) == trackBuilderPanel;
	}

	private String getCurrentInnerCardName()
	{
		for (Component component : innerContentPanel.getComponents())
		{
			if (component.isVisible())
			{
				return component.getName();
			}
		}
		return null;
	}
}