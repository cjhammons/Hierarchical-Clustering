package com.cjhammons;

import de.erichseifert.gral.data.DataSeries;
import de.erichseifert.gral.data.DataTable;
import de.erichseifert.gral.plots.XYPlot;
import de.erichseifert.gral.plots.points.DefaultPointRenderer2D;
import de.erichseifert.gral.plots.points.PointRenderer;
import de.erichseifert.gral.ui.InteractivePanel;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the implementation of the Hierarchical algorithm
 * as well as all supporting methods and classes.
 *
 * Author: Curtis Hammons
 */
public class Hierarchical extends JFrame{

    //Goal number of clusterList
    private static final int NUM_CLUSTERS_FINAL = 2;

    private List<Point> allPoints = new ArrayList<>();
    List<Cluster> clusterList = new ArrayList<>();
    int numCluster = 0;
    int numPoint = 0;

    /**
     * Simple class that holds (x, y) coordinates
     */
    public class Point {
        double x, y;
        int clustId;
        Cluster pCluster;
        int id;
        /**
         * Constructor
         * @param _x x coordinate of the point
         * @param _y y coordinate of the point
         */
        public Point(double _x, double _y) {
            x = _x;
            y = _y;
            id = numPoint;
            numPoint++;
        }

        public int getClustId() {
            return clustId;
        }

        public void setClustId(int clustId) {
            this.clustId = clustId;
        }

        public Cluster getpCluster() {
            return pCluster;
        }

        public void setpCluster(Cluster pCluster) {
            this.pCluster = pCluster;
            this.clustId = pCluster.getClusterId();
        }

        public String toString() {
            return "(" + x + ", " + y + ")";
        }
    }

    /**
     * Class to represent a cluster of points.
     */
    public class Cluster {
        List<Point> pointList;
        Point centroid;
        Point prevCentroid;
        int clusterId;

        /**
         * Constructor
         * @param _pointList List of points in the cluster
         */
        public Cluster(List<Point> _pointList) {
            pointList = _pointList;
            clusterId = numCluster;
            calculateCentroid();
            numCluster++;
        }

        /**
         * Calculates the cluster's centroid.
         * Sets the local centroid variable to the result.
         */
        public void calculateCentroid() {
            prevCentroid = centroid;
            double cx = 0;
            double cy = 0;
            for (int i = 0; i < pointList.size(); i++) {
                Point p = pointList.get(i);
                cx += p.x;
                cy += p.y;
            }
            centroid = new Point(cx / pointList.size(), cy / pointList.size());
            System.out.println("Cluster " + this.clusterId + "'s centroid is now: " + centroid.toString());
        }

        /**
         * Calculates the distance shifted between previous and current centroids.
         * All it does is call getDistance(), in hindsight it was probably unnecessary.
         */
        public double calculateShift() {
            return getDistance(centroid, prevCentroid);
        }

        public int getClusterId() {
            return clusterId;
        }

        /**
         * Absorbs another cluster, updates this in arraylist and
         * removes absorbed cluster
         * @param cluster
         */
        public void absorb(Cluster cluster) {
            //make sure they aren't the same cluster
            if (cluster.getClusterId() == this.clusterId || !clusterList.contains(cluster)) {
                return;
            }
            clusterList.remove(cluster);
            int index = clusterList.indexOf(this);
            clusterList.remove(index);
            for (Point p : cluster.pointList) {
                this.addPoint(p);
            }
            clusterList.add(index, this);
        }

        /**
         * Adds a point to the cluster, calculates new centroid
         * @param p
         */
        public void addPoint(Point p){
            if (!pointList.contains(p)) {
                int index = allPoints.indexOf(p);
                allPoints.remove(p);
                p.setpCluster(this);
                p.setClustId(this.clusterId);
                this.pointList.add(p);
                allPoints.add(index, p);
                calculateCentroid();
            }
        }

        public void removePoint(Point p){
            if (pointList.contains(p)) {
                int index = allPoints.indexOf(p);
                pointList.remove(p);
                allPoints.remove(p);
                p.clustId = -1;
                p.setpCluster(null);
                allPoints.add(index, p);
                calculateCentroid();
            }
        }

        public Point getCentroid() {
            return centroid;
        }

        public void setCentroid(Point centroid) {
            this.centroid = centroid;
        }
    }

    /**
     * Implementation of the hierarchical algorithm
     */
    public void hierarchical() {
        readFile();

        //Add each point to it's own cluster
        for (int i = 0; i < allPoints.size(); i++) {
            //create cluster with empty point list
            Point point = allPoints.get(i);
            Cluster c = new Cluster(new ArrayList<>());
//            point.setClustId(c.getClusterId());
//            point.setpCluster(c);
            c.addPoint(point);
            clusterList.add(c);
        }

        //Repeat forever until number of clusterList is reduced to 2
        int loopCount = 0;
        while (clusterList.size() > NUM_CLUSTERS_FINAL) {
            System.out.println("Loop: " + loopCount++ + " start");

            //Find closest neighboring cluster to each cluster
            for (int cNum = 0; cNum < clusterList.size(); cNum++) {
                Cluster cluster = clusterList.get(cNum);

                //Initial closest point
                Point closestPoint = allPoints.get(0);
                //Get distance to first point, which is now our shortest known distance
                double shortestDist = 0;
                for (Point clustPoint : cluster.pointList) {

                    //Get distance to first point, which is now our shortest known distance
                    if (shortestDist == 0) {
                        shortestDist = getDistance(clustPoint, closestPoint);
                    }

                    //Make sure points are not identical and do not share the same cluster
                    int listOffset = 0;
                    while (!canPointsMerge(clustPoint, closestPoint)) {
                        listOffset++;
                        closestPoint = allPoints.get(listOffset);
                        shortestDist = getDistance(clustPoint, allPoints.get(listOffset));
                    }
                    //WE NEED MORE NESTED LOOPS
                    //find distance for all other points, skip first point (since we already have that distance)
                    for (int i = 1 + listOffset; i < allPoints.size(); i++) {
                        Point point = allPoints.get(i);
                        double dist = getDistance(clustPoint, point);

                        //Check to see if we found a new shortest distance
                        if (dist < shortestDist && canPointsMerge(clustPoint, point)) {
                            shortestDist = dist;
                            closestPoint = point;
                        }
                    }
                }
                Cluster closestCluster = closestPoint.getpCluster();
                //Absorb the closest cluster
                if (clusterList.size() > NUM_CLUSTERS_FINAL) {
                    cluster.absorb(closestCluster);
                }

                System.out.println("Cluster " + closestCluster.getClusterId() + " absorbed into Cluster " + cluster.getClusterId() + "\n"
                                    + "Number of clusters: " + clusterList.size());
            }
        }
        System.out.println("Convergence complete!");
        plot();
        return;
    }

    /**
     * Checks if two points can be merged by checking to see if they are identical
     * or if they are already in the same cluster.
     * @param a
     * @param b
     * @return true if can absorb, else false
     */
    boolean canPointsMerge(Point a, Point b){
        if (a.id == b.id) {
            return false;
        } else if (a.getClustId() == b.getClustId()) {
            return false;
        } else if (a.getpCluster().equals(b.getpCluster())) {
            return false;
        } else if (a.equals(b)) {
            return false;
        }
        return true;
    }

    /**
     * Reads points form the provided file and adds them to the master point list.
     */
    void readFile(){
        String fileName = "B.txt";
        try {
            FileReader fileReader = new FileReader(fileName);
            BufferedReader reader = new BufferedReader(fileReader);
            String line = null;
            //Create points from the data in the file
            while ((line = reader.readLine()) != null) {
                //Split at the space
                String[] lineSplit = line.split("\\s+");
                Point p = new Point(Double.parseDouble(lineSplit[0]), Double.parseDouble(lineSplit[1]));
                allPoints.add(p);
                System.out.println("point " + p.toString() + " added");
            }

        } catch (FileNotFoundException e) {
            System.out.println(fileName + " does not exist");
        } catch (IOException e) {
            System.out.println("Error reading " + fileName);
        }
    }

    /**
     * Calculates Euclidian distance between two points
     * @param a point a
     * @param b point b
     * @return distance
     */
    double getDistance(Point a, Point b) {
        //Distance formula in java form
        return Math.sqrt(Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2));
    }

    /**
     * Creates a scatter plot of all the points with color coded clusterList.
     *
     * Requires GRAL java graphing library: http://trac.erichseifert.de/gral/
     */
    void plot() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 400);
        List<DataTable> dataTables = new ArrayList<>();
        List<DataSeries> dataSeries = new ArrayList<>();
        //init plot with blank datatable
        XYPlot plot = new XYPlot(new DataTable(double.class, double.class));
        for (int i = 0; i < clusterList.size(); i++) {
            Point centroid = clusterList.get(i).getCentroid();
            DataTable data = new DataTable(Double.class, Double.class);
            for (Point point : clusterList.get(i).pointList) {
                data.add(point.x, point.y);
            }
            DataSeries series = new DataSeries("Series" + i, data, 0, 1);
            dataTables.add(data);
            dataSeries.add(series);
            plot.add(series);
        }

        //Color the clusterList the appropiate color.
        //...I am not proud of this code. May God forgive me.
        for (int i = 0; i < NUM_CLUSTERS_FINAL; i++) {
            PointRenderer renderer = new DefaultPointRenderer2D();
            Color color;
            switch (i) {
                case 1:
                    color = new Color(1.0f, 0.0f, 0.0f);
                    break;
                case 2:
                default:
                    color = new Color(0.0f, 0.5f, 1.0f);
                    break;
            }
            renderer.setColor(color);
            plot.setPointRenderers(dataSeries.get(i), renderer);
        }

        PointRenderer centroidRender = new DefaultPointRenderer2D();
        centroidRender.setColor(new Color(0.0f, 0.0f, 0.0f));


        getContentPane().add(new InteractivePanel(plot));
        setVisible(true);

    }
}
