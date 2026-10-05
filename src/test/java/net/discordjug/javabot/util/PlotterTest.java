package net.discordjug.javabot.util;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class PlotterTest {
    @Test
    public void ImageDifferenceTest() throws IOException{
        List<Pair<String, Plotter.Bar>> testData = testData0();
        BufferedImage generatedImage = new Plotter(testData, "General helper statistics","subtitle").plot();
        BufferedImage expectedImage = readImage("PlotterTestImages/PlotterTest-Light.png");

        assertTrue(assertImagesAreEqual(generatedImage,expectedImage));
    }

    @Test
    public void ImageDifferenceTestDarkMode() throws IOException{
        List<Pair<String, Plotter.Bar>> testData = testData0();
        BufferedImage generatedImage = new Plotter(testData, "General helper statistics","subtitle",true).plot();
        BufferedImage expectedImage = readImage("PlotterTestImages/PlotterTest-Dark.png");

        assertTrue(assertImagesAreEqual(generatedImage,expectedImage));
    }

    @Test
    public void ImageDifferenceTestLessData() throws IOException{
        List<Pair<String, Plotter.Bar>> testData = testData1();
        BufferedImage generatedImage = new Plotter(testData, "General helper statistics","subtitle").plot();
        BufferedImage expectedImage = readImage("PlotterTestImages/PlotterTest-LessData.png");

        assertTrue(assertImagesAreEqual(generatedImage,expectedImage));
    }

    @Test
    public void ImageDifferenceTestNoData() throws IOException{
        List<Pair<String, Plotter.Bar>> testData = testData2();
        BufferedImage generatedImage = new Plotter(testData, "General helper statistics","subtitle").plot();
        BufferedImage expectedImage = readImage("PlotterTestImages/PlotterTest-NoData.png");

        assertTrue(assertImagesAreEqual(generatedImage,expectedImage));
    }

    private BufferedImage readImage(String fileName) throws IOException{
        URL resource = getClass().getClassLoader().getResource(fileName);
        BufferedImage image = ImageIO.read(resource);
        return image;
    }

    public static boolean assertImagesAreEqual(BufferedImage actualImage, BufferedImage expectedImage) throws IOException {
        assertNotNull(actualImage, "Generated Image is Null.");
        assertNotNull(expectedImage, "Expected Image is Null.");

        String actualImageBase64 = convertToBase64(actualImage);

        assertEquals(expectedImage.getWidth(), actualImage.getWidth(), "Image width does not match.\nActual image:"+actualImageBase64);
        assertEquals(expectedImage.getHeight(), actualImage.getHeight(), "Image height does not match.\nActual image:"+actualImageBase64);

        for (int y = 0; y < actualImage.getHeight(); y++) {
            for (int x = 0; x < actualImage.getWidth(); x++) {
                assertEquals(actualImage.getRGB(x, y),expectedImage.getRGB(x, y),() -> "Image does not match.\nActual image:"+actualImageBase64);
            }
        }
        return true;
    }

    private static String convertToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static List<Pair<String, Plotter.Bar>> testData0() {
        Color yellow = new Color(249,199,79);
        Color red = new Color(249,80,110);
        Color grey = new Color(108,114,128);
        Color blue = new Color(91,140,255);

        List<Pair<String, Plotter.Bar>> entries = Arrays.asList(
            new Pair<>("SEP '26", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 145.6),
                    new Pair<>(red, 133.8),
                    new Pair<>(grey, 1698.6),
                    new Pair<>(blue, 1288.5)
            ))),
            new Pair<>("OCT '26", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1630.9),
                    new Pair<>(red, 695.3),
                    new Pair<>(grey, 532.5)
            ))),
            new Pair<>("NOV '26", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1383.6),
                    new Pair<>(red, 1796.8),
                    new Pair<>(grey, 315.9),
                    new Pair<>(blue, 819.6)
            ))),
            new Pair<>("DEC '2026", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 726.5),
                    new Pair<>(red, 360.4),
                    new Pair<>(grey, 1090.1)
            ))),
            new Pair<>("JAN '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 488.6),
                    new Pair<>(red, 689.0),
                    new Pair<>(grey, 327.6),
                    new Pair<>(blue, 529.8)
            ))),
            new Pair<>("FEB '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1049.3),
                    new Pair<>(red, 1065.0),
                    new Pair<>(grey, 1366.8)
            ))),
            new Pair<>("MAR '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 603.9),
                    new Pair<>(red, 398.4),
                    new Pair<>(grey, 69.3),
                    new Pair<>(blue, 1396.6)
            ))),
            new Pair<>("APR '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1298.4),
                    new Pair<>(red, 891.0),
                    new Pair<>(grey, 560.4)
            ))),
            new Pair<>("MAY '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1092.8),
                    new Pair<>(red, 380.3),
                    new Pair<>(grey, 712.2)
            ))),
            new Pair<>("JUN '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 680.4),
                    new Pair<>(red, 810.6),
                    new Pair<>(grey, 850.3),
                    new Pair<>(blue, 877.2)
            ))),
            new Pair<>("JUL '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1010.6),
                    new Pair<>(red, 115.4),
                    new Pair<>(grey, 142.0),
                    new Pair<>(blue, 1430.0)
            ))),
            new Pair<>("AUG '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 541.0),
                    new Pair<>(red, 392.9),
                    new Pair<>(grey, 363.7),
                    new Pair<>(blue, 1564.9)
            ))),
            new Pair<>("SEP '27", new Plotter.Bar(Arrays.asList(
                    new Pair<>(yellow, 1009.2),
                    new Pair<>(red, 64.2),
                    new Pair<>(grey, 1375.8),
                    new Pair<>(blue, 1154.3)
            )))
        );
        return entries;
    }

    private static List<Pair<String, Plotter.Bar>> testData1() {
        Color yellow = new Color(249,199,79);
        Color red = new Color(249,80,110);
        Color grey = new Color(108,114,128);
        Color blue = new Color(91,140,255);

        List<Pair<String, Plotter.Bar>> entries = Arrays.asList(
                new Pair<>("SEP '26", new Plotter.Bar(Arrays.asList(
                        new Pair<>(yellow, 145.6),
                        new Pair<>(red, 133.8),
                        new Pair<>(grey, 1698.6),
                        new Pair<>(blue, 1288.5)
                ))),
                new Pair<>("OCT '26", new Plotter.Bar(Arrays.asList(
                        new Pair<>(yellow, 1630.9),
                        new Pair<>(red, 695.3),
                        new Pair<>(grey, 532.5)
                ))),
                new Pair<>("NOV '26", new Plotter.Bar(Arrays.asList(
                        new Pair<>(yellow, 1383.6),
                        new Pair<>(red, 1796.8),
                        new Pair<>(grey, 315.9),
                        new Pair<>(blue, 819.6)
                ))),
                new Pair<>("DEC '2026", new Plotter.Bar(Arrays.asList(
                        new Pair<>(yellow, 726.5),
                        new Pair<>(red, 360.4),
                        new Pair<>(grey, 1090.1)
                )))
        );
        return entries;
    }

    private static List<Pair<String, Plotter.Bar>> testData2() {
        return new ArrayList<Pair<String, Plotter.Bar>>();
    }
}
