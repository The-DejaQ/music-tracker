package org.dejaq.plugins.musictracker.ui.builder;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
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
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

	private static final int ITEM_TABLE_COLUMN_GAP_PIXELS = 12;
	private static final int ITEM_TABLE_LEFT_INSET_PIXELS = 6;
	private static final int ITEM_TABLE_RIGHT_INSET_PIXELS = 6;

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
	private final List<ItemRequirement> itemsRequired = new ArrayList<>();

	private final JPanel itemsRecommendedListPanel = new JPanel();
	private final List<ItemRequirement> itemsRecommended = new ArrayList<>();

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
		contentPanel.add(buildRequirementSection("Items Required", "Add Required Item", itemsRequiredListPanel, () -> onAddItemClicked(itemsRequiredListPanel, itemsRequired, "Required")));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Items Recommended", "Add Recommended Item", itemsRecommendedListPanel, () -> onAddItemClicked(itemsRecommendedListPanel, itemsRecommended, "Recommended")));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Levels Required", "Add Required Level", levelsRequiredListPanel, () -> addLevelRow(levelsRequiredListPanel, levelsRequiredRows, null)));
		contentPanel.add(Box.createVerticalStrut(10));
		contentPanel.add(buildRequirementSection("Levels Recommended", "Add Recommended Level", levelsRecommendedListPanel, () -> addLevelRow(levelsRecommendedListPanel, levelsRecommendedRows, null)));
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

	private JPanel buildRequirementSection(String headerText, String addButtonLabel, JPanel listPanel, Runnable onAddRow)
	{
		JPanel sectionPanel = new JPanel();
		sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.Y_AXIS));
		sectionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		sectionPanel.setBorder(new LineBorder(ColorScheme.DARK_GRAY_COLOR, 1));

		JPanel headerPanel = new JPanel(new BorderLayout());
		headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));
		headerPanel.setBorder(new EmptyBorder(4, 6, 4, 6));

		JLabel headerLabel = new JLabel(headerText);
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
		headerPanel.add(headerLabel, BorderLayout.WEST);

		listPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JButton addButton = new JButton(addButtonLabel);
		addButton.setFont(addButton.getFont().deriveFont(Font.PLAIN, 14f));
		addButton.setMargin(new Insets(3, 8, 3, 8));
		addButton.addActionListener(actionEvent -> {
			onAddRow.run();
			requestRelayout();
		});

		JPanel addButtonRowPanel = new JPanel();
		addButtonRowPanel.setLayout(new BoxLayout(addButtonRowPanel, BoxLayout.X_AXIS));
		addButtonRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		addButtonRowPanel.setBorder(new EmptyBorder(6, 0, 6, 6));
		addButtonRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height + 12));
		addButtonRowPanel.add(addButton);
		addButtonRowPanel.add(Box.createHorizontalGlue());

		sectionPanel.add(headerPanel);
		sectionPanel.add(listPanel);
		sectionPanel.add(addButtonRowPanel);

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
		headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, 14f));
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

	private void onAddItemClicked(JPanel itemsListPanel, List<ItemRequirement> itemRequirements, String sectionLabel)
	{
		ItemRequirementDialog itemRequirementDialog = new ItemRequirementDialog(SwingUtilities.getWindowAncestor(this), sectionLabel, null);
		itemRequirementDialog.setVisible(true);

		if (!itemRequirementDialog.wasConfirmed())
		{
			return;
		}

		itemRequirements.add(itemRequirementDialog.buildItemRequirement());
		rebuildItemsListPanel(itemsListPanel, itemRequirements, sectionLabel);
	}

	private void onEditItemClicked(JPanel itemsListPanel, List<ItemRequirement> itemRequirements, ItemRequirement itemRequirementToEdit, String sectionLabel)
	{
		ItemRequirementDialog itemRequirementDialog = new ItemRequirementDialog(SwingUtilities.getWindowAncestor(this), sectionLabel, itemRequirementToEdit);
		itemRequirementDialog.setVisible(true);

		if (!itemRequirementDialog.wasConfirmed())
		{
			return;
		}

		int itemRequirementIndex = -1;
		for (int candidateIndex = 0; candidateIndex < itemRequirements.size(); candidateIndex++)
		{
			if (itemRequirements.get(candidateIndex) == itemRequirementToEdit)
			{
				itemRequirementIndex = candidateIndex;
				break;
			}
		}

		if (itemRequirementIndex >= 0)
		{
			itemRequirements.set(itemRequirementIndex, itemRequirementDialog.buildItemRequirement());
		}
		rebuildItemsListPanel(itemsListPanel, itemRequirements, sectionLabel);
	}

	private void onDeleteItemClicked(JPanel itemsListPanel, List<ItemRequirement> itemRequirements, ItemRequirement itemRequirementToDelete, String sectionLabel)
	{
		int confirmation = JOptionPane.showConfirmDialog(this, "Delete this item requirement?",
			"Delete Item", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (confirmation != JOptionPane.YES_OPTION)
		{
			return;
		}

		itemRequirements.removeIf(candidateItemRequirement -> candidateItemRequirement == itemRequirementToDelete);
		rebuildItemsListPanel(itemsListPanel, itemRequirements, sectionLabel);
	}

	private void rebuildItemsListPanel(JPanel itemsListPanel, List<ItemRequirement> itemRequirements, String sectionLabel)
	{
		itemsListPanel.removeAll();

		if (!itemRequirements.isEmpty())
		{
			itemsListPanel.add(buildItemsHeaderRow());
			for (ItemRequirement itemRequirement : itemRequirements)
			{
				itemsListPanel.add(buildItemSummaryRow(itemsListPanel, itemRequirements, itemRequirement, sectionLabel));
			}
		}

		itemsListPanel.revalidate();
		itemsListPanel.repaint();
		requestRelayout();
	}

	private JPanel buildItemsHeaderRow()
	{
		JPanel headerRowPanel = new JPanel();
		headerRowPanel.setLayout(new BoxLayout(headerRowPanel, BoxLayout.X_AXIS));
		headerRowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		headerRowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));
		headerRowPanel.setBorder(new EmptyBorder(0, ITEM_TABLE_LEFT_INSET_PIXELS, 0, ITEM_TABLE_RIGHT_INSET_PIXELS));

		JPanel columnsPanel = new JPanel(new GridLayout(1, 4, ITEM_TABLE_COLUMN_GAP_PIXELS, 0));
		columnsPanel.add(columnLabel("Item", true));
		columnsPanel.add(columnLabel("Qty", true));
		columnsPanel.add(columnLabel("Collection", true));
		columnsPanel.add(columnLabel("Label", true));
		headerRowPanel.add(columnsPanel);

		headerRowPanel.add(Box.createHorizontalStrut(ITEM_TABLE_COLUMN_GAP_PIXELS));
		headerRowPanel.add(Box.createRigidArea(new Dimension(actionColumnWidthPixels(), ROW_HEIGHT_PIXELS)));

		return headerRowPanel;
	}

	private JPanel buildItemSummaryRow(JPanel itemsListPanel, List<ItemRequirement> itemRequirements, ItemRequirement itemRequirement, String sectionLabel)
	{
		JPanel rowPanel = new JPanel();
		rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
		rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT_PIXELS));
		rowPanel.setBorder(new EmptyBorder(0, ITEM_TABLE_LEFT_INSET_PIXELS, 0, ITEM_TABLE_RIGHT_INSET_PIXELS));

		String itemColumnText;
		if (itemRequirement.isItemCollection())
		{
			itemColumnText = itemRequirement.getItemCollection() != null ? itemRequirement.getItemCollection().name() : "";
		}
		else
		{
			itemColumnText = itemRequirement.getItemId() > 0 ? String.valueOf(itemRequirement.getItemId()) : "";
		}

		JPanel columnsPanel = new JPanel(new GridLayout(1, 4, ITEM_TABLE_COLUMN_GAP_PIXELS, 0));
		columnsPanel.add(columnLabel(itemColumnText, false));
		columnsPanel.add(columnLabel(String.valueOf(itemRequirement.getQuantity()), false));
		columnsPanel.add(columnLabel(itemRequirement.isItemCollection() ? "\u2713" : "", false));
		columnsPanel.add(columnLabel(itemRequirement.getLabel() != null ? itemRequirement.getLabel() : "", false));
		rowPanel.add(columnsPanel);

		rowPanel.add(Box.createHorizontalStrut(ITEM_TABLE_COLUMN_GAP_PIXELS));

		JPanel actionButtonsPanel = new JPanel();
		actionButtonsPanel.setLayout(new BoxLayout(actionButtonsPanel, BoxLayout.X_AXIS));
		Dimension actionColumnSize = new Dimension(actionColumnWidthPixels(), ROW_HEIGHT_PIXELS);
		actionButtonsPanel.setPreferredSize(actionColumnSize);
		actionButtonsPanel.setMinimumSize(actionColumnSize);
		actionButtonsPanel.setMaximumSize(actionColumnSize);

		JButton editButton = createSmallButton("Edit");
		editButton.addActionListener(actionEvent -> onEditItemClicked(itemsListPanel, itemRequirements, itemRequirement, sectionLabel));
		actionButtonsPanel.add(editButton);

		actionButtonsPanel.add(Box.createHorizontalStrut(4));

		JButton deleteButton = createSmallButton("Delete");
		deleteButton.addActionListener(actionEvent -> onDeleteItemClicked(itemsListPanel, itemRequirements, itemRequirement, sectionLabel));
		actionButtonsPanel.add(deleteButton);

		rowPanel.add(actionButtonsPanel);

		return rowPanel;
	}

	private int actionColumnWidthPixels()
	{
		JButton editSizingButton = createSmallButton("Edit");
		JButton deleteSizingButton = createSmallButton("Delete");
		return editSizingButton.getPreferredSize().width + 4 + deleteSizingButton.getPreferredSize().width;
	}

	private JLabel columnLabel(String text, boolean bold)
	{
		JLabel label = new JLabel(text != null ? text : "");
		if (bold)
		{
			label.setFont(label.getFont().deriveFont(Font.BOLD));
		}
		return label;
	}

	private JButton createSmallButton(String text)
	{
		JButton button = new JButton(text);
		button.setFont(button.getFont().deriveFont(10f));
		button.setMargin(new Insets(2, 4, 2, 4));
		return button;
	}

	private static final class ItemRequirementDialog extends JDialog
	{
		private static final int MAX_COLLECTION_SUGGESTION_RESULTS = 8;

		private final JCheckBox itemCollectionCheckBox = new JCheckBox("Item Collection");
		private final JPopupMenu collectionSuggestionsPopupMenu = new JPopupMenu();
		private final JTextField itemIdentifierField = new JTextField();
		private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 0, Integer.MAX_VALUE, 1));
		private final JTextField labelField = new JTextField();
		private final JLabel validationMessageLabel = new JLabel(" ");
		private final JButton saveButton = new JButton("Save");

		private boolean confirmed = false;

		private ItemRequirementDialog(Window owner, String sectionLabel, ItemRequirement existingItem)
		{
			super(owner, (existingItem == null ? "Add " : "Edit ") + sectionLabel + " Item", ModalityType.APPLICATION_MODAL);

			if (existingItem != null)
			{
				if (existingItem.isItemCollection())
				{
					itemCollectionCheckBox.setSelected(true);
					itemIdentifierField.setText(existingItem.getItemCollection() != null ? existingItem.getItemCollection().name() : "");
				}
				else if (existingItem.getItemId() > 0)
				{
					itemIdentifierField.setText(String.valueOf(existingItem.getItemId()));
				}
				quantitySpinner.setValue(Math.max(0, existingItem.getQuantity()));
				labelField.setText(existingItem.getLabel() != null ? existingItem.getLabel() : "");
			}

			JPanel formPanel = new JPanel(new GridLayout(0, 2, 6, 6));
			formPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
			formPanel.add(new JLabel("Item Collection:"));
			formPanel.add(itemCollectionCheckBox);
			formPanel.add(new JLabel("Item:"));
			formPanel.add(itemIdentifierField);
			formPanel.add(new JLabel("Quantity:"));
			formPanel.add(quantitySpinner);
			formPanel.add(new JLabel("Label:"));
			formPanel.add(labelField);

			validationMessageLabel.setForeground(ColorScheme.PROGRESS_ERROR_COLOR);
			validationMessageLabel.setBorder(new EmptyBorder(0, 12, 8, 12));

			addLiveTextValidation(itemIdentifierField,
				this::refreshValidationState,
				() -> {
					if (itemCollectionCheckBox.isSelected())
					{
						updateCollectionSuggestions();
					}
				});
			addLiveTextValidation(labelField, this::refreshValidationState);

			itemCollectionCheckBox.addActionListener(actionEvent -> {
				refreshValidationState();
				updateCollectionSuggestions();
			});

			saveButton.addActionListener(actionEvent -> {
				confirmed = true;
				setVisible(false);
			});

			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(actionEvent -> setVisible(false));

			JPanel buttonPanel = new JPanel();
			buttonPanel.add(saveButton);
			buttonPanel.add(cancelButton);

			JPanel southPanel = new JPanel();
			southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
			southPanel.add(validationMessageLabel);
			southPanel.add(buttonPanel);

			setLayout(new BorderLayout());
			add(formPanel, BorderLayout.CENTER);
			add(southPanel, BorderLayout.SOUTH);

			refreshValidationState();

			pack();
			setLocationRelativeTo(owner);
		}

		private void addLiveTextValidation(JTextField textField, Runnable... actions)
		{
			textField.getDocument().addDocumentListener(new DocumentListener()
			{
				@Override
				public void insertUpdate(DocumentEvent documentEvent)
				{
					for (Runnable action : actions)
					{
						action.run();
					}
				}

				@Override
				public void removeUpdate(DocumentEvent documentEvent)
				{
					for (Runnable action : actions)
					{
						action.run();
					}
				}

				@Override
				public void changedUpdate(DocumentEvent documentEvent)
				{
					for (Runnable action : actions)
					{
						action.run();
					}
				}
			});
		}

		private void updateCollectionSuggestions()
		{
			collectionSuggestionsPopupMenu.setVisible(false);
			collectionSuggestionsPopupMenu.removeAll();

			if (!itemCollectionCheckBox.isSelected())
			{
				return;
			}

			String input = itemIdentifierField.getText() == null ? "" : itemIdentifierField.getText().trim().toLowerCase(Locale.ROOT);
			if (input.isEmpty())
			{
				return;
			}

			for (ItemCollections itemCollection : ItemCollections.values())
			{
				if (itemCollection.name().equalsIgnoreCase(input))
				{
					return;
				}
			}

			int suggestionCount = 0;
			for (ItemCollections itemCollection : ItemCollections.values())
			{
				if (suggestionCount >= MAX_COLLECTION_SUGGESTION_RESULTS)
				{
					break;
				}
				if (!itemCollection.name().toLowerCase(Locale.ROOT).contains(input))
				{
					continue;
				}

				JMenuItem suggestionMenuItem = new JMenuItem(itemCollection.name());
				suggestionMenuItem.addActionListener(actionEvent -> {
					itemIdentifierField.setText(itemCollection.name());
					collectionSuggestionsPopupMenu.setVisible(false);
					itemIdentifierField.requestFocusInWindow();
				});
				collectionSuggestionsPopupMenu.add(suggestionMenuItem);
				suggestionCount++;
			}

			if (suggestionCount > 0)
			{
				collectionSuggestionsPopupMenu.show(itemIdentifierField, 0, itemIdentifierField.getHeight());
				itemIdentifierField.requestFocusInWindow();
			}
		}

		private String computeValidationError()
		{
			String itemText = itemIdentifierField.getText() == null ? "" : itemIdentifierField.getText().trim();
			String labelText = labelField.getText() == null ? "" : labelField.getText().trim();
			boolean isItemCollection = itemCollectionCheckBox.isSelected();

			if (itemText.isEmpty() && labelText.isEmpty())
			{
				return "You must provide an Item or a Label.";
			}

			if (isItemCollection && labelText.isEmpty())
			{
				return "A Label is required when Item Collection is checked.";
			}

			if (!itemText.isEmpty())
			{
				boolean isPurelyNumeric = itemText.matches("\\d+");
				if (isItemCollection)
				{
					if (isPurelyNumeric)
					{
						return "Invalid Item Collection.";
					}
					try
					{
						ItemCollections.valueOf(itemText.toUpperCase(Locale.ROOT));
					}
					catch (IllegalArgumentException invalidItemCollectionException)
					{
						return "\"" + itemText + "\" is not a known Item Collection.";
					}
				}
				else if (!isPurelyNumeric)
				{
					return "Item ID must be numeric (e.g. 4151).";
				}
			}

			return null;
		}

		private void refreshValidationState()
		{
			String validationError = computeValidationError();
			validationMessageLabel.setText(validationError != null ? validationError : " ");
			saveButton.setEnabled(validationError == null);
		}

		private boolean wasConfirmed()
		{
			return confirmed;
		}

		private ItemRequirement buildItemRequirement()
		{
			String itemText = itemIdentifierField.getText() == null ? "" : itemIdentifierField.getText().trim();
			String labelText = labelField.getText() == null ? "" : labelField.getText().trim();
			int quantity = (Integer) quantitySpinner.getValue();

			ItemRequirement itemRequirement;
			if (itemCollectionCheckBox.isSelected() && !itemText.isEmpty())
			{
				itemRequirement = new ItemRequirement(ItemCollections.valueOf(itemText.toUpperCase(Locale.ROOT)), quantity);
			}
			else if (!itemText.isEmpty())
			{
				itemRequirement = new ItemRequirement(Integer.parseInt(itemText), quantity);
			}
			else
			{
				itemRequirement = new ItemRequirement(-1, quantity);
			}

			if (!labelText.isEmpty())
			{
				itemRequirement.setLabel(labelText);
			}

			return itemRequirement;
		}
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
			itemsRequired.addAll(routeToEdit.getItems());
		}
		rebuildItemsListPanel(itemsRequiredListPanel, itemsRequired, "Required");

		if (routeToEdit.getRecommendedItems() != null)
		{
			itemsRecommended.addAll(routeToEdit.getRecommendedItems());
		}
		rebuildItemsListPanel(itemsRecommendedListPanel, itemsRecommended, "Recommended");
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

		routeToEdit.setItems(new ArrayList<>(itemsRequired));
		routeToEdit.setRecommendedItems(new ArrayList<>(itemsRecommended));
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