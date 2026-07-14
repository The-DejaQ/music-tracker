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
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.SwingUtil;
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
	private static final boolean ROUTES_TAB_ENABLED = false;
	private static final String REPORT_ISSUE_URL = "https://github.com/The-DejaQ/music-tracker/issues/new";
	private static final String CHANGELOG_URL = "https://github.com/The-DejaQ/music-tracker/blob/master/CHANGELOG.md";
	private static final Font SANS_SERIF_PLAIN = new Font("SansSerif", Font.PLAIN, 12);

	@Getter
	private final MusicTrackerPlugin musicTrackerPlugin;
	private final Client client;
	private final ClientThread clientThread;
	@Getter
	private final MusicTrackerConfig musicTrackerConfig;
	private final MusicTrackManager musicTrackManager;
	private final ConfigManager configManager;

	private CardLayout innerCardLayout;
	private JPanel innerContentPanel;
	@Getter
	private TrackerContentPanel trackerContentPanel;

	@Getter
	private SettingsRequiredPanel settingsRequiredPanel;

	private JPanel outerContentPanel;
	private JPanel tabBarPanel;
	private JLabel trackerTabLabel;
	private JLabel routesTabLabel;
	private JLabel builderTabLabel;

	private JPanel routesPlaceholderPanel;

	@Getter
	private TrackBuilderPanel trackBuilderPanel;

	public MusicTrackPanel(MusicTrackerPlugin musicTrackerPlugin, Client client, ClientThread clientThread,
						   MusicTrackerConfig musicTrackerConfig, MusicTrackManager musicTrackManager, ConfigManager configManager)
	{
		super();
		this.musicTrackerPlugin = musicTrackerPlugin;
		this.client = client;
		this.clientThread = clientThread;
		this.musicTrackerConfig = musicTrackerConfig;
		this.musicTrackManager = musicTrackManager;
		this.configManager = configManager;

		setBorder(new EmptyBorder(6, 0, 6, 6));
		setLayout(new BorderLayout());
		buildUserInterface();
	}

	private void buildUserInterface()
	{
		JLabel titleLabel = new JLabel("Music Tracker", SwingConstants.CENTER);
		titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));

		BufferedImage githubIcon = ImageUtil.loadImageResource(MusicTrackerPlugin.class, "github.png");
		BufferedImage changelogIcon = ImageUtil.loadImageResource(MusicTrackerPlugin.class, "changelog.png");

		JButton githubButton = buildTitleIconButton(githubIcon, "Report an issue on GitHub", REPORT_ISSUE_URL);
		JButton changelogButton = buildTitleIconButton(changelogIcon, "View the changelog", CHANGELOG_URL);

		JPanel titleIconsPanel = new JPanel(new GridLayout(1, 2, 2, 0));
		titleIconsPanel.setOpaque(false);
		titleIconsPanel.add(githubButton);
		titleIconsPanel.add(changelogButton);

		JLabel titleSpacerLabel = new JLabel();
		titleSpacerLabel.setPreferredSize(titleIconsPanel.getPreferredSize());

		JPanel titleRowPanel = new JPanel(new BorderLayout());
		titleRowPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		titleRowPanel.add(titleSpacerLabel, BorderLayout.WEST);
		titleRowPanel.add(titleLabel, BorderLayout.CENTER);
		titleRowPanel.add(titleIconsPanel, BorderLayout.EAST);

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
		northPanel.add(titleRowPanel);

		buildTabBar();
		northPanel.add(tabBarPanel);

		add(northPanel, BorderLayout.NORTH);

		buildInnerTrackerCards();

		outerContentPanel = new JPanel(new BorderLayout());

		trackBuilderPanel = new TrackBuilderPanel(musicTrackerPlugin, musicTrackManager);

		routesPlaceholderPanel = new JPanel(new BorderLayout());

		add(outerContentPanel, BorderLayout.CENTER);

		showTrackerTab();
		refreshBuilderTabVisibility();
		refreshState();
	}

	private JButton buildTitleIconButton(BufferedImage icon, String tooltip, String url)
	{
		JButton iconButton = new JButton();
		SwingUtil.removeButtonDecorations(iconButton);
		iconButton.setIcon(new ImageIcon(ImageUtil.resizeImage(icon, 16, 16)));
		iconButton.setToolTipText(tooltip);
		iconButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
		iconButton.setUI(new BasicButtonUI());
		iconButton.addActionListener(actionEvent -> LinkBrowser.browse(url));
		iconButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent mouseEvent)
			{
				iconButton.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent mouseEvent)
			{
				iconButton.setBackground(ColorScheme.DARK_GRAY_COLOR);
			}
		});
		return iconButton;
	}

	private void buildInnerTrackerCards()
	{
		innerCardLayout = new CardLayout();
		innerContentPanel = new JPanel(innerCardLayout);

		addInnerCard(new LoggedOutPanel(), LOGGED_OUT_CARD_NAME);

		settingsRequiredPanel = new SettingsRequiredPanel();
		addInnerCard(settingsRequiredPanel, SETTINGS_REQUIRED_CARD_NAME);

		trackerContentPanel = new TrackerContentPanel(musicTrackerPlugin, musicTrackManager, musicTrackerConfig, configManager);
		addInnerCard(trackerContentPanel, TRACKER_CARD_NAME);
	}

	private void addInnerCard(JPanel panel, String name)
	{
		innerContentPanel.add(panel, name);
		panel.setName(name);
	}

	private void buildTabBar()
	{
		tabBarPanel = new JPanel(new GridLayout(1, 0, 4, 0));
		tabBarPanel.setBorder(new EmptyBorder(8, 0, 4, 0));

		trackerTabLabel = createTabLabel("Tracker");
		routesTabLabel = createTabLabel("Routes");
		builderTabLabel = createTabLabel("Builder");

		trackerTabLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (trackerTabLabel.isEnabled())
				{
					showTrackerTab();
				}
			}
		});
		routesTabLabel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent mouseEvent)
			{
				if (routesTabLabel.isEnabled())
				{
					showRoutesTab();
				}
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
		if (ROUTES_TAB_ENABLED)
		{
			tabBarPanel.add(routesTabLabel);
		}
		tabBarPanel.add(builderTabLabel);
	}

	private JLabel createTabLabel(String text)
	{
		JLabel tabLabel = new JLabel(text, SwingConstants.CENTER);
		tabLabel.setOpaque(true);
		tabLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		tabLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
		tabLabel.setFont(SANS_SERIF_PLAIN);
		return tabLabel;
	}

	public void showTrackerTab()
	{
		outerContentPanel.removeAll();
		outerContentPanel.add(innerContentPanel, BorderLayout.CENTER);
		outerContentPanel.revalidate();
		outerContentPanel.repaint();
		updateTabLabelStyles(trackerTabLabel);
	}

	public void showRoutesTab()
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		outerContentPanel.removeAll();
		outerContentPanel.add(routesPlaceholderPanel, BorderLayout.CENTER);
		outerContentPanel.revalidate();
		outerContentPanel.repaint();
		updateTabLabelStyles(routesTabLabel);
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
		updateTabLabelStyles(builderTabLabel);
	}

	private void updateTabLabelStyles(JLabel activeTabLabel)
	{
		Color activeBackground = ColorScheme.DARKER_GRAY_COLOR;
		Color inactiveBackground = ColorScheme.DARK_GRAY_COLOR;
		Color activeForeground = Color.WHITE;
		Color inactiveForeground = Color.LIGHT_GRAY;

		for (JLabel tabLabel : new JLabel[]{trackerTabLabel, routesTabLabel, builderTabLabel})
		{
			boolean isActive = tabLabel == activeTabLabel;
			tabLabel.setBackground(isActive ? activeBackground : inactiveBackground);
			tabLabel.setForeground(isActive ? activeForeground : inactiveForeground);
		}
	}

	public void refreshBuilderTabVisibility()
	{
		builderTabLabel.setVisible(musicTrackerConfig.enableTrackBuilder());

		if (!musicTrackerConfig.enableTrackBuilder() && isShowingBuilderTab())
		{
			showTrackerTab();
		}

		revalidate();
		repaint();
	}

	public void refreshState()
	{
		boolean isLoggedIn = client.getGameState() == GameState.LOGGED_IN;

		updateLoginGatedTabAvailability(isLoggedIn);

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

	private void updateLoginGatedTabAvailability(boolean isLoggedIn)
	{
		for (JLabel loginGatedTabLabel : new JLabel[]{routesTabLabel, builderTabLabel})
		{
			loginGatedTabLabel.setEnabled(isLoggedIn);
			loginGatedTabLabel.setCursor(Cursor.getPredefinedCursor(isLoggedIn ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			loginGatedTabLabel.setToolTipText(isLoggedIn ? null : "Log in to use this tab");
		}

		if (!isLoggedIn && !isShowingTrackerTab())
		{
			showTrackerTab();
		}
	}

	private boolean isShowingTrackerTab()
	{
		return outerContentPanel.getComponentCount() > 0 && outerContentPanel.getComponent(0) == innerContentPanel;
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