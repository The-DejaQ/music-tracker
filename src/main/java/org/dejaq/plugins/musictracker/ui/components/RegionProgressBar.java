package org.dejaq.plugins.musictracker.ui.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;

public class RegionProgressBar extends JComponent
{
	private static final int BAR_WIDTH_PIXELS = 54;
	private static final int BAR_HEIGHT_PIXELS = 12;

	private static final Color FILLED_COLOR = ColorScheme.PROGRESS_COMPLETE_COLOR;
	private static final Color REMAINING_COLOR = ColorScheme.PROGRESS_ERROR_COLOR;
	private static final Color BORDER_COLOR = ColorScheme.DARK_GRAY_COLOR;

	private double percentCompleted;

	public RegionProgressBar()
	{
		Dimension fixedSize = new Dimension(BAR_WIDTH_PIXELS, BAR_HEIGHT_PIXELS);
		setPreferredSize(fixedSize);
		setMinimumSize(fixedSize);
		setMaximumSize(fixedSize);
		setOpaque(false);
	}

	public void setPercentCompleted(double percentCompleted)
	{
		double clampedPercentCompleted = Math.max(0d, Math.min(100d, percentCompleted));
		if (clampedPercentCompleted != this.percentCompleted)
		{
			this.percentCompleted = clampedPercentCompleted;
			repaint();
		}
	}

	@Override
	protected void paintComponent(Graphics graphics)
	{
		Graphics2D graphics2D = (Graphics2D) graphics.create();
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		int barWidth = getWidth();
		int barHeight = getHeight();

		graphics2D.setColor(REMAINING_COLOR);
		graphics2D.fillRect(0, 0, barWidth, barHeight);

		int filledWidth = (int) Math.round(barWidth * (percentCompleted / 100d));
		if (filledWidth > 0)
		{
			graphics2D.setColor(FILLED_COLOR);
			graphics2D.fillRect(0, 0, filledWidth, barHeight);
		}

		graphics2D.setColor(BORDER_COLOR);
		graphics2D.drawRect(0, 0, barWidth - 1, barHeight - 1);

		graphics2D.dispose();
	}
}