import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * ManhattanCircuit - 3-lane city loop between Manhattan buildings (approx 40.75°N, -73.99°W)
 * Realistic Manhattan street grid: avenues between buildings, 3-lane highway loop on outer rectangle (7th Ave - Lex, 34th - 59th) with Broadway diagonal.
 * CW winding to match Silverstone (interior = right lane, voie de droite obligatoire).
 */
public class ManhattanCircuit implements Circuit {

    private final List<Point2D.Double> meterPoints = new ArrayList<>();
    private double minX_M, maxX_M, minY_M, maxY_M, rangeX_M, rangeY_M;
    private double totalLengthM;

    // 3 lanes - same as Silverstone
    private static final double LANE_SPACING = 10.0;
    private static final double TOTAL_ROAD_WIDTH = 34.0;
    private static final double OUTER_BORDER_EXTRA = 4.0;

    // Manhattan grid - realistic block sizes (approx 80m x 270m)
    private static final double[] AVENUES_X = {0, 180, 360, 540, 720, 900};
    private static final String[] AVENUE_NAMES = {"7th Ave", "6th Ave", "5th Ave", "Madison", "Park", "Lex"};
    private static final double[] STREETS_Y = {0, 180, 360, 540, 720, 900, 1100};
    private static final String[] STREET_NAMES = {"34th St", "39th St", "42nd St", "47th St", "50th St", "55th St", "59th St"};

    public ManhattanCircuit() {
        // CW loop around Midtown - voie de droite = interior (toward center) for CW, like Silverstone
        // Starting at 34th & 7th SW, going north on 7th, east on 59th, south on Lex, west on 34th
        // Inset 80m from outer grid to leave sidewalk + building border
        double inset = 80;
        // CW points: SW -> NW -> NE -> SE -> SW
        double[][] pts = {
            {inset, inset},               // 34th & 7th SW start/finish
            {inset, 1100 - inset},        // 59th & 7th NW
            {inset + 60, 1100 - inset},   // short east along 59th
            {900 - inset - 60, 1100 - inset},
            {900 - inset, 1100 - inset},  // 59th & Lex NE
            {900 - inset, 1100 - inset - 60},
            {900 - inset, inset + 60},
            {900 - inset, inset},         // 34th & Lex SE
            {900 - inset - 60, inset},
            {inset + 60, inset},
            {inset, inset}
        };
        for (double[] p : pts) meterPoints.add(new Point2D.Double(p[0], p[1]));

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
        for (int i=0;i<meterPoints.size();i++) {
            Point2D.Double a = meterPoints.get(i);
            Point2D.Double b = meterPoints.get((i+1)%meterPoints.size());
            total += a.distance(b);
        }
        totalLengthM = total; // ~ 3500m, will scale
    }

    @Override public String getName() { return "Manhattan - Midtown Loop"; }
    @Override public int getLaneCount() { return 3; }
    @Override public double getTotalLengthMeters() { return totalLengthM * 1.35; }

    @Override
    public List<Point2D.Double> getCenterScreenPoints(int panelWidth, int panelHeight) {
        if (panelWidth <= 10 || panelHeight <= 10) return new ArrayList<>();
        int margin = 55;
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
        if (lane <0 || lane>2) lane = 1;
        double offset = (lane - 1) * LANE_SPACING; // -10,0,+10 - lane0 interior=right for CW
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
            if (c<0.3) c=0.3;
            double miter = 1.0/c;
            if (miter>1.6) miter=1.6;
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

    private Point2D.Double meterToScreen(double xM, double yM, int pw,int ph) {
        int margin=55;
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
        // Background - asphalt city
        g2.setColor(new Color(235, 235, 225));
        g2.fillRect(0,0,pw,ph);

        List<Point2D.Double> center=getCenterScreenPoints(pw,ph);
        if(center.isEmpty()) return;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Draw all building blocks between streets - realistic Manhattan blocks
        for(int i=0;i<AVENUES_X.length-1;i++){
            for(int j=0;j<STREETS_Y.length-1;j++){
                double x0 = AVENUES_X[i]+12, y0 = STREETS_Y[j]+12;
                double x1 = AVENUES_X[i+1]-12, y1 = STREETS_Y[j+1]-12;
                if(x1<=x0 || y1<=y0) continue;
                // Check if this block is on the circuit road - if so, don't draw building over road
                // Circuit is outer rectangle inset 80, so inner blocks inside loop and outer border outside
                // Outer border buildings: all blocks partly outside inset will be visible as outside city
                // We'll draw all blocks, road will be drawn on top
                Point2D.Double p0=meterToScreen(x0,y0,pw,ph);
                Point2D.Double p1=meterToScreen(x1,y1,pw,ph);
                int rx=(int)Math.min(p0.x,p1.x), ry=(int)Math.min(p0.y,p1.y);
                int rw=(int)Math.abs(p1.x-p0.x), rh=(int)Math.abs(p1.y-p0.y);
                if(rw<4 || rh<4) continue;

                // Building color variation - some taller office blocks in midtown
                boolean isMidtown = (j>=2 && j<=4); // 42nd-50th area - taller
                Color blockColor = isMidtown ? new Color(195, 190, 175) : new Color(210, 205, 190);
                if (i==1 && j==3) blockColor = new Color(180, 180, 160); // Times Square area darker
                g2.setColor(blockColor);
                g2.fillRect(rx, ry, rw, rh);

                // Building outline
                g2.setColor(new Color(160,160,150));
                g2.setStroke(new BasicStroke(0.8f));
                g2.drawRect(rx, ry, rw, rh);

                // Windows - small dots
                g2.setColor(new Color(130, 150, 180, 90));
                int winStepX = Math.max(6, rw/8);
                int winStepY = Math.max(6, rh/8);
                for(int wx=rx+4; wx<rx+rw-4; wx+=winStepX){
                    for(int wy=ry+4; wy<ry+rh-4; wy+=winStepY){
                        if (Math.random()<0.3) { // not needed random - use pattern
                        }
                        // draw window as tiny rect with slight variation
                        int winW = 2, winH=3;
                        // alternate lit windows
                        if ((wx+wy)%11==0) g2.setColor(new Color(255, 240, 150, 140));
                        else g2.setColor(new Color(100, 115, 130, 110));
                        g2.fillRect(wx, wy, winW, winH);
                    }
                }

                // Rooftop details for tall buildings
                if (isMidtown && rw>20 && rh>20) {
                    g2.setColor(new Color(80,80,80,40));
                    g2.fillRect(rx+rw/3, ry+rh/3, rw/3, rh/3);
                }
            }
        }

        // Central Park - top east area inside loop (north of 59th is outside, but park is inside north-central)
        Point2D.Double cp1=meterToScreen(460, 820, pw, ph);
        Point2D.Double cp2=meterToScreen(840, 1020, pw, ph);
        // Park is inside loop? Actually Central Park is north of circuit (outside loop) - but show as green north
        // For realism, park should be just north of 59th (outside loop) - draw north of road
        // Let's place park north of 59th street
        Point2D.Double parkOut1=meterToScreen(380, 1060+20, pw, ph);
        Point2D.Double parkOut2=meterToScreen(860, 1100+40, pw, ph);
        // But we clip to panel, so just draw inside north area as park extension
        g2.setColor(new Color(110, 170, 95));
        // Draw park as area north-east inside loop top (still inside city blocks)
        int prx=(int)Math.min(parkOut1.x, parkOut2.x);
        int pry=(int)Math.min(parkOut1.y, parkOut2.y);
        int prw=(int)Math.abs(parkOut2.x-parkOut1.x);
        int prh=(int)Math.abs(parkOut2.y-parkOut1.y);
        // Actually draw Central Park as green rectangle north of circuit (outside)
        // For our inset circuit, park is outside loop to north - simulate by drawing green at top
        // Use center area top inside loop as park for visual
        g2.setColor(new Color(85, 150, 80));
        g2.fillRect(prx, pry, prw, prh);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.ITALIC, 10));
        if(prw>60) g2.drawString("Central Park", prx+8, pry+14);

        // Draw street grid lines - avenues and streets as thin gray lines between buildings
        g2.setColor(new Color(200,200,190));
        g2.setStroke(new BasicStroke(0.9f));
        for (double ax : AVENUES_X) {
            Point2D.Double top = meterToScreen(ax, 0, pw, ph);
            Point2D.Double bottom = meterToScreen(ax, 1100, pw, ph);
            g2.drawLine((int)top.x, (int)top.y, (int)bottom.x, (int)bottom.y);
        }
        for (double sy : STREETS_Y) {
            Point2D.Double left = meterToScreen(0, sy, pw, ph);
            Point2D.Double right = meterToScreen(900, sy, pw, ph);
            g2.drawLine((int)left.x, (int)left.y, (int)right.x, (int)right.y);
        }
        // Broadway diagonal - iconic Times Square diagonal between 7th and 5th
        g2.setColor(new Color(220, 200, 130));
        g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,0,new float[]{8,6},0));
        Point2D.Double b1=meterToScreen(90, 240, pw, ph);
        Point2D.Double b2=meterToScreen(720, 980, pw, ph);
        g2.drawLine((int)b1.x,(int)b1.y,(int)b2.x,(int)b2.y);
        g2.setColor(new Color(180,160,100));
        g2.setFont(new Font("SansSerif", Font.ITALIC, 8));
        g2.drawString("Broadway", (int)meterToScreen(300, 560, pw, ph).x, (int)meterToScreen(300, 560, pw, ph).y);

        // Times Square highlight - intersection of 7th/Broadway/42nd
        Point2D.Double ts=meterToScreen(180, 360, pw, ph);
        g2.setColor(new Color(255, 220, 0, 60));
        g2.fillOval((int)ts.x-14, (int)ts.y-14, 28,28);
        g2.setColor(new Color(200, 180, 0));
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.drawString("Times Sq", (int)ts.x+16, (int)ts.y+4);

        // Draw the 3-lane circuit road on top of buildings - outer loop
        Path2D.Double centerPath=createPath(center,true);
        g2.setStroke(new BasicStroke((float)(TOTAL_ROAD_WIDTH + OUTER_BORDER_EXTRA), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(35,35,35)); // curb
        g2.draw(centerPath);
        g2.setStroke(new BasicStroke((float)TOTAL_ROAD_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(45,45,45)); // asphalt
        g2.draw(centerPath);

        // Lane dividers - 2 dividers for 3 lanes at +-5
        List<Point2D.Double> divA = offsetPath(center, -5);
        List<Point2D.Double> divB = offsetPath(center, 5);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[]{12,8}, 0));
        g2.setColor(Color.WHITE);
        g2.draw(createPath(divA, true));
        g2.draw(createPath(divB, true));

        // Start/finish line at 34th & 7th SW (first point) - across 3 lanes
        Point2D.Double start=getPosition(1, 0.0, pw, ph); // center lane
        double angle=getAngle(1, 0.0, pw, ph);
        double perp=angle+Math.PI/2;
        double halfW=TOTAL_ROAD_WIDTH/2+2;
        int x1=(int)(start.x + Math.cos(perp)*halfW);
        int y1=(int)(start.y + Math.sin(perp)*halfW);
        int x2=(int)(start.x - Math.cos(perp)*halfW);
        int y2=(int)(start.y - Math.sin(perp)*halfW);
        g2.setStroke(new BasicStroke(5f));
        g2.setColor(Color.WHITE);
        g2.drawLine(x1,y1,x2,y2);
        int segments=8;
        for(int i=0;i<segments;i++) if(i%2==1){
            double t1=(double)i/segments, t2=(double)(i+1)/segments;
            int sx1=(int)(x1+t1*(x2-x1)), sy1=(int)(y1+t1*(y2-y1));
            int sx2=(int)(x1+t2*(x2-x1)), sy2=(int)(y1+t2*(y2-y1));
            g2.setColor(Color.BLACK);
            g2.drawLine(sx1,sy1,sx2,sy2);
        }
        double arrowLen=16;
        int ax1=(int)(start.x + Math.cos(angle)*arrowLen);
        int ay1=(int)(start.y + Math.sin(angle)*arrowLen);
        g2.setColor(new Color(255,215,0));
        g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int)start.x,(int)start.y,ax1,ay1);
        double head=7;
        int hx1=(int)(ax1 - Math.cos(angle-Math.PI/6)*head);
        int hy1=(int)(ay1 - Math.sin(angle-Math.PI/6)*head);
        int hx2=(int)(ax1 - Math.cos(angle+Math.PI/6)*head);
        int hy2=(int)(ay1 - Math.sin(angle+Math.PI/6)*head);
        g2.drawLine(ax1,ay1,hx1,hy1); g2.drawLine(ax1,ay1,hx2,hy2);

        // Street labels
        g2.setColor(new Color(80,80,80));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        for(int i=0;i<AVENUES_X.length;i++){
            Point2D.Double p=meterToScreen(AVENUES_X[i], 1080, pw,ph);
            g2.drawString(AVENUE_NAMES[i], (int)p.x+2, (int)p.y-4);
        }
        for(int j=0;j<STREETS_Y.length;j++){
            Point2D.Double p=meterToScreen(880, STREETS_Y[j], pw,ph);
            g2.drawString(STREET_NAMES[j], (int)p.x+4, (int)p.y-2);
        }
        // Building labels
        g2.setColor(new Color(60,60,60));
        g2.setFont(new Font("SansSerif", Font.BOLD, 7));
        // Example building names
        Point2D.Double esb=meterToScreen(330, 80, pw, ph);
        g2.drawString("Empire State", (int)esb.x-18, (int)esb.y-6);
    }

    @Override
    public String getInfo(int totalCars) {
        return String.format("Manhattan Midtown • %.1f km • 3 voies • %d voitures", getTotalLengthMeters()/1000.0, totalCars);
    }
}
