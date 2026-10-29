import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * SilverstoneCircuit - real Silverstone geometry (135 points, 5.89km)
 * Fixed visual artifacts: single Path2D stroked road, smoothed centerline,
 * proper joins, reduced lane spacing, continuous dashed dividers.
 */
public class SilverstoneCircuit implements Circuit {

    private static final double[][] RAW_LON_LAT = {
        {-1.015349, 52.07879}, {-1.01262, 52.078936}, {-1.012135, 52.078918}, {-1.011762, 52.078842}, {-1.011519, 52.078743}, {-1.011229, 52.078536}, {-1.011046, 52.0783}, {-1.010898, 52.078003}, {-1.01069, 52.077499}, {-1.010477, 52.077004}, {-1.010353, 52.076457}, {-1.0103, 52.076113}, {-1.010229, 52.075279}, {-1.010199, 52.074567}, {-1.010152, 52.074289}, {-1.00998, 52.074029}, {-1.009655, 52.073666}, {-1.009518, 52.073459}, {-1.009524, 52.073303}, {-1.009584, 52.07312}, {-1.010045, 52.07237}, {-1.010075, 52.072068}, {-1.010004, 52.071852}, {-1.009838, 52.071668}, {-1.009382, 52.071277}, {-1.009276, 52.071097}, {-1.009264, 52.070885}, {-1.009341, 52.070692}, {-1.009619, 52.07048}, {-1.009974, 52.070324}, {-1.01046, 52.07015}, {-1.010838, 52.070013}, {-1.011105, 52.069853}, {-1.011412, 52.069537}, {-1.012016, 52.06884}, {-1.015337, 52.065083}, {-1.016426, 52.064041}, {-1.016734, 52.063786}, {-1.016982, 52.06365}, {-1.017272, 52.06356}, {-1.017574, 52.063513}, {-1.017852, 52.063527}, {-1.01816, 52.06357}, {-1.01842, 52.06364}, {-1.018651, 52.063753}, {-1.018847, 52.063933}, {-1.018906, 52.064003}, {-1.019202, 52.064399}, {-1.019533, 52.064753}, {-1.019888, 52.065045}, {-1.020131, 52.065224}, {-1.020403, 52.065408}, {-1.020883, 52.06571}, {-1.021954, 52.066497}, {-1.022061, 52.066539}, {-1.022209, 52.066549}, {-1.022356, 52.066506}, {-1.022806, 52.066256}, {-1.02296, 52.066223}, {-1.023114, 52.066233}, {-1.023244, 52.066266}, {-1.023428, 52.066374}, {-1.023623, 52.066497}, {-1.023866, 52.066695}, {-1.024043, 52.066907}, {-1.024168, 52.067124}, {-1.024274, 52.067369}, {-1.024286, 52.067468}, {-1.024256, 52.067567}, {-1.024168, 52.067661}, {-1.023955, 52.067869}, {-1.020687, 52.070489}, {-1.019953, 52.071069}, {-1.0198, 52.071178}, {-1.019533, 52.071286}, {-1.019214, 52.071333}, {-1.018953, 52.071338}, {-1.018592, 52.071319}, {-1.017586, 52.071225}, {-1.017254, 52.071201}, {-1.016911, 52.071192}, {-1.01652, 52.071225}, {-1.016165, 52.071281}, {-1.015846, 52.07139}, {-1.013762, 52.072408}, {-1.013626, 52.072464}, {-1.01346, 52.072516}, {-1.013289, 52.072526}, {-1.013159, 52.072497}, {-1.01304, 52.072431}, {-1.012969, 52.072356}, {-1.012614, 52.071592}, {-1.012507, 52.071484}, {-1.01236, 52.071418}, {-1.012188, 52.071413}, {-1.011998, 52.071465}, {-1.01188, 52.071559}, {-1.011803, 52.071658}, {-1.011655, 52.071908}, {-1.011537, 52.07212}, {-1.011448, 52.072328}, {-1.011401, 52.07253}, {-1.011371, 52.072743}, {-1.011395, 52.072964}, {-1.011466, 52.073063}, {-1.011649, 52.07319}, {-1.018403, 52.07698}, {-1.018586, 52.077051}, {-1.018841, 52.077131}, {-1.019154, 52.077159}, {-1.01948, 52.077136}, {-1.019811, 52.077037}, {-1.01993, 52.076947}, {-1.020013, 52.076815}, {-1.020149, 52.076127}, {-1.020226, 52.075986}, {-1.020391, 52.075872}, {-1.02064, 52.075797}, {-1.020912, 52.075773}, {-1.021214, 52.075821}, {-1.021427, 52.07591}, {-1.021557, 52.076014}, {-1.021658, 52.076136}, {-1.021676, 52.076268}, {-1.021634, 52.07641}, {-1.021469, 52.076641}, {-1.020599, 52.077701}, {-1.02032, 52.077904}, {-1.020048, 52.078074}, {-1.019657, 52.078281}, {-1.019338, 52.078418}, {-1.018935, 52.078522}, {-1.018586, 52.078592}, {-1.017846, 52.078649}, {-1.015349, 52.07879}
    };

    private final List<Point2D.Double> meterPoints = new ArrayList<>();
    private double minX_M, maxX_M, minY_M, maxY_M, rangeX_M, rangeY_M;
    private double totalLengthM;

    // tuned visual params to avoid artifacts
    private static final double LANE_SPACING = 10.0; // reduced from 14 to avoid crossing in hairpins
    private static final double TOTAL_ROAD_WIDTH = 34.0; // 3 lanes visual width
    private static final double OUTER_BORDER_EXTRA = 4.0;

    public SilverstoneCircuit() {
        double minLon = -1.024286;
        double minLat = 52.063513;
        double maxLat = 52.078936;
        double lat0 = (minLat + maxLat) / 2.0;
        double cosLat = Math.cos(Math.toRadians(lat0));
        double metersPerLon = 111320 * cosLat;
        double metersPerLat = 111320;

        for (double[] lonLat : RAW_LON_LAT) {
            double xM = (lonLat[0] - minLon) * metersPerLon;
            double yM = (lonLat[1] - minLat) * metersPerLat;
            meterPoints.add(new Point2D.Double(xM, yM));
        }
        // remove duplicate closing point if present (first == last)
        if (meterPoints.size() > 1) {
            Point2D.Double first = meterPoints.get(0);
            Point2D.Double last = meterPoints.get(meterPoints.size()-1);
            if (first.distance(last) < 1e-6) meterPoints.remove(meterPoints.size()-1);
        }

        minX_M = Double.MAX_VALUE; maxX_M = -Double.MAX_VALUE;
        minY_M = Double.MAX_VALUE; maxY_M = -Double.MAX_VALUE;
        for (Point2D.Double p : meterPoints) {
            minX_M = Math.min(minX_M, p.x);
            maxX_M = Math.max(maxX_M, p.x);
            minY_M = Math.min(minY_M, p.y);
            maxY_M = Math.max(maxY_M, p.y);
        }
        rangeX_M = maxX_M - minX_M;
        rangeY_M = maxY_M - minY_M;

        double total = 0;
        for (int i = 0; i < meterPoints.size(); i++) {
            Point2D.Double a = meterPoints.get(i);
            Point2D.Double b = meterPoints.get((i+1) % meterPoints.size());
            total += a.distance(b);
        }
        totalLengthM = total;
    }

    @Override public String getName() { return "Silverstone Circuit"; }
    @Override public int getLaneCount() { return 3; }
    @Override public double getTotalLengthMeters() { return totalLengthM; }

    @Override
    /** Centerline screen points with smoothing to reduce kinks */
    public List<Point2D.Double> getCenterScreenPoints(int panelWidth, int panelHeight) {
        if (panelWidth <= 10 || panelHeight <= 10) return new ArrayList<>();
        int margin = 55;
        int availableW = panelWidth - 2 * margin;
        int availableH = panelHeight - 2 * margin - 15; // leave room for header
        if (availableW <= 10 || availableH <= 10) return new ArrayList<>();
        double scale = Math.min(availableW / rangeX_M, availableH / rangeY_M);
        double extraX = availableW - rangeX_M * scale;
        double extraY = availableH - rangeY_M * scale;
        double offsetX = margin + extraX / 2.0;
        double offsetY = margin + 15 + extraY / 2.0; // shift down for header

        List<Point2D.Double> screen = new ArrayList<>(meterPoints.size());
        for (Point2D.Double pm : meterPoints) {
            double sx = offsetX + (pm.x - minX_M) * scale;
            double sy = offsetY + (maxY_M - pm.y) * scale;
            screen.add(new Point2D.Double(sx, sy));
        }
        // apply 2 iterations of smoothing (weighted moving average) to reduce sharp artifacts
        screen = smoothClosed(screen, 2);
        return screen;
    }

    /** Chaikin-like smoothing for closed loop */
    private List<Point2D.Double> smoothClosed(List<Point2D.Double> pts, int iterations) {
        List<Point2D.Double> cur = new ArrayList<>(pts);
        for (int iter = 0; iter < iterations; iter++) {
            List<Point2D.Double> nxt = new ArrayList<>(cur.size());
            int n = cur.size();
            for (int i = 0; i < n; i++) {
                Point2D.Double prev = cur.get((i-1+n)%n);
                Point2D.Double curr = cur.get(i);
                Point2D.Double next = cur.get((i+1)%n);
                // weighted average: 0.25*prev + 0.5*curr + 0.25*next
                double x = 0.25*prev.x + 0.5*curr.x + 0.25*next.x;
                double y = 0.25*prev.y + 0.5*curr.y + 0.25*next.y;
                nxt.add(new Point2D.Double(x,y));
            }
            cur = nxt;
        }
        return cur;
    }

    @Override
    public List<Point2D.Double> getLaneScreenPoints(int lane, int panelWidth, int panelHeight) {
        List<Point2D.Double> center = getCenterScreenPoints(panelWidth, panelHeight);
        if (center.isEmpty()) return center;
        if (lane < 0 || lane > 2) lane = 1;
        double offset = (lane - 1) * LANE_SPACING;
        if (offset == 0) return center;
        return offsetPath(center, offset);
    }

    /** Improved offset using averaged segment normals with miter limit */
    private List<Point2D.Double> offsetPath(List<Point2D.Double> center, double offset) {
        int n = center.size();
        List<Point2D.Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Point2D.Double curr = center.get(i);
            Point2D.Double prev = center.get((i-1+n)%n);
            Point2D.Double next = center.get((i+1)%n);

            // incoming and outgoing direction
            double inX = curr.x - prev.x;
            double inY = curr.y - prev.y;
            double outX = next.x - curr.x;
            double outY = next.y - curr.y;
            double inLen = Math.hypot(inX, inY);
            double outLen = Math.hypot(outX, outY);
            if (inLen < 1e-6 || outLen < 1e-6) {
                // fallback to simple normal
                double dx = next.x - prev.x;
                double dy = next.y - prev.y;
                double len = Math.hypot(dx, dy);
                if (len < 1e-6) { out.add(new Point2D.Double(curr.x, curr.y)); continue; }
                double nx = -dy/len, ny = dx/len;
                out.add(new Point2D.Double(curr.x + nx*offset, curr.y + ny*offset));
                continue;
            }
            double inNx = inX/inLen, inNy = inY/inLen;
            double outNx = outX/outLen, outNy = outY/outLen;
            // tangent as average of directions
            double tx = inNx + outNx;
            double ty = inNy + outNy;
            double tLen = Math.hypot(tx, ty);
            if (tLen < 1e-6) {
                // 180 turn, use perpendicular of incoming
                double nx = -inNy, ny = inNx;
                out.add(new Point2D.Double(curr.x + nx*offset, curr.y + ny*offset));
                continue;
            }
            tx/=tLen; ty/=tLen;
            double nx = -ty, ny = tx;
            // miter scale to keep offset correct at sharp angles
            // cos between tangent and segment normal
            double cos = Math.abs(nx * (-inNy) + ny * inNx); // dot with incoming normal
            // also with outgoing
            double cos2 = Math.abs(nx * (-outNy) + ny * outNx);
            double c = Math.min(cos, cos2);
            if (c < 0.3) c = 0.3; // limit miter to avoid huge spikes in hairpins
            double miter = 1.0 / c;
            if (miter > 1.6) miter = 1.6;
            out.add(new Point2D.Double(curr.x + nx*offset*miter, curr.y + ny*offset*miter));
        }
        return out;
    }

    private List<Point2D.Double> getOffsetFromCenter(List<Point2D.Double> center, double offset) {
        return offsetPath(center, offset);
    }

    @Override
    public double getTotalLengthScreen(int lane, int panelWidth, int panelHeight) {
        List<Point2D.Double> pts = getLaneScreenPoints(lane, panelWidth, panelHeight);
        if (pts.size() < 2) return 0;
        double total = 0;
        for (int i = 0; i < pts.size(); i++) {
            Point2D.Double a = pts.get(i);
            Point2D.Double b = pts.get((i+1) % pts.size());
            total += a.distance(b);
        }
        return total;
    }

    @Override
    public Point2D.Double getPosition(int lane, double progress, int panelWidth, int panelHeight) {
        List<Point2D.Double> pts = getLaneScreenPoints(lane, panelWidth, panelHeight);
        if (pts.isEmpty()) return new Point2D.Double(0,0);
        progress = ((progress % 1.0) + 1.0) % 1.0;
        double total = getTotalLengthScreen(lane, panelWidth, panelHeight);
        if (total < 1e-6) return pts.get(0);
        double targetDist = progress * total;
        int n = pts.size();
        double cum = 0;
        for (int i = 0; i < n; i++) {
            Point2D.Double a = pts.get(i);
            Point2D.Double b = pts.get((i+1)%n);
            double segLen = a.distance(b);
            if (targetDist >= cum && targetDist < cum + segLen) {
                double t = (targetDist - cum) / segLen;
                return new Point2D.Double(a.x + t*(b.x - a.x), a.y + t*(b.y - a.y));
            }
            cum += segLen;
        }
        return pts.get(0);
    }

    @Override
    public double getAngle(int lane, double progress, int panelWidth, int panelHeight) {
        List<Point2D.Double> pts = getLaneScreenPoints(lane, panelWidth, panelHeight);
        if (pts.size() < 2) return 0;
        progress = ((progress % 1.0)+1)%1;
        double total = getTotalLengthScreen(lane, panelWidth, panelHeight);
        double targetDist = progress * total;
        int n = pts.size();
        double cum=0;
        for (int i=0;i<n;i++) {
            Point2D.Double a = pts.get(i);
            Point2D.Double b = pts.get((i+1)%n);
            double segLen = a.distance(b);
            if (targetDist >= cum && targetDist < cum+segLen) return Math.atan2(b.y - a.y, b.x - a.x);
            cum+=segLen;
        }
        return 0;
    }

    private Path2D.Double createPath(List<Point2D.Double> pts, boolean close) {
        Path2D.Double p = new Path2D.Double();
        if (pts.isEmpty()) return p;
        p.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i=1;i<pts.size();i++) p.lineTo(pts.get(i).x, pts.get(i).y);
        if (close) p.closePath();
        return p;
    }

    @Override
    public void draw(Graphics2D g2, int panelWidth, int panelHeight) {
        g2.setColor(new Color(34, 139, 34));
        g2.fillRect(0,0,panelWidth,panelHeight);
        List<Point2D.Double> center = getCenterScreenPoints(panelWidth, panelHeight);
        if (center.isEmpty()) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Path2D.Double centerPath = createPath(center, true);

        // Outer white border + asphalt as single wide strokes (no per-lane artifacts)
        g2.setStroke(new BasicStroke((float)(TOTAL_ROAD_WIDTH + OUTER_BORDER_EXTRA), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Color.WHITE);
        g2.draw(centerPath);

        g2.setStroke(new BasicStroke((float)TOTAL_ROAD_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(45,45,45));
        g2.draw(centerPath);

        // Lane dividers: two dashed lines offset from center (inside asphalt)
        // With spacing 10, dividers at +-5
        List<Point2D.Double> divA = offsetPath(center, -5);
        List<Point2D.Double> divB = offsetPath(center, 5);
        Path2D.Double pathA = createPath(divA, true);
        Path2D.Double pathB = createPath(divB, true);

        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{10,8}, 0));
        g2.setColor(Color.WHITE);
        g2.draw(pathA);
        g2.draw(pathB);

        // Center dashed line for 3-lane effect? optionally add middle? we already have 2 dividers for 3 lanes
        // Outer inner edge already handled by border, no need for extra outer draws

        // Start/finish line across full road width
        Point2D.Double start = getPosition(1, 0.0, panelWidth, panelHeight);
        double angle = getAngle(1, 0.0, panelWidth, panelHeight);
        double perp = angle + Math.PI/2;
        double halfW = TOTAL_ROAD_WIDTH/2 + 1;
        int x1 = (int)(start.x + Math.cos(perp)*halfW);
        int y1 = (int)(start.y + Math.sin(perp)*halfW);
        int x2 = (int)(start.x - Math.cos(perp)*halfW);
        int y2 = (int)(start.y - Math.sin(perp)*halfW);
        // draw thick base for start line (white)
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        g2.setColor(Color.WHITE);
        g2.drawLine(x1,y1,x2,y2);
        // checkered on top: 8 alternating segments
        int segments = 8;
        for (int i=0;i<segments;i++) {
            if (i%2==0) continue; // keep white base, draw black over
            double t1 = (double)i/segments;
            double t2 = (double)(i+1)/segments;
            int sx1 = (int)(x1 + t1*(x2-x1));
            int sy1 = (int)(y1 + t1*(y2-y1));
            int sx2 = (int)(x1 + t2*(x2-x1));
            int sy2 = (int)(y1 + t2*(y2-y1));
            g2.setColor(Color.BLACK);
            g2.drawLine(sx1, sy1, sx2, sy2);
        }
        // small yellow direction arrow just after line
        double arrowLen = 12;
        int ax1 = (int)(start.x + Math.cos(angle)*arrowLen);
        int ay1 = (int)(start.y + Math.sin(angle)*arrowLen);
        g2.setColor(new Color(255, 215, 0));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int)start.x, (int)start.y, ax1, ay1);
        // arrow head
        double head = 5;
        int hx1 = (int)(ax1 - Math.cos(angle - Math.PI/6)*head);
        int hy1 = (int)(ay1 - Math.sin(angle - Math.PI/6)*head);
        int hx2 = (int)(ax1 - Math.cos(angle + Math.PI/6)*head);
        int hy2 = (int)(ay1 - Math.sin(angle + Math.PI/6)*head);
        g2.drawLine(ax1, ay1, hx1, hy1);
        g2.drawLine(ax1, ay1, hx2, hy2);
    }

    @Override
    public String getInfo(int totalCars) {
        return String.format("Silverstone • %.2f km • 3 Lanes • %d cars", totalLengthM/1000.0, totalCars);
    }
}
