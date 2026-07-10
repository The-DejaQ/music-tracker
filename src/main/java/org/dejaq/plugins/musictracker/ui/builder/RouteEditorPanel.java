package org.dejaq.plugins.musictracker.ui.builder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.builder.RouteBuilderSession;
import org.dejaq.plugins.musictracker.quest.Quest;
import org.dejaq.plugins.musictracker.requirement.ItemRequirement;
import org.dejaq.plugins.musictracker.requirement.LevelRequirement;
import org.dejaq.plugins.musictracker.requirement.collections.ItemCollections;
import org.dejaq.plugins.musictracker.track.InteractionTarget;
import org.dejaq.plugins.musictracker.track.InteractionType;
import org.dejaq.plugins.musictracker.track.Route;
import org.dejaq.plugins.musictracker.track.TrackStep;

public class RouteEditorPanel extends JPanel
{
	private static final int ROW_HEIGHT_PIXELS = 28;
	private static final int SESSION_POLL_INTERVAL_MILLIS = 500;

	private final Route routeToEdit;
	private final MusicTrackerPlugin musicTrackerPlugin;

	private Runnable onSavedCallback;
	private Runnable onClosedCallback;

	private final Timer sessionPollTimer;
	private int lastKnownSessionStepCount;

	private final JTextField nameField = new JTextField();
	private final JComboBox<String> questComboBox;
	private final JTextArea notesTextArea = new JTextArea(3, 20);

	private final JPanel itemsRequiredListPanel = new JPanel();
	private final List<ItemRow> itemsRequiredRows = new ArrayList<>();

	private final JPanel itemsRecommendedListPanel = new JPanel();
	private final List<ItemRow> itemsRecommendedRows = new ArrayList<>();

	private final JPanel levelsRequiredListPanel = new JPanel();
	private final List<LevelRow> levelsRequiredRows = new ArrayList<>();

	private final JPanel levelsRecommendedListPanel = new JPanel();
	private final List<LevelRow> levelsRecommendedRows = new ArrayList<>();

	private final JPanel stepsListPanel = new JPanel();
	private final List<StepEntry> stepEntries = new ArrayList<>();

	public RouteEditorPanel(Route routeToEdit, MusicTrackerPlugin musicTrackerPlugin)
	{
		this.routeToEdit = routeToEdit;
		this.musicTrackerPlugin = musicTrackerPlugin;

		List<String> questOptions = new ArrayList<>();
		questOptions.add("(none)");
		for (Quest quest : Quest.values())
		{
			questOptions.add(quest.name());
		}
		questComboBox = new JComboBox<>(questOptions.toArray(new String[0]));

		itemsRequiredListPanel.setLayout(new BoxLayout(itemsRequiredListPanel, BoxLayout.Y_AXIS));
		itemsRecommendedListPanel.setLayout(new BoxLayout(itemsRecommendedListPanel, BoxLayout.Y_AXIS));
		levelsRequiredListPanel.setLayout(new BoxLayout(levelsRequiredListPanel, BoxLayout.Y_AXIS));
		levelsRecommendedListPanel.setLayout(new BoxLayout(levelsRecommendedListPanel, BoxLayout.Y_AXIS));
		stepsListPanel.setLayout(new BoxLayout(stepsListPanel, BoxLayout.Y_AXIS));

		buildUserInterface();
		populateFromRoute();

		lastKnownSessionStepCount = routeToEdit.getTrackSteps() != null ? routeToEdit.getTrackSteps().size() : 0;
		sessionPollTimer = new Timer(SESSION_POLL_INTERVAL_MILLIS, actionEvent -> pollSessionForNewSteps());
		sessionPollTimer.start();
	}

	public void setOnSaved(Runnable onSavedCallback)
	{
		this.onSavedCallback = onSavedCallback;
	}

	public void setOnClosed(Runnable onClosedCallback)
	{
		this.onClosedCallback = onClosedCallback;
	}

	public void stopLiveRefresh()
	{
		sessionPollTimer.stop();
	}

	private void pollSessionForNewSteps()
	{
		RouteBuilderSession routeBuilderSession = musicTrackerPlugin.getRouteBuilderSession();
		if (!routeBuilderSession.isActive())
		{
			return;
		}

		List<TrackStep> sessionSteps = routeBuilderSession.getFinishedSteps();
		if (sessionSteps.size() <= lastKnownSessionStepCount)
		{
			return;
		}

		for (int stepIndex = lastKnownSessionStepCount; stepIndex < sessionSteps.size(); stepIndex++)
		{
			addStepEntry(sessionSteps.get(stepIndex));
		}
		lastKnownSessionStepCount = sessionSteps.size();
		requestRelayout();
	}

	private void requestRelayout()
	{
		revalidate();
		repaint();
		Window hostWindow = SwingUtilities.getWindowAncestor(this);
		if (hostWindow instanceof JDialog)
		{
			hostWindow.pack();
		}
	}

	private void buildUserInterface()
	{
		JPanel contentPanel = new JPanel();
		contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
		contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		contentPanel.add(buildBasicFieldsSection());
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Items Required", itemsRequiredListPanel, () -> addItemRow(itemsRequiredListPanel, itemsRequiredRows, null)));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Items Recommended", itemsRecommendedListPanel, () -> addItemRow(itemsRecommendedListPanel, itemsRecommendedRows, null)));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Levels Required", levelsRequiredListPanel, () -> addLevelRow(levelsRequiredListPanel, levelsRequiredRows, null)));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Levels Recommended", levelsRecommendedListPanel, () -> addLevelRow(levelsRecommendedListPanel, levelsRecommendedRows, null)));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildStepsSection());

		JScrollPane scrollPane = new JScrollPane(contentPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		add(buildBottomButtonsPanel(), BorderLayout.SOUTH);
	}

	private JPanel buildBasicFieldsSection()
	{
		JPanel sectionPanel = new JPanel();
		sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
		sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		sectionPanel.add(labeledRow("Name:", nameField));
		sectionPanel.add(Box.createVerticalStrut(6));
		sectionPanel.add(labeledRow("Quest:", questComboBox));
		sectionPanel.add(Box.createVerticalStrut(6));

		JLabel notesLabel = new JLabel("Notes:");
		notesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionPanel.add(notesLabel);
		notesTextArea.setLineWrap(true);
		notesTextArea.setWrapStyleWord(true);
		sectionPanel.add(new JScrollPane(notesTextArea));

		return sectionPanel;
	}

	private JPanel labeledRow(String labelText, Component field)
	{
		JPanel rowPanel = new JPanel(new BorderLayout(6, 0));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));
		rowPanel.add(new JLabel(labelText), BorderLayout.WEST);
		rowPanel.add(field, BorderLayout.CENTER);
		return rowPanel;
	}

	private JPanel buildRequirementSection(String headerText, JPanel listPanel, Runnable onAddRow)
	{
		JPanel sectionPanel = new JPanel();
		sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
		sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionPanel.setBorder(new LineBorder(ColorScheme.DARK_GRAY_COLOR, 1));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel headerLabel = new JLabel(headerText);
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));
		headerPanel.add(headerLabel, BorderLayout.WEST);

		JButton addButton = new JButton("Add");
		addButton.setMargin(new Insets(1, 4, 1, 4));
		addButton.addActionListener(actionEvent -> {
			onAddRow.run();
			requestRelayout();
		});
		headerPanel.add(addButton, BorderLayout.EAST);

		sectionPanel.add(headerPanel);
		sectionPanel.add(listPanel);
		sectionPanel.add(Box.createVerticalStrut(4));

		return sectionPanel;
	}

	private JPanel buildStepsSection()
	{
		JPanel sectionPanel = new JPanel();
		sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
		sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionPanel.setBorder(new LineBorder(ColorScheme.DARK_GRAY_COLOR, 1));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel headerLabel = new JLabel("Steps");
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 11f));
		headerPanel.add(headerLabel, BorderLayout.WEST);

		JPanel headerButtonsPanel = new JPanel();
		headerButtonsPanel.setLayout(new BoxLayout(headerButtonsPanel, BoxLayout.X_AXIS));

		JButton finishInGameStepButton = new JButton("Finish Current In-Game Step");
		finishInGameStepButton.setMargin(new Insets(1, 4, 1, 4));
		finishInGameStepButton.setToolTipText("Rolls up whatever you've added via the in-game \"Route Builder\" right-click menu into a finished step");
		finishInGameStepButton.addActionListener(actionEvent -> onFinishInGameStepClicked());
		headerButtonsPanel.add(finishInGameStepButton);
		headerButtonsPanel.add(Box.createHorizontalStrut(4));

		JButton addStepButton = new JButton("Add Step");
		addStepButton.setMargin(new Insets(1, 4, 1, 4));
		addStepButton.addActionListener(actionEvent -> {
			addStepEntry(null);
			requestRelayout();
		});
		headerButtonsPanel.add(addStepButton);

		headerPanel.add(headerButtonsPanel, BorderLayout.EAST);

		sectionPanel.add(headerPanel);
		sectionPanel.add(stepsListPanel);

		return sectionPanel;
	}

	private void onFinishInGameStepClicked()
	{
		RouteBuilderSession routeBuilderSession = musicTrackerPlugin.getRouteBuilderSession();
		if (!routeBuilderSession.hasInProgressStepContent())
		{
			JOptionPane.showMessageDialog(this,
				"Nothing to finish yet - use \"Set Step Destination\" or add an interaction in-game first.",
				"Route Builder", JOptionPane.INFORMATION_MESSAGE);
			return;
		}

		String stepName = JOptionPane.showInputDialog(this, "Step Name (optional):", "Finish Step", JOptionPane.PLAIN_MESSAGE);
		routeBuilderSession.finishCurrentStep(stepName);
		pollSessionForNewSteps();
	}

	private JPanel buildBottomButtonsPanel()
	{
		JPanel buttonPanel = new JPanel();

		JButton saveButton = new JButton("Save");
		saveButton.addActionListener(actionEvent -> onSaveClicked());
		buttonPanel.add(saveButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(actionEvent -> onCancelClicked());
		buttonPanel.add(cancelButton);

		return buttonPanel;
	}

	private void onCancelClicked()
	{
		stopLiveRefresh();
		if (onClosedCallback != null)
		{
			onClosedCallback.run();
		}
	}

	private static final class ItemRow
	{
		private final JPanel rowPanel;
		private final JTextField nameField;
		private final JCheckBox itemCollectionCheckBox;
		private final JSpinner quantitySpinner;
		private final JTextField labelField;

		private ItemRow(JPanel rowPanel, JTextField nameField, JCheckBox itemCollectionCheckBox, JSpinner quantitySpinner, JTextField labelField)
		{
			this.rowPanel = rowPanel;
			this.nameField = nameField;
			this.itemCollectionCheckBox = itemCollectionCheckBox;
			this.quantitySpinner = quantitySpinner;
			this.labelField = labelField;
		}
	}

	private void addItemRow(JPanel listPanel, List<ItemRow> rowList, ItemRequirement existingItem)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));

		JTextField nameField = new JTextField();
		nameField.setToolTipText("Item name or ItemCollection");

		JCheckBox itemCollectionCheckBox = new JCheckBox("Item Collection");

		JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
		quantitySpinner.setMaximumSize(new Dimension(64, ROW_HEIGHT_PIXELS));

		JTextField labelField = new JTextField();
		labelField.setToolTipText("Optional display label override");

		if (existingItem != null)
		{
			if (existingItem.getItemCollection() != null)
			{
				itemCollectionCheckBox.setSelected(true);
				nameField.setText(existingItem.getItemCollection().name());
			}
			else
			{
				nameField.setText(existingItem.getItem() != null ? existingItem.getItem() : "");
			}
			quantitySpinner.setValue(Math.max(0, existingItem.getQuantity()));
			labelField.setText(existingItem.getLabel() != null ? existingItem.getLabel() : "");
		}

		JButton removeButton = new JButton("x");
		removeButton.setMargin(new Insets(0, 6, 0, 6));

		rowPanel.add(new JLabel("Item: "));
		rowPanel.add(nameField);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(itemCollectionCheckBox);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(new JLabel("Qty: "));
		rowPanel.add(quantitySpinner);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(new JLabel("Label: "));
		rowPanel.add(labelField);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(removeButton);

		ItemRow itemRow = new ItemRow(rowPanel, nameField, itemCollectionCheckBox, quantitySpinner, labelField);
		removeButton.addActionListener(actionEvent -> {
			listPanel.remove(rowPanel);
			rowList.remove(itemRow);
			listPanel.revalidate();
			listPanel.repaint();
			requestRelayout();
		});

		rowList.add(itemRow);
		listPanel.add(rowPanel);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private List<ItemRequirement> readItemRows(List<ItemRow> rowList)
	{
		List<ItemRequirement> itemRequirements = new ArrayList<>();
		for (ItemRow itemRow : rowList)
		{
			String itemName = itemRow.nameField.getText();
			String label = itemRow.labelField.getText();
			if ((itemName == null || itemName.isBlank()) && (label == null || label.isBlank()))
			{
				continue;
			}
			int quantity = (Integer) itemRow.quantitySpinner.getValue();

			ItemRequirement itemRequirement;
			if (itemName == null || itemName.isBlank())
			{
				itemRequirement = new ItemRequirement(-1, quantity);
			}
			else if (itemRow.itemCollectionCheckBox.isSelected())
			{
				try
				{
					ItemCollections itemCollection = ItemCollections.valueOf(itemName.trim().toUpperCase(Locale.ROOT));
					itemRequirement = new ItemRequirement(itemCollection, quantity);
				}
				catch (IllegalArgumentException invalidItemCollectionException)
				{
					JOptionPane.showMessageDialog(this, "\"" + itemName.trim() + "\" is not a known Item Collection.",
						"Validation", JOptionPane.WARNING_MESSAGE);
					continue;
				}
			}
			else
			{
				itemRequirement = new ItemRequirement(itemName.trim(), quantity);
			}

			if (label != null && !label.isBlank())
			{
				itemRequirement.setLabel(label.trim());
			}
			itemRequirements.add(itemRequirement);
		}
		return itemRequirements;
	}

	private static final class LevelRow
	{
		private final JPanel rowPanel;
		private final JComboBox<Skill> skillComboBox;
		private final JSpinner levelSpinner;

		private LevelRow(JPanel rowPanel, JComboBox<Skill> skillComboBox, JSpinner levelSpinner)
		{
			this.rowPanel = rowPanel;
			this.skillComboBox = skillComboBox;
			this.levelSpinner = levelSpinner;
		}
	}

	private void addLevelRow(JPanel listPanel, List<LevelRow> rowList, LevelRequirement existingLevel)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));

		JComboBox<Skill> skillComboBox = new JComboBox<>(Skill.values());
		JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
		levelSpinner.setMaximumSize(new Dimension(64, ROW_HEIGHT_PIXELS));

		if (existingLevel != null)
		{
			if (existingLevel.getSkill() != null)
			{
				skillComboBox.setSelectedItem(existingLevel.getSkill());
			}
			levelSpinner.setValue(Math.max(1, Math.min(99, existingLevel.getLevel())));
		}

		JButton removeButton = new JButton("x");
		removeButton.setMargin(new Insets(0, 6, 0, 6));

		rowPanel.add(new JLabel("Skill: "));
		rowPanel.add(skillComboBox);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(new JLabel("Level: "));
		rowPanel.add(levelSpinner);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(removeButton);

		LevelRow levelRow = new LevelRow(rowPanel, skillComboBox, levelSpinner);
		removeButton.addActionListener(actionEvent -> {
			listPanel.remove(rowPanel);
			rowList.remove(levelRow);
			listPanel.revalidate();
			listPanel.repaint();
			requestRelayout();
		});

		rowList.add(levelRow);
		listPanel.add(rowPanel);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private List<LevelRequirement> readLevelRows(List<LevelRow> rowList)
	{
		List<LevelRequirement> levelRequirements = new ArrayList<>();
		for (LevelRow levelRow : rowList)
		{
			Skill selectedSkill = (Skill) levelRow.skillComboBox.getSelectedItem();
			if (selectedSkill == null)
			{
				continue;
			}
			int level = (Integer) levelRow.levelSpinner.getValue();
			levelRequirements.add(LevelRequirement.of(selectedSkill, level));
		}
		return levelRequirements;
	}

	private static final class StepEntry
	{
		private final JPanel panel;
		private final JTextField nameField;
		private final JSpinner xSpinner;
		private final JSpinner ySpinner;
		private final JSpinner zSpinner;
		private final JPanel interactionsListPanel;
		private final List<InteractionRow> interactionRows = new ArrayList<>();

		private StepEntry(JPanel panel, JTextField nameField, JSpinner xSpinner, JSpinner ySpinner, JSpinner zSpinner, JPanel interactionsListPanel)
		{
			this.panel = panel;
			this.nameField = nameField;
			this.xSpinner = xSpinner;
			this.ySpinner = ySpinner;
			this.zSpinner = zSpinner;
			this.interactionsListPanel = interactionsListPanel;
		}
	}

	private void addStepEntry(TrackStep existingStep)
	{
		JPanel stepPanel = new JPanel();
		stepPanel.setLayout(new BoxLayout(stepPanel, BoxLayout.Y_AXIS));
		stepPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		stepPanel.setBorder(new EmptyBorder(6, 6, 6, 6));
		stepPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		stepPanel.setOpaque(true);

		JTextField stepNameField = new JTextField();
		JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 20_000, 1));
		JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 20_000, 1));
		JSpinner zSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));

		if (existingStep != null)
		{
			stepNameField.setText(existingStep.getName() != null ? existingStep.getName() : "");
			if (existingStep.getDestination() != null)
			{
				xSpinner.setValue(existingStep.getDestination().getX());
				ySpinner.setValue(existingStep.getDestination().getY());
				zSpinner.setValue(existingStep.getDestination().getPlane());
			}
		}

		JPanel headerRow = new JPanel(new BorderLayout(4, 0));
		headerRow.setOpaque(false);
		headerRow.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel nameAndCoordsPanel = new JPanel();
		nameAndCoordsPanel.setLayout(new BoxLayout(nameAndCoordsPanel, BoxLayout.X_AXIS));
		nameAndCoordsPanel.setOpaque(false);
		nameAndCoordsPanel.add(new JLabel("Step name: "));
		nameAndCoordsPanel.add(stepNameField);
		nameAndCoordsPanel.add(Box.createHorizontalStrut(6));
		nameAndCoordsPanel.add(new JLabel("X:"));
		nameAndCoordsPanel.add(xSpinner);
		nameAndCoordsPanel.add(new JLabel("Y:"));
		nameAndCoordsPanel.add(ySpinner);
		nameAndCoordsPanel.add(new JLabel("Z:"));
		nameAndCoordsPanel.add(zSpinner);
		headerRow.add(nameAndCoordsPanel, BorderLayout.CENTER);

		JPanel stepButtonsPanel = new JPanel();
		stepButtonsPanel.setLayout(new BoxLayout(stepButtonsPanel, BoxLayout.X_AXIS));
		stepButtonsPanel.setOpaque(false);
		JButton moveUpButton = new JButton("^");
		moveUpButton.setMargin(new Insets(0, 4, 0, 4));
		JButton moveDownButton = new JButton("v");
		moveDownButton.setMargin(new Insets(0, 4, 0, 4));
		JButton removeStepButton = new JButton("Remove Step");
		removeStepButton.setMargin(new Insets(0, 4, 0, 4));
		stepButtonsPanel.add(moveUpButton);
		stepButtonsPanel.add(moveDownButton);
		stepButtonsPanel.add(removeStepButton);
		headerRow.add(stepButtonsPanel, BorderLayout.EAST);

		JPanel interactionsListPanel = new JPanel();
		interactionsListPanel.setLayout(new BoxLayout(interactionsListPanel, BoxLayout.Y_AXIS));
		interactionsListPanel.setOpaque(false);
		interactionsListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton addInteractionButton = new JButton("Add Interaction");
		addInteractionButton.setAlignmentX(Component.LEFT_ALIGNMENT);

		stepPanel.add(headerRow);
		stepPanel.add(Box.createVerticalStrut(4));
		stepPanel.add(interactionsListPanel);
		stepPanel.add(addInteractionButton);

		StepEntry stepEntry = new StepEntry(stepPanel, stepNameField, xSpinner, ySpinner, zSpinner, interactionsListPanel);

		if (existingStep != null && existingStep.getInteractions() != null)
		{
			for (InteractionTarget interactionTarget : existingStep.getInteractions())
			{
				addInteractionRow(stepEntry, interactionTarget);
			}
		}

		addInteractionButton.addActionListener(actionEvent -> {
			addInteractionRow(stepEntry, null);
			requestRelayout();
		});

		moveUpButton.addActionListener(actionEvent -> {
			moveStepEntry(stepEntry, -1);
			requestRelayout();
		});
		moveDownButton.addActionListener(actionEvent -> {
			moveStepEntry(stepEntry, 1);
			requestRelayout();
		});
		removeStepButton.addActionListener(actionEvent -> {
			stepsListPanel.remove(stepPanel);
			stepEntries.remove(stepEntry);
			stepsListPanel.revalidate();
			stepsListPanel.repaint();
			requestRelayout();
		});

		stepEntries.add(stepEntry);
		stepsListPanel.add(stepPanel);
		stepsListPanel.add(Box.createVerticalStrut(6));
		stepsListPanel.revalidate();
		stepsListPanel.repaint();
	}

	private void moveStepEntry(StepEntry stepEntry, int direction)
	{
		int currentIndex = stepEntries.indexOf(stepEntry);
		int targetIndex = currentIndex + direction;
		if (currentIndex < 0 || targetIndex < 0 || targetIndex >= stepEntries.size())
		{
			return;
		}

		stepEntries.remove(currentIndex);
		stepEntries.add(targetIndex, stepEntry);

		stepsListPanel.removeAll();
		for (StepEntry entry : stepEntries)
		{
			stepsListPanel.add(entry.panel);
			stepsListPanel.add(Box.createVerticalStrut(6));
		}
		stepsListPanel.revalidate();
		stepsListPanel.repaint();
	}

	private static final class InteractionRow
	{
		private final JPanel rowPanel;
		private final JTextField entityField;
		private final JComboBox<InteractionType> typeComboBox;
		private final JTextField hintField;
		private final JTextField actionsField;

		private InteractionRow(JPanel rowPanel, JTextField entityField, JComboBox<InteractionType> typeComboBox, JTextField hintField, JTextField actionsField)
		{
			this.rowPanel = rowPanel;
			this.entityField = entityField;
			this.typeComboBox = typeComboBox;
			this.hintField = hintField;
			this.actionsField = actionsField;
		}
	}

	private void addInteractionRow(StepEntry stepEntry, InteractionTarget existingInteraction)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));

		JTextField entityField = new JTextField();
		entityField.setToolTipText("NPC or object name (matched by partial, case-insensitive name)");

		JComboBox<InteractionType> typeComboBox = new JComboBox<>(InteractionType.values());

		JTextField hintField = new JTextField();
		hintField.setToolTipText("Hint text shown above the highlight");

		JTextField actionsField = new JTextField();
		actionsField.setToolTipText("Menu actions that advance this step, comma-separated (e.g. Climb-down, Open)");

		if (existingInteraction != null)
		{
			entityField.setText(existingInteraction.getEntity() != null ? existingInteraction.getEntity() : "");
			if (existingInteraction.getType() != null)
			{
				typeComboBox.setSelectedItem(existingInteraction.getType());
			}
			hintField.setText(existingInteraction.getHint() != null ? existingInteraction.getHint() : "");
			if (existingInteraction.getActions() != null && !existingInteraction.getActions().isEmpty())
			{
				actionsField.setText(String.join(", ", existingInteraction.getActions()));
			}
		}

		JButton removeButton = new JButton("x");
		removeButton.setMargin(new Insets(0, 6, 0, 6));

		rowPanel.add(new JLabel("Entity: "));
		rowPanel.add(entityField);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(typeComboBox);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(new JLabel("Hint: "));
		rowPanel.add(hintField);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(new JLabel("Actions: "));
		rowPanel.add(actionsField);
		rowPanel.add(Box.createHorizontalStrut(4));
		rowPanel.add(removeButton);

		InteractionRow interactionRow = new InteractionRow(rowPanel, entityField, typeComboBox, hintField, actionsField);
		removeButton.addActionListener(actionEvent -> {
			stepEntry.interactionsListPanel.remove(rowPanel);
			stepEntry.interactionRows.remove(interactionRow);
			stepEntry.interactionsListPanel.revalidate();
			stepEntry.interactionsListPanel.repaint();
			requestRelayout();
		});

		stepEntry.interactionRows.add(interactionRow);
		stepEntry.interactionsListPanel.add(rowPanel);
		stepEntry.interactionsListPanel.revalidate();
		stepEntry.interactionsListPanel.repaint();
	}

	private List<InteractionTarget> readInteractionRows(StepEntry stepEntry)
	{
		List<InteractionTarget> interactionTargets = new ArrayList<>();
		for (InteractionRow interactionRow : stepEntry.interactionRows)
		{
			String entityName = interactionRow.entityField.getText();
			if (entityName == null || entityName.isBlank())
			{
				continue;
			}

			InteractionType selectedType = (InteractionType) interactionRow.typeComboBox.getSelectedItem();
			String hintText = interactionRow.hintField.getText();

			InteractionTarget.InteractionTargetBuilder interactionTargetBuilder = InteractionTarget.builder()
				.entityId(-1)
				.entity(entityName.trim())
				.type(selectedType != null ? selectedType : InteractionType.GAME_OBJECT)
				.hint(hintText != null && !hintText.isBlank() ? hintText.trim() : null);

			String actionsText = interactionRow.actionsField.getText();
			if (actionsText != null && !actionsText.isBlank())
			{
				List<String> parsedActions = new ArrayList<>();
				for (String actionToken : actionsText.split(","))
				{
					String trimmedAction = actionToken.trim();
					if (!trimmedAction.isEmpty())
					{
						parsedActions.add(trimmedAction);
					}
				}
				interactionTargetBuilder.actions(parsedActions);
			}

			interactionTargets.add(interactionTargetBuilder.build());
		}
		return interactionTargets;
	}

	private void populateFromRoute()
	{
		nameField.setText(routeToEdit.getName() != null ? routeToEdit.getName() : "");
		questComboBox.setSelectedItem(routeToEdit.getQuest() != null ? routeToEdit.getQuest().name() : "(none)");
		notesTextArea.setText(routeToEdit.getNotes() != null ? routeToEdit.getNotes() : "");

		if (routeToEdit.getItems() != null)
		{
			for (ItemRequirement itemRequirement : routeToEdit.getItems())
			{
				addItemRow(itemsRequiredListPanel, itemsRequiredRows, itemRequirement);
			}
		}
		if (routeToEdit.getRecommendedItems() != null)
		{
			for (ItemRequirement itemRequirement : routeToEdit.getRecommendedItems())
			{
				addItemRow(itemsRecommendedListPanel, itemsRecommendedRows, itemRequirement);
			}
		}
		if (routeToEdit.getLevels() != null)
		{
			for (LevelRequirement levelRequirement : routeToEdit.getLevels())
			{
				addLevelRow(levelsRequiredListPanel, levelsRequiredRows, levelRequirement);
			}
		}
		if (routeToEdit.getRecommendedLevels() != null)
		{
			for (LevelRequirement levelRequirement : routeToEdit.getRecommendedLevels())
			{
				addLevelRow(levelsRecommendedListPanel, levelsRecommendedRows, levelRequirement);
			}
		}
		if (routeToEdit.getTrackSteps() != null)
		{
			for (TrackStep trackStep : routeToEdit.getTrackSteps())
			{
				addStepEntry(trackStep);
			}
		}
	}

	private void onSaveClicked()
	{
		if (nameField.getText() == null || nameField.getText().isBlank())
		{
			JOptionPane.showMessageDialog(this, "Route name is required.", "Validation", JOptionPane.WARNING_MESSAGE);
			return;
		}

		routeToEdit.setName(nameField.getText().trim());

		String selectedQuestName = (String) questComboBox.getSelectedItem();
		if (selectedQuestName == null || selectedQuestName.equals("(none)"))
		{
			routeToEdit.setQuest(null);
		}
		else
		{
			try
			{
				routeToEdit.setQuest(Quest.valueOf(selectedQuestName.toUpperCase(Locale.ROOT)));
			}
			catch (IllegalArgumentException invalidQuestException)
			{
				routeToEdit.setQuest(null);
			}
		}

		String notesText = notesTextArea.getText();
		routeToEdit.setNotes(notesText != null && !notesText.isBlank() ? notesText.trim() : null);

		routeToEdit.setItems(readItemRows(itemsRequiredRows));
		routeToEdit.setRecommendedItems(readItemRows(itemsRecommendedRows));
		routeToEdit.setLevels(readLevelRows(levelsRequiredRows));
		routeToEdit.setRecommendedLevels(readLevelRows(levelsRecommendedRows));

		List<TrackStep> trackSteps = new ArrayList<>();
		for (StepEntry stepEntry : stepEntries)
		{
			WorldPoint destination = new WorldPoint(
				(Integer) stepEntry.xSpinner.getValue(),
				(Integer) stepEntry.ySpinner.getValue(),
				(Integer) stepEntry.zSpinner.getValue());

			TrackStep.TrackStepBuilder trackStepBuilder = TrackStep.builder().destination(destination);

			String stepName = stepEntry.nameField.getText();
			if (stepName != null && !stepName.isBlank())
			{
				trackStepBuilder.name(stepName.trim());
			}

			for (InteractionTarget interactionTarget : readInteractionRows(stepEntry))
			{
				trackStepBuilder.interaction(interactionTarget);
			}

			trackSteps.add(trackStepBuilder.build());
		}
		routeToEdit.setTrackSteps(trackSteps);

		stopLiveRefresh();
		if (onSavedCallback != null)
		{
			onSavedCallback.run();
		}
		if (onClosedCallback != null)
		{
			onClosedCallback.run();
		}
	}
}