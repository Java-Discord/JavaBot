package net.discordjug.javabot.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Creates diagrams.
 */
public class Plotter {
	
	private String title;
	private final List<Pair<String, Bar>> entries;
	private int width=3000;
	private int height=1500;
	
	/**
	 * Creates the plotter.
	 * @param entries a list of all data points to plot, each represented as a {@link Pair} consisting of the name and value of the data point
	 * @param title the title of the plot
	 */
	public Plotter(List<Pair<String, Bar>> entries, String title) {
		this.entries = entries;
		this.title = title;
	}
	
	/**
	 * Create a diagram from the data supplied to the constructor.
	 * @return the diagram as a {@link BufferedImage}
	 */
	public BufferedImage plot() {
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = img.createGraphics();
		
		g2d.setFont(ImageGenerationUtils.getResourceFont("assets/fonts/Uni-Sans-Heavy.ttf", 30).orElseThrow());
		
		g2d.setBackground(Color.WHITE);
		g2d.fillRect(0, 0, width, height);
		g2d.setColor(Color.BLACK);
		
		centeredText(g2d, title, width/2, 50);
		
		plotEntries(g2d, 100, 100, width-200, height-200);
		
		return img;
	}

	private void plotEntries(Graphics2D g2d, int x, int y, int width, int height) {
		double maxValue = entries.stream().<Bar>map(Pair::second).mapToDouble(Bar::sum).max().orElse(0);
		int stepSize = 2*(int)Math.pow(10,(int)Math.log10(maxValue)-1);
		if (stepSize==0) {
			stepSize=1;
		}
		maxValue += stepSize;
		
		int numEntries = entries.size();
		
		int currentX = x;
		
		g2d.drawLine(x, y, x, y+height);
		
		if(maxValue>0) {
			for (int current = 0; current < maxValue; current += stepSize) {
				g2d.drawString(String.valueOf(current), 95-g2d.getFontMetrics().stringWidth(String.valueOf(current)), this.height-(y+(height*current)/(int)maxValue)+g2d.getFontMetrics().getHeight()/3);
			}
		}
		
		boolean shift=false;
		for (Pair<String, Bar> entry : entries) {
			int shiftNum = shift ? g2d.getFontMetrics().getHeight() : 0;
			centeredText(g2d, entry.first(), currentX+(width/(2*numEntries)), this.height-y/2+shiftNum);
			Bar bar = entry.second();
			int totalBarHeight = 0;
			
			double barSum = 0;
			
			for (Pair<Color, Double> barSquares : bar.elements()) {
				int entryHeight = (int)(height*barSquares.second()/maxValue);
				barSum += barSquares.second();
				g2d.setColor(barSquares.first());
				int start = this.height-y-entryHeight - totalBarHeight;
				g2d.fillRect(currentX, start, width/numEntries, entryHeight);
				g2d.setColor(Color.BLACK);
				g2d.drawRect(currentX, start, width/numEntries, entryHeight);
				totalBarHeight += entryHeight;
			}
			centeredText(g2d, String.valueOf(Math.round(barSum*100)/100.0), currentX+(width/(2*numEntries)), this.height-y-totalBarHeight-10);
			shift=!shift;
			currentX += width/numEntries;
		}
	}
	
	private void centeredText(Graphics2D g2d, String text, int x, int y) {
		g2d.drawString(text, x-g2d.getFontMetrics().stringWidth(text)/2, y);
	}
	
	/**
	 * A single bar which should be plotted.
	 * 
	 * @param elements any number of the entries to plot
	 */
	public record Bar(List<Pair<Color,Double>> elements) {
		public Bar(double singleElement) {
			this(List.of(new Pair<>(Color.GRAY, singleElement)));
		}
		
		private double sum() {
			return elements.stream().mapToDouble(Pair::second).sum();
		}
	}
}
