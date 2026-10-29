import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * ManhattanCircuit - a city-block loop in Midtown Manhattan (approx 40.75°N, -73.99°W)
 * Outer loop is a rectangular block loop (5th Ave - 7th Ave, 34th St - 59th St) with Broadway diagonal.
 * Background draws Manhattan street grid for context, foreground is the drivable 2-lane circuit.
 */
public class ManhattanCircuit implements Circuit {

    private final List<Point2D.Double> meterPoints = new ArrayList<>();
    private double minX_M, maxX_M, minY_M, maxY_M, rangeX_M, rangeY_M;
    private double totalLengthM;

    // Visual params - 2 lanes (inner/outer) spacing 12
    private static final double LANE_SPACING = 12.0;
    private static final double TOTAL_ROAD_WIDTH = 26.0;
    private static final double OUTER_BORDER_EXTRA = 4.0;

    // City grid definition in meter space (same coordinate system as outer loop)
    // Outer loop bounds: x 0..900, y 0..1100 (approx 900m east-west, 1100m north-south)
    // Avenues: vertical streets
    private static final double[] AVENUES_X = {0, 180, 360, 540, 720, 900};
    private static final String[] AVENUE_NAMES = {"7th Ave", "6th Ave", "5th Ave", "Madison", "Park", "Lex"};
    // Streets: horizontal
    private static final double[] STREETS_Y = {0, 180, 360, 540, 720, 900, 1100};
    private static final String[] STREET_NAMES = {"34th St", "42nd St", "47th St", "50th St", "55th St", "59th St", "60th"};

    public ManhattanCircuit() {
        // Define outer loop meter points - a rectangular loop with Broadway diagonal cut
        // Clockwise starting at 34th & 7th (southwest corner)
        double[][] pts = {
            {40, 40},       // 34th & 7th - start/finish
            {860, 40},      // 34th & Lex east
            {860, 120},
            {780, 120},     // cut in for building
            {780, 980},
            {860, 980},
            {860, 1060},    // 59th north east
            {40, 1060},     // 59th & 7th west
            {40, 980},
            {120, 980},
            {120, 560},     // mid Broadway diagonal jog
            {280, 400},     // Broadway diagonal
            {120, 240},
            {120, 120},
            {40, 120},
            {40, 40}
        };
        for (double[] p : pts) meterPoints.add(new Point2D.Double(p[0], p[1]));

        // compute bounds
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
        // total length
        double total = 0;
        for (int i=0;i<meterPoints.size();i++) {
            Point2D.Double a = meterPoints.get(i);
            Point2D.Double b = meterPoints.get((i+1)%meterPoints.size());
            total += a.distance(b);
        }
        totalLengthM = total; // ~ 4200m, scale to realistic 4.2km city loop
        // adjust to realistic city block length (~ 4.5km)
        // we keep meterPoints as is, totalLengthM used for info
    }

    @Override public String getName() { return "Manhattan - Midtown Loop"; }
    @Override public int getLaneCount() { return 2; }
    @Override public double getTotalLengthMeters() { return totalLengthM * 1.5; } // scaled to ~6.3km for display

    @Override
    public List<Point2D.Double> getCenterScreenPoints(int panelWidth, int panelHeight) {
        if (panelWidth <= 10 || panelHeight <= 10) return new ArrayList<>();
        int margin = 60;
        int availableW = panelWidth - 2*margin;
        int availableH = panelHeight - 2*margin - 15;
        if (availableW <=10 || availableH <=10) return new ArrayList<>();
        double scale = Math.min(availableW / rangeX_M, availableH / rangeY_M);
        double extraX = availableW - rangeX_M*scale;
        double extraY = availableH - rangeY_M*scale;
        double offsetX = margin + extraX/2;
        double offsetY = margin + 15 + extraY/2;

        List<Point2D.Double> screen = new ArrayList<>(meterPoints.size());
        for (Point2D.Double pm : meterPoints) {
            double sx = offsetX + (pm.x - minX_M)*scale;
            double sy = offsetY + (maxY_M - pm.y)*scale;
            screen.add(new Point2D.Double(sx,sy));
        }
        // light smoothing for Manhattan right angles: 1 iteration only to keep 90deg character
        screen = smoothClosed(screen, 1);
        return screen;
    }

    private List<Point2D.Double> smoothClosed(List<Point2D.Double> pts, int iterations) {
        List<Point2D.Double> cur = new ArrayList<>(pts);
        for (int iter=0; iter<iterations; iter++) {
            List<Point2D.Double> nxt = new ArrayList<>(cur.size());
            int n = cur.size();
            for (int i=0;i<n;i++) {
                Point2D.Double prev = cur.get((i-1+n)%n);
                Point2D.Double curr = cur.get(i);
                Point2D.Double next = cur.get((i+1)%n);
                double x = 0.2*prev.x + 0.6*curr.x + 0.2*next.x;
                double y = 0.2*prev.y + 0.6*curr.y + 0.2*next.y;
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
        if (lane <0 || lane>1) lane = 0;
        // Inverted vs Silverstone due to opposite winding (Manhattan CCW vs Silverstone CW) - lane 0 = voie de droite
        // Silverstone CW interior = right, Manhattan CCW exterior = right. To have same visual side as Silverstone's right (interior toward center), swap sign.
        // Keeping same rotation (CCW) as requested, but fixing visual inversion.
        double offset = (lane == 0 ? LANE_SPACING/2 : -LANE_SPACING/2); // +6, -6 - swapped to match Silverstone right side (interior toward center)
        if (offset==0) return center;
        return offsetPath(center, offset);
    }

    private List<Point2D.Double> offsetPath(List<Point2D.Double> center, double offset) {
        int n = center.size();
        List<Point2D.Double> out = new ArrayList<>(n);
        for (int i=0;i<n;i++) {
            Point2D.Double curr = center.get(i);
            Point2D.Double prev = center.get((i-1+n)%n);
            Point2D.Double next = center.get((i+1)%n);
            double inX = curr.x - prev.x, inY = curr.y - prev.y;
            double outX = next.x - curr.x, outY = next.y - curr.y;
            double inLen = Math.hypot(inX,inY), outLen = Math.hypot(outX,outY);
            if (inLen <1e-6 || outLen<1e-6) {
                double dx = next.x - prev.x, dy = next.y - prev.y;
                double len = Math.hypot(dx,dy);
                if (len<1e-6) { out.add(new Point2D.Double(curr.x,curr.y)); continue; }
                double nx = -dy/len, ny = dx/len;
                out.add(new Point2D.Double(curr.x+nx*offset, curr.y+ny*offset));
                continue;
            }
            double inNx=inX/inLen, inNy=inY/inLen;
            double outNx=outX/outLen, outNy=outY/outLen;
            double tx = inNx+outNx, ty = inNy+outNy;
            double tLen = Math.hypot(tx,ty);
            if (tLen<1e-6) {
                double nx=-inNy, ny=inNx;
                out.add(new Point2D.Double(curr.x+nx*offset, curr.y+ny*offset));
                continue;
            }
            tx/=tLen; ty/=tLen;
            double nx=-ty, ny=tx;
            double cos = Math.abs(nx*(-inNy)+ny*inNx);
            double cos2 = Math.abs(nx*(-outNy)+ny*outNx);
            double c = Math.min(cos,cos2);
            if (c<0.35) c=0.35;
            double miter = 1.0/c;
            if (miter>1.5) miter=1.5;
            out.add(new Point2D.Double(curr.x+nx*offset*miter, curr.y+ny*offset*miter));
        }
        return out;
    }

    @Override
    public double getTotalLengthScreen(int lane, int w, int h) {
        List<Point2D.Double> pts = getLaneScreenPoints(lane,w,h);
        if (pts.size()<2) return 0;
        double total=0;
        for(int i=0;i<pts.size();i++) {
            Point2D.Double a=pts.get(i), b=pts.get((i+1)%pts.size());
            total+=a.distance(b);
        }
        return total;
    }

    @Override
    public Point2D.Double getPosition(int lane, double progress, int w,int h) {
        List<Point2D.Double> pts = getLaneScreenPoints(lane,w,h);
        if (pts.isEmpty()) return new Point2D.Double(0,0);
        progress = ((progress%1.0)+1)%1.0;
        double total=getTotalLengthScreen(lane,w,h);
        if (total<1e-6) return pts.get(0);
        double target=progress*total;
        int n=pts.size();
        double cum=0;
        for(int i=0;i<n;i++) {
            Point2D.Double a=pts.get(i), b=pts.get((i+1)%n);
            double seg=a.distance(b);
            if(target>=cum && target<cum+seg) {
                double t=(target-cum)/seg;
                return new Point2D.Double(a.x+t*(b.x-a.x), a.y+t*(b.y-a.y));
            }
            cum+=seg;
        }
        return pts.get(0);
    }

    @Override
    public double getAngle(int lane,double progress,int w,int h) {
        List<Point2D.Double> pts=getLaneScreenPoints(lane,w,h);
        if(pts.size()<2) return 0;
        progress=((progress%1)+1)%1;
        double total=getTotalLengthScreen(lane,w,h);
        double target=progress*total;
        int n=pts.size(); double cum=0;
        for(int i=0;i<n;i++) {
            Point2D.Double a=pts.get(i), b=pts.get((i+1)%n);
            double seg=a.distance(b);
            if(target>=cum && target<cum+seg) return Math.atan2(b.y-a.y,b.x-a.x);
            cum+=seg;
        }
        return 0;
    }

    private Path2D.Double createPath(List<Point2D.Double> pts, boolean close) {
        Path2D.Double p=new Path2D.Double();
        if(pts.isEmpty()) return p;
        p.moveTo(pts.get(0).x,pts.get(0).y);
        for(int i=1;i<pts.size();i++) p.lineTo(pts.get(i).x,pts.get(i).y);
        if(close) p.closePath();
        return p;
    }

    /** Transform meter point to screen using same scale as center */
    private Point2D.Double meterToScreen(double xM, double yM, int pw,int ph) {
        int margin=60;
        int availableW=pw-2*margin;
        int availableH=ph-2*margin-15;
        double scale=Math.min(availableW/rangeX_M, availableH/rangeY_M);
        double extraX=availableW - rangeX_M*scale;
        double extraY=availableH - rangeY_M*scale;
        double offsetX=margin+extraX/2;
        double offsetY=margin+15+extraY/2;
        double sx=offsetX + (xM - minX_M)*scale;
        double sy=offsetY + (maxY_M - yM)*scale;
        return new Point2D.Double(sx,sy);
    }

    @Override
    public void draw(Graphics2D g2,int pw,int ph) {
        // City background - light concrete
        g2.setColor(new Color(220,220,215));
        g2.fillRect(0,0,pw,ph);

        List<Point2D.Double> center=getCenterScreenPoints(pw,ph);
        if(center.isEmpty()) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw Manhattan street grid in meter space transformed to screen
        // Vertical avenues
        g2.setColor(new Color(180,180,175));
        g2.setStroke(new BasicStroke(1f));
        for (double ax : AVENUES_X) {
            Point2D.Double top = meterToScreen(ax, 0, pw, ph);
            Point2D.Double bottom = meterToScreen(ax, 1100, pw, ph);
            g2.drawLine((int)top.x, (int)top.y, (int)bottom.x, (int)bottom.y);
        }
        // Horizontal streets
        for (double sy : STREETS_Y) {
            Point2D.Double left = meterToScreen(0, sy, pw, ph);
            Point2D.Double right = meterToScreen(900, sy, pw, ph);
            g2.drawLine((int)left.x, (int)left.y, (int)right.x, (int)right.y);
        }
        // Broadway diagonal (Times Square diagonal)
        g2.setColor(new Color(200,190,150));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,0,new float[]{6,6},0));
        Point2D.Double b1=meterToScreen(120,240,pw,ph);
        Point2D.Double b2=meterToScreen(780,980,pw,ph);
        g2.drawLine((int)b1.x,(int)b1.y,(int)b2.x,(int)b2.y);

        // Draw building blocks as slightly darker rectangles between streets (simple)
        g2.setColor(new Color(205,205,195));
        // We'll draw a few sample blocks: between avenues/streets, fill small rects
        // Not too many to keep performance
        for(int i=0;i<AVENUES_X.length-1;i++){
            for(int j=0;j<STREETS_Y.length-1;j++){
                // skip where circuit passes to keep road visible (approx check)
                if(i==0 && j==0) continue; // start area
                double x0 = AVENUES_X[i]+8, y0 = STREETS_Y[j]+8;
                double x1 = AVENUES_X[i+1]-8, y1 = STREETS_Y[j+1]-8;
                if(x1<=x0 || y1<=y0) continue;
                Point2D.Double p0=meterToScreen(x0,y0,pw,ph);
                Point2D.Double p1=meterToScreen(x1,y1,pw,ph);
                int rx=(int)Math.min(p0.x,p1.x), ry=(int)Math.min(p0.y,p1.y);
                int rw=(int)Math.abs(p1.x-p0.x), rh=(int)Math.abs(p1.y-p0.y);
                if(rw>4 && rh>4) {
                    g2.fillRect(rx,ry,rw,rh);
                    g2.setColor(new Color(190,190,180));
                    g2.drawRect(rx,ry,rw,rh);
                    g2.setColor(new Color(205,205,195));
                }
            }
        }
        // Highlight Central Park area (top, east side) as green
        Point2D.Double cp1=meterToScreen(400,850,pw,ph);
        Point2D.Double cp2=meterToScreen(850,1050,pw,ph);
        g2.setColor(new Color(100,160,90));
        g2.fillRect((int)cp1.x, (int)cp1.y, (int)(cp2.x-cp1.x), (int)(cp2.y-cp1.y));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
        g2.drawString("Central Park", (int)cp1.x+8, (int)cp1.y+14);

        // Draw circuit road (2 lanes) as wide street
        Path2D.Double centerPath=createPath(center,true);
        g2.setStroke(new BasicStroke((float)(TOTAL_ROAD_WIDTH + OUTER_BORDER_EXTRA), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(30,30,30)); // curb dark
        g2.draw(centerPath);
        g2.setStroke(new BasicStroke((float)TOTAL_ROAD_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(50,50,50)); // asphalt
        g2.draw(centerPath);
        // Center dashed line for 2 lanes (single divider)
        List<Point2D.Double> divider = offsetPath(center, 0); // actually center is divider for 2 lanes
        // For 2 lanes, divider is center line (between -6 and +6)
        Path2D.Double divPath=createPath(center,true); // center itself is divider
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,0,new float[]{12,8},0));
        g2.setColor(Color.WHITE);
        g2.draw(divPath);
        // Outer curbs already via wide stroke, add thin white edge inside road? already via border

        // Start/finish at 34th & 7th (first point)
        Point2D.Double start=getPosition(0,0.0,pw,ph);
        double angle=getAngle(0,0.0,pw,ph);
        double perp=angle+Math.PI/2;
        double halfW=TOTAL_ROAD_WIDTH/2+1;
        int x1=(int)(start.x + Math.cos(perp)*halfW);
        int y1=(int)(start.y + Math.sin(perp)*halfW);
        int x2=(int)(start.x - Math.cos(perp)*halfW);
        int y2=(int)(start.y - Math.sin(perp)*halfW);
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(Color.WHITE);
        g2.drawLine(x1,y1,x2,y2);
        int segments=6;
        for(int i=0;i<segments;i++) if(i%2==1){
            double t1=(double)i/segments, t2=(double)(i+1)/segments;
            int sx1=(int)(x1+t1*(x2-x1)), sy1=(int)(y1+t1*(y2-y1));
            int sx2=(int)(x1+t2*(x2-x1)), sy2=(int)(y1+t2*(y2-y1));
            g2.setColor(Color.BLACK);
            g2.drawLine(sx1,sy1,sx2,sy2);
        }
        double arrowLen=14;
        int ax1=(int)(start.x + Math.cos(angle)*arrowLen);
        int ay1=(int)(start.y + Math.sin(angle)*arrowLen);
        g2.setColor(new Color(255,215,0));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int)start.x,(int)start.y,ax1,ay1);
        double head=6;
        int hx1=(int)(ax1 - Math.cos(angle-Math.PI/6)*head);
        int hy1=(int)(ay1 - Math.sin(angle-Math.PI/6)*head);
        int hx2=(int)(ax1 - Math.cos(angle+Math.PI/6)*head);
        int hy2=(int)(ay1 - Math.sin(angle+Math.PI/6)*head);
        g2.drawLine(ax1,ay1,hx1,hy1); g2.drawLine(ax1,ay1,hx2,hy2);

        // Street labels
        g2.setColor(new Color(80,80,80));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        // Draw a few labels near grid
        for(int i=0;i<AVENUES_X.length;i++){
            Point2D.Double p=meterToScreen(AVENUES_X[i], 1080, pw,ph);
            g2.drawString(AVENUE_NAMES[i], (int)p.x+2, (int)p.y-4);
        }
        for(int j=0;j<STREETS_Y.length;j++){
            Point2D.Double p=meterToScreen(880, STREETS_Y[j], pw,ph);
            g2.drawString(STREET_NAMES[j], (int)p.x+4, (int)p.y-2);
        }
    }

    @Override
    public String getInfo(int totalCars) {
        return String.format("Manhattan Midtown • %.1f km • 2 voies • %d voitures", getTotalLengthMeters()/1000.0, totalCars);
    }
}
