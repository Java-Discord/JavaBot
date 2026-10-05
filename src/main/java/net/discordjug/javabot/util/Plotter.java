package net.discordjug.javabot.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Creates diagrams.
 */
public class Plotter {
	private static final int WIDTH = 3000;
	private static final int HEIGHT = 1500;

	private static final int GRID_LINES = 5;
	private static final int BAR_LABEL_MARGIN = 34;
	private static final int BAR_LABEL_HEIGHT = 42;
	private static final int BAR_LABEL_BOTTOM_MARGIN = 65;
	private static final int TITLE_FONT_SIZE = 57;
	private static final int SUBTITLE_FONT_SIZE = 27;
	private static final int AXIS_FONT_SIZE = 24;
	private static final int HEADING_MARGIN_LEFT = (int) (WIDTH * 0.02);               // 2%
	private static final int AXIS_LABEL_MARGIN_LEFT = (int) (WIDTH * 0.045);           // 4.5%
	private static final int GRAPH_MARGIN_LEFT = (int) (WIDTH * 0.05);                 // 5%
	private static final int GRAPH_MARGIN_RIGHT = (int) (WIDTH * 0.02);                // 2%
	private static final int AXIS_LABEL_MARGIN_BOTTOM = (int) (HEIGHT * 0.043);        // 4.3%
	private static final int HEADING_MARGIN_TOP = (int) (HEIGHT * 0.06);               // 6%
	private static final int SUBHEADING_MARGIN_TOP = (int) (HEIGHT * 0.09);            // 9%
	private static final int GRAPH_MARGIN_TOP = (int) (HEIGHT * 0.15);                 // 15%
	private static final int GRAPH_MARGIN_BOTTOM = (int) (HEIGHT * 0.09);              // 9%
	private static final int LABEL_SIZE = (int) (HEIGHT * 0.016);                      // 1.6%
	private static final int VALUE_SIZE = (int) (HEIGHT * 0.016);                      // 1.6%
	private static final int GRAPH_WIDTH = WIDTH - GRAPH_MARGIN_LEFT - GRAPH_MARGIN_RIGHT;
	private static final int GRAPH_HEIGHT = HEIGHT - GRAPH_MARGIN_TOP - GRAPH_MARGIN_BOTTOM;
	private static final int ARC_SIZE = 18;
	private static final double BAR_WIDTH_RATIO = 0.42;
	private static final int BAR_WIDTH_MAX = 110;

	private final Color borderColor;
	private final Color backgroundColor;
	private final Color gridLineColor;
	private final Color gridLineColorStrong;
	private final Color textColor;
	private final Color textColorMuted;
	private final Color textColorDim;
	private final Color barLabelColor;

	private final String titleText;
	private final String subtitleText;
	private final List<Pair<String, Bar>> entries;

	/**
	 * Creates the plotter.
	 *
	 * @param entries      a list of all data points to plot, each represented as a {@link Pair} consisting of the name and value of the data point
	 * @param titleText    the title of the plot
	 * @param subtitleText the subtitle of plot
	 */
	public Plotter(List<Pair<String, Bar>> entries, String titleText, String subtitleText) {
		this.entries = entries;
		this.titleText = titleText;
		this.subtitleText = subtitleText;
		borderColor = Color.BLACK;
		backgroundColor = Color.decode("#EAEDF5");
		gridLineColor = Color.decode("#B2B2B2");
		gridLineColorStrong = Color.decode("#606061");
		textColor = Color.decode("#2D2D2D");
		textColorMuted = Color.decode("#42474D");
		textColorDim = Color.decode("#59616D");
		barLabelColor = Color.decode("#D9D9D9");
	}

	/**
	 * Creates the plotter.
	 *
	 * @param entries      a list of all data points to plot, each represented as a {@link Pair} consisting of the name and value of the data point
	 * @param titleText    the title of the plot
	 * @param subtitleText the subtitle of plot
	 * @param darkMode     {@code true} if the plot should be generated in dark mode, otherwise {@code false}
	 */
	public Plotter(List<Pair<String, Bar>> entries, String titleText, String subtitleText, boolean darkMode) {
		this.entries = entries;
		this.titleText = titleText;
		this.subtitleText = subtitleText;
		borderColor = Color.BLACK;
		if (darkMode) {
			backgroundColor = Color.decode("#111318");
			gridLineColor = Color.decode("#252A32");
			gridLineColorStrong = Color.decode("#303640");
			textColor = Color.decode("#F5F7FA");
			barLabelColor = Color.decode("#333333");
			textColorMuted = Color.decode("#C9C9C9");
			textColorDim = Color.decode("#E3E3E3");
		} else {
			backgroundColor = Color.decode("#EAEDF5");
			gridLineColor = Color.decode("#B2B2B2");
			gridLineColorStrong = Color.decode("#606061");
			textColor = Color.decode("#2D2D2D");
			barLabelColor = Color.decode("#D9D9D9");
			textColorMuted = Color.decode("#42474D");
			textColorDim = Color.decode("#59616D");
		}
	}

	/**
	 * Create a diagram from the data supplied to the constructor.
	 * @return the diagram as a {@link BufferedImage}
	 */
	public BufferedImage plot() {
		BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = img.createGraphics();

		fillBackground(graphics2D);

		drawHeadings(graphics2D);
		drawGraph(graphics2D);

		return img;
	}

	private void drawHeadings(Graphics2D graphics2D) {
		Font titleFont = ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", TITLE_FONT_SIZE).orElseThrow();
		Font subtitleFont = ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", SUBTITLE_FONT_SIZE).orElseThrow();

		graphics2D.setColor(textColor);
		graphics2D.setFont(titleFont);
		graphics2D.drawString(titleText, HEADING_MARGIN_LEFT, HEADING_MARGIN_TOP);

		graphics2D.setColor(textColorMuted);
		graphics2D.setFont(subtitleFont);
		graphics2D.drawString(subtitleText, HEADING_MARGIN_LEFT, SUBHEADING_MARGIN_TOP);

	}

	private void fillBackground(Graphics2D graphics2D) {
		graphics2D.setColor(backgroundColor);
		graphics2D.fillRect(-1, -1, WIDTH, HEIGHT);
	}

	private void drawGraph(Graphics2D graphics2D) {
		double maxValue = entries.stream().map(Pair::second).mapToDouble(Bar::sum).max().orElse(0);

		if (maxValue == 0) {
			return;
		}
		double axisMax = niceMaximum(maxValue);

		drawGridLines(graphics2D, axisMax);
		drawBars(graphics2D, axisMax);
	}

	private void drawGridLines(Graphics2D graphics2D, double axisMax) {
		Font axisFont = ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", AXIS_FONT_SIZE).orElseThrow();
		graphics2D.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));    // Use a 2px stroke because a line is between pixels.
		graphics2D.setFont(axisFont);

		for (int i = 0; i <= GRID_LINES; i++) {
			double fraction = (double) i / GRID_LINES;
			int gridY = GRAPH_MARGIN_TOP + GRAPH_HEIGHT - (int) (GRAPH_HEIGHT * fraction);

			graphics2D.setColor(i == 0 ? gridLineColorStrong : gridLineColor);
			graphics2D.drawLine(GRAPH_MARGIN_LEFT, gridY + 1, GRAPH_MARGIN_LEFT + GRAPH_WIDTH, gridY + 1);    // Move 1px down so the 2px stroke aligns with the grid position.

			double value = axisMax * fraction;

			String label = formatValue(value);
			graphics2D.setColor(textColorDim);
			graphics2D.drawString(label, AXIS_LABEL_MARGIN_LEFT - (graphics2D.getFontMetrics().stringWidth(label)), gridY + (graphics2D.getFontMetrics().getAscent() + graphics2D.getFontMetrics().getDescent()) / 2);
		}
	}

	private void drawBars(Graphics2D graphics2D, double axisMax) {
		int count = entries.size();
		int slotWidth = GRAPH_WIDTH / count;
		Font labelFont = ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", LABEL_SIZE).orElseThrow();

		for (int i = 0; i < count; i++) {
			Pair<String, Bar> entry = entries.get(i);
			Bar bar = entry.second();

			int centerX = GRAPH_MARGIN_LEFT + (slotWidth * i) + (slotWidth / 2);
			int barWidth = Math.min(BAR_WIDTH_MAX, (int) (slotWidth * BAR_WIDTH_RATIO));
			int barX = centerX - barWidth / 2;

			drawBarLabel(graphics2D, axisMax, centerX, bar);
			drawSegmentedBars(graphics2D, bar, barX, barWidth, axisMax);

			graphics2D.setFont(labelFont);
			graphics2D.setColor(textColorMuted);

			String label = entry.first();
			drawStringCentered(graphics2D, label, centerX, GRAPH_MARGIN_TOP + GRAPH_HEIGHT + AXIS_LABEL_MARGIN_BOTTOM);
		}
	}

	private void drawBarLabel(Graphics2D graphics2D, double axisMax, int centerX, Bar bar) {
		int barBottom = GRAPH_MARGIN_TOP + GRAPH_HEIGHT;
		double barTotal = bar.sum();
		int totalBarHeight = (int) (GRAPH_HEIGHT * (barTotal / axisMax));
		int barLabelY = barBottom - totalBarHeight - BAR_LABEL_BOTTOM_MARGIN;

		Font valueFont = ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", VALUE_SIZE).orElseThrow();
		graphics2D.setFont(valueFont);

		String totalText = formatValue(barTotal);
		int textWidth = graphics2D.getFontMetrics().stringWidth(totalText);
		int barLabelWidth = textWidth + BAR_LABEL_MARGIN;

		graphics2D.setColor(barLabelColor);
		graphics2D.fillRoundRect(centerX - barLabelWidth / 2, barLabelY, barLabelWidth, BAR_LABEL_HEIGHT, ARC_SIZE, ARC_SIZE);
		graphics2D.setColor(textColor);
		drawStringCentered(graphics2D,totalText,centerX, barLabelY + 29);
	}

	private void drawSegmentedBars(Graphics2D graphics2D, Bar bar,int barX, int barWidth, double axisMax) {
		int barBottom = GRAPH_MARGIN_TOP + GRAPH_HEIGHT;
		int barSum = 0;
		int incrementalBarHeight = 0;

		for (Pair<Color, Double> element : bar.elements()) {
			double value = element.second();
			barSum += value;
			int expectedBarHeight = (int) (GRAPH_HEIGHT * (barSum / axisMax));
			int segmentHeight = expectedBarHeight - incrementalBarHeight;
			incrementalBarHeight += segmentHeight;
			int currentY = barBottom - incrementalBarHeight;

			if (segmentHeight > 0) {
				graphics2D.setColor(element.first());
				graphics2D.fillRect(barX, currentY, barWidth, segmentHeight);

				graphics2D.setColor(borderColor);
				graphics2D.drawRect(barX - 1, currentY - 1, barWidth + 1, segmentHeight + 1);
			}
		}
	}

	private void drawStringCentered(Graphics2D graphics2D,String text,int x, int y){
		int textWidth = graphics2D.getFontMetrics().stringWidth(text);
		graphics2D.drawString(text,x- textWidth / 2, y);
	}

	private String formatValue(double value) {
		if (value >= 1_000_000) {
			return String.format("%.1fM", value / 1_000_000);
		}

		if (value >= 1_000) {
			return String.format("%.1fK", value / 1_000);
		}

		if (value % 1 == 0) {
			return String.format("%.0f", value);
		}

		return String.format("%.2f", value);
	}

	private double niceMaximum(double value) {
		double magnitude = Math.pow(10, Math.floor(Math.log10(value)));
		double normalized = value / magnitude;
		double nice;

		if (normalized <= 1) {
			nice = 1;
		} else if (normalized <= 2.5) {
			nice = 2.5;
		} else if (normalized <= 5) {
			nice = 5;
		} else {
			nice = 10;
		}

		return nice * magnitude;
	}

	/**
	 * A single bar which should be plotted.
	 * @param elements any number of the entries to plot
	 */
	public record Bar(List<Pair<Color, Double>> elements) {
		public Bar(double singleElement) {
			this(List.of(new Pair<>(Color.GRAY, singleElement)));
		}

		private double sum() {
			return elements.stream().mapToDouble(Pair::second).sum();
		}
	}
}
