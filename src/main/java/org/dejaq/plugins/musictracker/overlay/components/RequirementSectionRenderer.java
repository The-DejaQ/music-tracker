package org.dejaq.plugins.musictracker.overlay.components;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import org.dejaq.plugins.musictracker.MusicTrackerPlugin;
import org.dejaq.plugins.musictracker.quest.QuestState;
import org.dejaq.plugins.musictracker.requirement.DynamicRequirement;
import org.dejaq.plugins.musictracker.track.Route;

public class RequirementSectionRenderer
{
	public <T> void renderRequirementList(
		PanelComponent panelComponent,
		String sectionHeaderText,
		List<DynamicRequirement<T>> dynamicRequirements,
		List<T> staticRequirements,
		Function<T, String> displayTextResolver,
		Predicate<T> satisfactionChecker,
		Color unsatisfiedColorWhenRequirementKnown,
		Color unsatisfiedColorWhenRequirementUnknown)
	{
		renderRequirementList(panelComponent, sectionHeaderText, dynamicRequirements, staticRequirements,
			displayTextResolver, satisfactionChecker, unsatisfiedColorWhenRequirementKnown, unsatisfiedColorWhenRequirementUnknown,
			staticRequirement -> true);
	}

	public <T> void renderRequirementList(
		PanelComponent panelComponent,
		String sectionHeaderText,
		List<DynamicRequirement<T>> dynamicRequirements,
		List<T> staticRequirements,
		Function<T, String> displayTextResolver,
		Predicate<T> satisfactionChecker,
		Color unsatisfiedColorWhenRequirementKnown,
		Color unsatisfiedColorWhenRequirementUnknown,
		Predicate<T> hasRealBackingData)
	{
		if (dynamicRequirements != null && !dynamicRequirements.isEmpty())
		{
			addSectionHeader(panelComponent, sectionHeaderText);
			for (DynamicRequirement<T> dynamicRequirement : dynamicRequirements)
			{
				Color lineColor = resolveDynamicRequirementColor(dynamicRequirement, satisfactionChecker,
					unsatisfiedColorWhenRequirementKnown, unsatisfiedColorWhenRequirementUnknown);
				addLine(panelComponent, dynamicRequirement.getDisplayText(), lineColor);
			}
			return;
		}

		if (staticRequirements == null || staticRequirements.isEmpty())
		{
			return;
		}

		addSectionHeader(panelComponent, sectionHeaderText);
		for (T staticRequirement : staticRequirements)
		{
			Color lineColor;
			if (!hasRealBackingData.test(staticRequirement))
			{
				lineColor = ColorScheme.TEXT_COLOR;
			}
			else
			{
				boolean isSatisfied = satisfactionChecker.test(staticRequirement);
				lineColor = isSatisfied ? ColorScheme.PROGRESS_COMPLETE_COLOR : unsatisfiedColorWhenRequirementKnown;
			}
			addLine(panelComponent, displayTextResolver.apply(staticRequirement), lineColor);
		}
	}

	private <T> Color resolveDynamicRequirementColor(
		DynamicRequirement<T> dynamicRequirement,
		Predicate<T> satisfactionChecker,
		Color unsatisfiedColorWhenRequirementKnown,
		Color unsatisfiedColorWhenRequirementUnknown)
	{
		if (dynamicRequirement.getColor() != null)
		{
			return dynamicRequirement.getColor();
		}
		if (dynamicRequirement.getRequirement() != null)
		{
			boolean isSatisfied = satisfactionChecker.test(dynamicRequirement.getRequirement());
			return isSatisfied ? ColorScheme.PROGRESS_COMPLETE_COLOR : unsatisfiedColorWhenRequirementKnown;
		}
		return unsatisfiedColorWhenRequirementUnknown;
	}

	public void renderQuestSection(PanelComponent panelComponent, MusicTrackerPlugin musicTrackerPlugin, DynamicRequirement<String> dynamicQuestRequirement, Route currentRoute)
	{
		boolean hasDynamicQuestText = dynamicQuestRequirement != null
			&& dynamicQuestRequirement.getDisplayText() != null
			&& !dynamicQuestRequirement.getDisplayText().isBlank();

		if (hasDynamicQuestText)
		{
			addSectionHeader(panelComponent, "Quest Required:");
			Color questLineColor = dynamicQuestRequirement.getColor() != null
				? dynamicQuestRequirement.getColor()
				: resolveStaticQuestColor(musicTrackerPlugin, currentRoute);
			addLine(panelComponent, dynamicQuestRequirement.getDisplayText(), questLineColor);
			return;
		}

		if (currentRoute.getQuest() != null)
		{
			addSectionHeader(panelComponent, "Quest Required:");
			addLine(panelComponent, currentRoute.getQuest().getName(), resolveStaticQuestColor(musicTrackerPlugin, currentRoute));
		}
	}

	private Color resolveStaticQuestColor(MusicTrackerPlugin musicTrackerPlugin, Route currentRoute)
	{
		if (currentRoute.getQuest() == null)
		{
			return ColorScheme.PROGRESS_ERROR_COLOR;
		}

		QuestState questState = musicTrackerPlugin.getPlayerState()
			.getCachedQuestState(currentRoute.getQuest());

		return questState == QuestState.FINISHED
			? ColorScheme.PROGRESS_COMPLETE_COLOR
			: ColorScheme.PROGRESS_ERROR_COLOR;
	}

	public void renderNotesSection(PanelComponent panelComponent, DynamicRequirement<String> dynamicNotesRequirement, Route currentRoute)
	{
		String notesText = null;
		Color notesColor = Color.LIGHT_GRAY;

		boolean hasDynamicNotesText = dynamicNotesRequirement != null
			&& dynamicNotesRequirement.getDisplayText() != null
			&& !dynamicNotesRequirement.getDisplayText().isBlank();

		if (hasDynamicNotesText)
		{
			notesText = dynamicNotesRequirement.getDisplayText();
			if (dynamicNotesRequirement.getColor() != null)
			{
				notesColor = dynamicNotesRequirement.getColor();
			}
		}
		else if (currentRoute.getNotes() != null && !currentRoute.getNotes().isBlank())
		{
			notesText = currentRoute.getNotes();
		}

		if (notesText != null)
		{
			addSectionHeader(panelComponent, "Notes:");
			addLine(panelComponent, notesText, notesColor);
		}
	}

	private void addSectionHeader(PanelComponent panelComponent, String headerText)
	{
		addSpace(panelComponent);
		panelComponent.getChildren().add(LineComponent.builder().left(headerText).leftColor(Color.WHITE).build());
	}

	private void addLine(PanelComponent panelComponent, String text, Color color)
	{
		panelComponent.getChildren().add(LineComponent.builder().left(text).leftColor(color).build());
	}

	public void addSpace(PanelComponent panelComponent)
	{
		panelComponent.getChildren().add(LineComponent.builder().left(" ").build());
	}
}