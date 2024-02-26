import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.swing.*;
import java.lang.Math;

public class HistogramExample {

    public class pixel{
        int x,y, pix;
        int hue;
        double saturation;
        boolean partofSubImage;
        public pixel(int xCoordinate,int yCoordinate,int pixelValue){
            x = xCoordinate;
            y = yCoordinate;
            pix = pixelValue;

            int r = (pix >> 16) & 0xFF;
            int g = (pix >> 8) & 0xFF;
            int b = (pix) & 0xFF;

            int M = Math.max(Math.max(r,g),b);
            int m = Math.min(Math.min(r,g),b);
            int C = M - m;
            double H;
            if (C == 0){
                H = 0;
            } else if (M == r) {
                H = (Math.abs(g - b) / (double) C) % 6;
            } else if (M == g) {
                H = (Math.abs(b - r) / (double) C) + 2;
            }else {
                H = (Math.abs(r - g) / (double) C) + 4;
            }
            H *= 60;
            hue = (int) Math.round(H);

            double S;
            if (M == 0){
                S = 0;
            }else {
                S = (double) C / (double) M;
            }
            saturation = S;
        }

    }

    JFrame frame;
    JLabel lbIm1;

    void displayImage(BufferedImage image){
        frame = new JFrame();

        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        lbIm1 = new JLabel(new ImageIcon(image));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 0;
        c.gridy = 0;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;

        frame.getContentPane().add(lbIm1, c);
        frame.pack();
        frame.setVisible(true);
    }

    public void readRGBImage(int width, int height, String imgPath, BufferedImage img){
        try {
            //System.out.println((imgPath));
            int frameLength = width * height * 3;

            File file = new File(imgPath);
            RandomAccessFile raf = new RandomAccessFile(file, "r");
            raf.seek(0);

            long len = frameLength;
            byte[] bytes = new byte[(int) len];
            raf.read(bytes);
            int ind = 0;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    byte a = 0;
                    byte r = bytes[ind];
                    byte g = bytes[ind + height * width];
                    byte b = bytes[ind + height * width * 2];

                    int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
                    //int pix = ((a << 24) + (r << 16) + (g << 8) + b);
                    img.setRGB(x, y, pix);
                    ind++;
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getHue(int r, int g, int b){
        int M = Math.max(Math.max(r,g),b);
        int m = Math.min(Math.min(r,g),b);
        int C = M - m;
        double H;
        if (C == 0){
            H = 0;
        } else if (M == r) {
            H = (Math.abs(g - b) / (double) C) % 6;
        } else if (M == g) {
            H = (Math.abs(b - r) / (double) C) + 2;
        }else {
            H = (Math.abs(r - g) / (double) C) + 4;
        }
        H *= 60;
        return (int) Math.round(H);
    }



    public void createHistogram(BufferedImage image, int width, int height, int[] hueHistogram, int[] saturationHistogram){
        //int val = 360/numberOfHistogramBins;

        for (int y = 0; y < height; y++){
            for (int x = 0; x < width; x++){
                int pix = image.getRGB(x,y);
                int r = (pix >> 16) & 0xFF;
                int g = (pix >> 8) & 0xFF;
                int b = (pix) & 0xFF;
                hueHistogram[getHue(r,g,b)]++;
                //saturationHistogram[(int) (getSaturation(r,g,b)*100)]++;
            }
        }
    }

    public void printHistValues(int[] hist){
        System.out.println("------------");
        for (int i = 0; i < hist.length; i++){
            if (hist[i] != 0){
                System.out.print(i);
                System.out.print(" ");
                System.out.println(hist[i]);
            }
        }
    }

    public void eliminateBackgroundValues(int[] hueHistogram){
        int val = -1;
        for (int i = 0; i < 360;i++){
            if (hueHistogram[i] >= 100000){
                val = i;
                break;
            }
        }

        for (int i = 0; i<= 5; i++){
            if (val + i < 360) {
                hueHistogram[val + i] = 0;
            }
            if (val - i >= 0) {
                hueHistogram[val - i] = 0;
            }
        }
    }

    public List findValidHueBins(int[] hueHistogram){
        List<Integer> binList = new ArrayList<>();
        for (int i = 0; i < 360; i++){
            if (hueHistogram[i] >= 1000){
                binList.add(i);
            }
        }
        return binList;
    }

    public boolean checkIfObjectIsPresentinImage(int[] imageHistogram, List histogram){
        //List imgHistList = Arrays.asList(imageHistogram);
        for (int i = 0; i< histogram.size(); i++){
            int bin = (Integer) histogram.get(i);
            int flag = 0;
            for (int j = 0; j < imageHistogram.length; j++){
                if ((bin == j) & (imageHistogram[j] > 0)){
                    flag = 1;
                    break;
                }
            }
            if (flag == 0){
                System.out.println(bin);
                return false;
            }

        }
        return true;
    }

    public List<Point> getNeighbours(int[][] matrix, int x, int y, int eps){
        int xMin = Math.max((x - eps), 0);
        int xMax = Math.min((x + eps), 640);
        int yMin = Math.max((y - eps), 0);
        int yMax = Math.min((y + eps), 380);
        List<Point> neighbours = new ArrayList<>();
        for (int i = yMin; i < yMax; i++){
            for (int j = xMin; j < xMax; j++){
                if (matrix[i][j] == 1){
                    Point p = new Point();
                    p.x = j;
                    p.y = i;
                    neighbours.add(p);
                }
            }
        }
        return neighbours;
    }

    public int mapClusterIndex(List<Point> validPoints, Point point){
        int i = 0;
        while (i < validPoints.size()){
            if (validPoints.get(i) == point){
                return i;
            }
            i++;
        }
        return -1;
    }

    public int dfs(int row, int col, int[] rectCoordinates, int[][] matrix, int height, int width){
        if ((row < 0) | (row >= height) | (col < 0) | (col >= width)){
            return 0;
        }
        if (matrix[row][col] == 0){
            return 0;
        }
        matrix[row][col] = 0;
        rectCoordinates[0] = Math.min(col,rectCoordinates[0]);
        rectCoordinates[1] = Math.max(col,rectCoordinates[1]);
        rectCoordinates[2] = Math.min(row,rectCoordinates[2]);
        rectCoordinates[3] = Math.max(row,rectCoordinates[3]);
        int clusterSize = 1;

        for (int i = -10; i <= 10;i+=1){
            for (int j = -10; j <= 10; j+=1){
                if (((i%2 != 0) & (j%2 == 0)) | ((i%2 == 0) & (j%2 != 0)) | ((i%2 != 0) & (j%2 != 0))){
                    if ((row + i >= 0) & (row+i < height) & (col+j >= 0) & (col+j < width)) {
                        clusterSize += matrix[row + i][col + j];
                        matrix[row + i][col + j] = 0;
                    }
                }
            }
        }
        for (int i = -10; i <= 10; i+=2){
            for (int j = -10; j <= 10; j+=2){
                if ((i != 0) | (j != 0)){
                    /*System.out.print(row+i);
                    System.out.print(" ");
                    System.out.println(col+j);*/
                    clusterSize += dfs(row + i, col + j, rectCoordinates, matrix, height, width);
                }
            }
        }
        return clusterSize;
    }

    public void clustering(int[][] matrix, BufferedImage image, int height, int width, String objectName){
        int maxCluster = 0;
        for (int row = 0; row < height; row++){
            for (int col = 0; col < width; col++){
                if (matrix[row][col] == 1){
                    int[] rectCoordinates = new int[4];
                    rectCoordinates[0] = 640;  //xMin
                    rectCoordinates[1] = 0;    //xMax
                    rectCoordinates[2] = 480;  //yMin
                    rectCoordinates[3] = 0;    //yMax
                    int clusterSize = dfs(row, col, rectCoordinates, matrix, height, width);
                    maxCluster = Math.max(maxCluster, clusterSize);
                    if (clusterSize >= 4000){
                        System.out.println(clusterSize);
                        Graphics2D g2d = image.createGraphics();
                        g2d.setStroke(new java.awt.BasicStroke(3));
                        g2d.drawRect(rectCoordinates[0], rectCoordinates[2], rectCoordinates[1] - rectCoordinates[0], rectCoordinates[3] - rectCoordinates[2]);
                        g2d.drawString(objectName, (rectCoordinates[1] - rectCoordinates[0])/3 + rectCoordinates[0], rectCoordinates[3] - 6);
                        g2d.dispose();
                        for (int i = 0; i< rectCoordinates.length;i++){
                            System.out.println(rectCoordinates[i]);
                        }
                    }
                }
            }
        }
        System.out.println(maxCluster);

    }


    public void detectObject(BufferedImage image, int width, int height, List objectHueBinList, String objectName){
        int[][] matrix = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pix = image.getRGB(x,y);
                int r = (pix >> 16) & 0xFF;
                int g = (pix >> 8) & 0xFF;
                int b = (pix) & 0xFF;
                int h = getHue(r,g,b);
                //double s = getSaturation(r,g,b);
                if (objectHueBinList.contains(h)){
                    //image.setRGB(x,y,0xff000000 | ((0 & 0xff) << 16) | ((0 & 0xff) << 8) | (0 & 0xff));
                    matrix[y][x] = 1;
                }else {
                    matrix[y][x] = 0;
                }
            }
        }

        clustering(matrix, image, height, width, objectName);

        displayImage(image);
    }
    public void process(String[] args, int width, int height){
        int numberOfObjects = args.length;

        BufferedImage imgImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] imgHueHistogram = new int[360];
        int[] imgSaturationHistogram = new int[101];
        readRGBImage(width, height, args[0], imgImage);
        createHistogram(imgImage, width, height, imgHueHistogram, imgSaturationHistogram);
        //printHistValues(imgSaturationHistogram);

        for (int i = 1; i < numberOfObjects; i++){
            BufferedImage imgObject = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            String[] parts = args[i].split("/");
            String objectName = parts[parts.length - 1];
            //System.out.println(objectName);
            int[] objHueHistogram = new int[360];
            int[] objSaturationHistogram = new int[101];
            readRGBImage(width, height, args[i], imgObject);
            createHistogram(imgObject, width, height, objHueHistogram, objSaturationHistogram);
            eliminateBackgroundValues(objHueHistogram);
            //printHistValues(objHueHistogram);
            List validHueBinList = findValidHueBins(objHueHistogram);
            if (checkIfObjectIsPresentinImage(imgHueHistogram, validHueBinList)){
                System.out.println("True");
                detectObject(imgImage,width,height,validHueBinList, objectName);
            }
        }
    }


    public static void main(String[] args) {
        HistogramExample img = new HistogramExample();
        img.process(args,640,480);
    }
}