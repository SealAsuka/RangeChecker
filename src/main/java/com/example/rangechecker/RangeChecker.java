package com.example.rangechecker;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.lang3.math.NumberUtils;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class RangeChecker {
    public static void main(String[] args) throws IOException {
        System.out.println("请输入点的x坐标:");
        Scanner sc = new Scanner(System.in);
        Double abscissa = Double.valueOf(sc.nextLine());
        System.out.println("请输入点的y坐标:");
        Scanner sc1 = new Scanner(System.in);
        Double ordinate = Double.valueOf(sc1.nextLine());
        //P2D下要测的点的坐标
        Point2D.Double point = new Point2D.Double(abscissa, ordinate);
        //读取Json
        String fileName = "src/main/resources/latlon.json";
        String jsonString = new String(Files.readAllBytes(new File(fileName).toPath()));
        JSONArray points = JSON.parseArray(jsonString);
        //解析完了转P2D
        List<Point2D.Double> pts = convertJsonArrayToPointList(points);


        //调用
        if (isItInArea(points, abscissa, ordinate)) {
            if (IsPtInPoly(point, pts)) {
                System.out.println("该点在多边形内");
            } else {
                System.out.println("该点在多边形外");
            }
        }

    }

    private static List<Point2D.Double> convertJsonArrayToPointList(JSONArray points) {
        List<Point2D.Double> pointList = new ArrayList<>();

        for (int i = 0; i < points.size() - 1; i += 2) {
            double x = NumberUtils.toDouble(points.getString(i));
            double y = NumberUtils.toDouble(points.getString(i + 1));
            Point2D.Double point = new Point2D.Double(x, y);
            pointList.add(point);
        }
        return pointList;
    }

    /**
     * 先将横纵坐标数组的横坐标最大最小值，纵坐标的最大最小值，求出来，需要判断的一点大于横纵坐标的最大值或者小于横纵坐标的最小值，
     * 也就是粗略的计算一下，如果这个条件不满足的话，就不用往下计算了，直接不在指定的区域里面。
     */
    public static boolean isItInArea(JSONArray points, double abscissa, double ordinate) {
        //初始化横纵坐标的最大值和最小值
        double maxX = Double.MAX_VALUE;
        double minX = Double.MIN_VALUE;
        double maxY = Double.MAX_VALUE;
        double minY = Double.MIN_VALUE;
        //遍历多边形的每个点，更新最大值和最小值
        for (int i = 0; i < points.size(); i += 2) {
            double x = Double.parseDouble(((JSONArray) points.get(i)).get(0).toString());
            double y = Double.parseDouble(((JSONArray) points.get(i)).get(1).toString());
            if (x < minX) {
                minX = x;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (abscissa < minX || abscissa > maxX || ordinate < minY || ordinate > maxY) {
                return false;
            }
        }

        return true;
    }

    /**
     * 使用射线法:通过一点，画一条线，这条线与多边形相交，如果相交点数位奇数，就在区域内，如果为偶数，就不在区域内。这也是一种比较简单实用的方法
     * 判断点是否在多边形内
     *
     * @param point 检测点
     * @param pts   多边形的顶点
     * @return 点在多边形内返回true, 否则返回false
     */
    public static boolean IsPtInPoly(Point2D.Double point, List<Point2D.Double> pts) {

        int N = pts.size();
        boolean boundOrVertex = true; //如果点位于多边形的顶点或边上，也算做点在多边形内，直接返回true
        int intersectCount = 0;// x轴交点个数
        double precision = 2e-10; //浮点类型计算时候与0比较时候的容差
        Point2D.Double p1, p2;//相邻边界顶点
        Point2D.Double p = point; //当前点

        p1 = pts.get(0);//左顶点
        for (int i = 1; i <= N; ++i) {//检查所有射线
            if (p.equals(p1)) {
                return boundOrVertex;// p是一个顶点
            }

            p2 = pts.get(i % N);//右顶点
            if (p.x < Math.min(p1.x, p2.x) || p.x > Math.max(p1.x, p2.x)) {//射线在区域外
                p1 = p2;
                continue;//下一条射线的左边点
            }

            if (p.x > Math.min(p1.x, p2.x) && p.x < Math.max(p1.x, p2.x)) {
                if (p.y <= Math.max(p1.y, p2.y)) {//x is before of ray
                    if (p1.x == p2.x && p.y >= Math.min(p1.y, p2.y)) {
                        return boundOrVertex;
                    }

                    if (p1.y == p2.y) { //当射线是垂直的时候
                        if (p1.y == p.y) {
                            return boundOrVertex;
                        } else {
                            ++intersectCount;
                        }
                    } else {//交点在左侧时
                        double xinters = (p.x - p1.x) * (p2.y - p1.y) / (p2.x - p1.x) + p1.y; //穿过点的纵坐标时
                        if (Math.abs(p.y - xinters) < precision) {
                            return boundOrVertex;
                        }
                        if (p.y < xinters) {
                            ++intersectCount;
                        }
                    }
                }
            } else { //当射线穿过顶点时
                if (p.x == p2.x && p.y <= p2.y) {//当p点跃过P2时
                    Point2D.Double p3 = pts.get((i + 1) % N); //下一个顶点
                    if (p.x >= Math.min(p1.x, p3.x) && p.x <= Math.max(p1.x, p3.x)) {//p横坐标在p1和p3之间
                        ++intersectCount;
                    } else {
                        intersectCount += 2;
                    }
                }
            }
            p1 = p2;//next ray left point
        }

        if (intersectCount % 2 == 0) {//偶数在多边形外
            return false;
        } else { //奇数在多边形内
            return true;
        }

    }

}
