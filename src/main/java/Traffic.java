import javax.swing.JPanel;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Traffic panel with circuit + overtaking rules (voie de droite obligatoire)
 */
public class Traffic extends JPanel implements Runnable {

    private static final Logger logger = LogManager.getLogger(Traffic.class);
    private Thread thread;
    private final List<List<Car>> carLists;
    private final Circuit circuit;
    private final int FPS = 30;
    private final int targetTime = 1000 / FPS;
    private boolean running = true;

    // Overtaking thresholds tuned for screen pixels - French code de la route - voie de droite obligatoire stricte
    private static final double OVERTAKE_TRIGGER = 30.0;
    private static final double SAFE_FOLLOW = 20.0;
    private static final double STOP_DIST = 7.0;
    private static final double LANE_SAFE_FRONT = 28.0;
    private static final double LANE_SAFE_BACK = 16.0;
    private static final double OVERTAKE_BLOCK_DIST = 45.0;
    private static final double RIGHT_LANE_BLOCK_FRONT = 32.0; // interdiction dépasser par la droite - très stricte
    private static final double RIGHT_LANE_BLOCK_BACK = 16.0;
    private static final double RETURN_SAFE_FRONT = 8.0; // ultra permissif pour rabattement rapide (voie de droite)
    private static final double RETURN_SAFE_BACK = 5.0;
    private static final int FORCED_RETURN_TICKS = 60; // ~2 sec, force retour droite si oublié

    public Traffic(List<List<Car>> carLists) {
        this(carLists, new SilverstoneCircuit());
    }

    public Traffic(List<List<Car>> carLists, Circuit circuit) {
        super(null);
        this.carLists = carLists;
        this.circuit = circuit;
        setBackground(new Color(34, 139, 34));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this, "traffic-simulation");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public void update() {
        boolean circuitMode = isCircuitMode();
        if (circuitMode) {
            updateWithOvertaking();
        } else {
            for (List<Car> cars : carLists) updateCars(cars);
        }
        if (circuitMode) syncCircuitPositions();
    }

    private boolean isCircuitMode() {
        if (circuit != null) {
            for (List<Car> list : carLists) for (Car c : list) if (c.isCircuitMode()) return true;
        }
        return circuit != null && !carLists.isEmpty();
    }

    private void syncCircuitPositions() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        for (List<Car> cars : carLists) for (Car car : cars) {
            if (!car.isCircuitMode()) continue;
            Point2D.Double pos;
            if (circuit instanceof ManhattanCircuit && car.getRoadId() != 0) {
                ManhattanCircuit mc = (ManhattanCircuit) circuit;
                int roadIdx = car.getRoadId() - 1;
                pos = mc.getInnerRoadPosition(roadIdx, car.getLane(), car.getProgress(), w, h);
            } else {
                pos = circuit.getPosition(car.getLane(), car.getProgress(), w, h);
            }
            car.setX((int) pos.x); car.setY((int) pos.y);
        }
    }

    public void updateCars(List<Car> cars) {
        IntStream.range(0, cars.size()).forEachOrdered(i -> {
            Car car = cars.get(i);
            int curX = car.getX();
            if (curX < this.getWidth()) {
                Car next = cars.get((i+1)%cars.size());
                adjustSpeedToNextCar(car, next);
                logger.debug("Car {} X={} next {} X={} d={}", car.getId(), car.getX(), next.getId(), next.getX(), next.getX()-car.getX());
            } else car.setX(0);
        });
    }

    /** New overtaking logic: voie de droite obligatoire, dépassement par la gauche si libre */
    private void updateWithOvertaking() {
        int w = getWidth(), h = getHeight();
        if (w <= 10 || h <= 10) return;
        int laneCount = circuit.getLaneCount();
        // Update traffic lights (feux aléatoires) - each light cycles independently
        for (TrafficLight tl : circuit.getTrafficLights()) tl.update();
        // Ensure carLists size matches laneCount (for Manhattan 3, Silverstone 3)
        while (carLists.size() < laneCount) carLists.add(new ArrayList<>());
        // Build lane lengths map for outer circuit
        java.util.Map<Integer, Double> laneLengths = new java.util.HashMap<>();
        for (int lane=0; lane<laneCount; lane++) laneLengths.put(lane, circuit.getTotalLengthScreen(lane, w, h));
        // Inner roads lengths for Manhattan (for roadId 1,2)
        java.util.Map<Integer, java.util.Map<Integer, Double>> innerLengths = new java.util.HashMap<>();
        if (circuit instanceof ManhattanCircuit) {
            ManhattanCircuit mc = (ManhattanCircuit) circuit;
            for (int r=0; r<mc.getInnerRoads().size(); r++) {
                java.util.Map<Integer, Double> m = new java.util.HashMap<>();
                for (int lane=0; lane<mc.getInnerRoads().get(r).laneCount; lane++) m.put(lane, mc.getInnerRoadLength(r, lane, w, h));
                innerLengths.put(r+1, m);
            }
        }

        // Flatten all cars for global checks
        List<Car> allCars = carLists.stream().flatMap(List::stream).collect(Collectors.toList());
        if (allCars.isEmpty()) return;

        // Collect lane change requests to apply after decision
        List<LaneChange> changes = new ArrayList<>();

        // Process cars sorted by progress descending (front first) to give priority to front
        List<Car> sorted = new ArrayList<>(allCars);
        sorted.sort((a,b) -> Double.compare(b.getProgress(), a.getProgress()));

        for (Car car : sorted) {
            int lane = car.getLane();
            if (lane <0) continue;
            int roadId = car.getRoadId();
            double totalLength;
            if (roadId==0) totalLength = laneLengths.getOrDefault(lane, laneLengths.get(0));
            else {
                java.util.Map<Integer, Double> m = innerLengths.get(roadId);
                totalLength = (m!=null) ? m.getOrDefault(lane, 1000.0) : 1000.0;
            }
            if (totalLength < 1e-6) continue;

            // Traffic lights and roundabouts update is handled once per tick outside loop
            // Reset left-lane counter when not illegitimately in left lane
            if (lane == 0) car.resetLeftLaneTicks();
            else if (car.isOvertaking()) car.resetLeftLaneTicks();

            List<Car> myLaneCars = getCarsInLaneForRoad(lane, roadId);
            Car ahead = findClosestAhead(car, myLaneCars, totalLength);
            double distToAhead = ahead != null ? arcDistance(car, ahead, totalLength) : Double.MAX_VALUE;

            // French code - feux tricolores : arrêt au rouge, priorité à l'autre axe (arrêt complet)
            boolean stoppedByLight = false;
            for (TrafficLight tl : circuit.getTrafficLights()) {
                double dLight = tl.distanceToLight(car, totalLength);
                boolean redOrYellowShouldStop = tl.shouldStop(car, totalLength, 20);
                if (redOrYellowShouldStop) {
                    if (dLight < 3.5) car.forceStopAtLight();
                    else if (dLight < 10) car.brakeTrack();
                    else if (dLight < 20) car.slowTrack();
                    else car.slowTrack();
                    car.setOvertaking(false);
                    stoppedByLight = true;
                    logger.debug("Car {} lane {} stopped at red light {} dist {:.1f} state {}", car.getId(), lane, tl.getName(), dLight, tl.getState());
                    break;
                }
                // Anticipation: orange = ralentir si peut s'arreter à temps
                if (tl.isYellow() && dLight > 8 && dLight < 25) {
                    car.slowTrack();
                }
            }
            if (stoppedByLight) continue;

            // French code - ronds-points : céder le passage à ceux déjà dans l'anneau (priorité à gauche / à l'intérieur)
            boolean yielding = false;
            for (Roundabout rb : circuit.getRoundabouts()) {
                if (rb.shouldYield(car, allCars, totalLength)) {
                    double dEntry = rb.distanceToEntry(car, totalLength);
                    if (dEntry < 4) car.forceStopAtLight();
                    else if (dEntry < 12) car.brakeTrack();
                    else car.slowTrack();
                    car.setOvertaking(false);
                    yielding = true;
                    logger.debug("Car {} lane {} yielding at roundabout {} dist {:.1f}", car.getId(), lane, rb.getName(), dEntry);
                    break;
                }
            }
            if (yielding) continue;

            // French code: interdiction de doubler par la droite - any lane except leftmost must not overtake on right
            Car leftAheadForRight = null;
            double distLeftForRight = Double.MAX_VALUE;
            if (lane < laneCount - 1) {
                leftAheadForRight = findClosestAheadInLeftLanes(car, lane, allCars, totalLength);
                if (leftAheadForRight != null) distLeftForRight = arcDistance(car, leftAheadForRight, totalLength);
                if (leftAheadForRight != null && distLeftForRight < RIGHT_LANE_BLOCK_FRONT && distLeftForRight > 0.5) {
                    if (distLeftForRight <= STOP_DIST) car.stopTrack();
                    else if (distLeftForRight <= SAFE_FOLLOW) car.slowTrack();
                    else {
                        if (car.getTrackSpeed() > leftAheadForRight.getTrackSpeed()) {
                            car.setTrackSpeed(Math.min(car.getTrackSpeed(), Math.max(1, leftAheadForRight.getTrackSpeed())));
                        }
                        car.slowTrack();
                    }
                    car.setOvertaking(false);
                    logger.debug("Car {} lane {} blocked by left lane {} ahead at {:.1f} - no overtaking on right (code de la route)", car.getId(), lane, leftAheadForRight.getId(), distLeftForRight);
                    continue;
                }
            }

            // Right lane (voie de droite = 0) is mandatory
            if (lane == 0) {
                // Check if need to overtake: car faster than ahead
                boolean wantsToOvertake = ahead != null && distToAhead < OVERTAKE_TRIGGER && car.getMaxSpeed() > ahead.getMaxSpeed();
                // Also if ahead is stopped/slow and distance is closing
                if (ahead != null && distToAhead < OVERTAKE_TRIGGER && car.getTrackSpeed() > ahead.getTrackSpeed()) wantsToOvertake = true;

                if (wantsToOvertake) {
                    int targetLane = lane + 1;
                    if (targetLane < laneCount) {
                        double targetLen = laneLengths.get(targetLane);
                        boolean free = isLaneFree(car, targetLane, targetLen);
                        boolean blocked = isOvertakingBlocked(car, targetLane, allCars, targetLen);
                        if (free && !blocked) {
                            // Do overtake
                            changes.add(new LaneChange(car, targetLane, ahead));
                            car.setOvertaking(true);
                            car.setOvertakeTarget(ahead);
                            car.accelerateTrack();
                            logger.info("Car {} (v{} lane {}) wants to overtake {} (v{} ) -> switching to lane {} (free={}, blocked={})", car.getId(), car.getTrackSpeed(), lane, ahead.getId(), ahead.getTrackSpeed(), targetLane, free, blocked);
                        } else {
                            // Must slow and wait behind
                            if (distToAhead <= STOP_DIST) car.stopTrack();
                            else if (distToAhead <= SAFE_FOLLOW) car.slowTrack();
                            else car.slowTrack(); // approach
                            car.setOvertaking(false);
                            logger.debug("Car {} blocked to overtake {} (free={} blocked={}) dist={} -> slow", car.getId(), ahead.getId(), free, blocked, distToAhead);
                        }
                    } else {
                        // No lane to overtake, must slow
                        if (distToAhead <= STOP_DIST) car.stopTrack();
                        else if (distToAhead <= SAFE_FOLLOW) car.slowTrack();
                        else car.slowTrack();
                    }
                } else {
                    // No need to overtake, normal follow
                    if (ahead != null) {
                        if (distToAhead <= STOP_DIST) car.stopTrack();
                        else if (distToAhead <= SAFE_FOLLOW) car.slowTrack();
                        else car.accelerateTrack();
                    } else {
                        car.accelerateTrack();
                    }
                    car.setOvertaking(false);
                }
            } else {
                // Left lane(s) - currently overtaking
                if (car.isOvertaking()) {
                    Car target = car.getOvertakeTarget();
                    boolean complete = false;
                    if (target != null) {
                        // Check if we have passed target: target is now behind us by safe distance
                        // Compute signed delta from us to target: if target behind, delta large
                        double deltaToTarget = arcDistance(car, target, totalLength); // distance from us to target ahead (wrapping)
                        // If target behind, delta will be > totalLength - 20 (target is just behind)
                        // More robust: compute progress difference
                        double progDelta = (target.getProgress() - car.getProgress() + 1) % 1;
                        double behindDist = (1 - progDelta) * totalLength; // distance from car to target behind? Actually if target behind, progDelta ~0.95, behindDist ~0.05*total
                        // Let's use: if progDelta > 0.5, target is behind (we are ahead)
                        if (progDelta > 0.5) {
                            double distBehind = (1 - progDelta) * totalLength;
                            // Actually distBehind is small when just passed, we want to ensure we are ahead by safe distance
                            // Compute distance from us to target behind = totalLength - deltaToTarget ? Let's just check progDelta >0.5 and distToAhead for target behind
                            // Use behindDist as distance target is behind us
                            double targetBehindDist = progDelta > 0.5 ? (1 - progDelta) * totalLength : Double.MAX_VALUE;
                            // Wait we already have progDelta, if >0.5 we are ahead, distance behind = (1-progDelta)*total
                            // If we are ahead by 30, progDelta ~ 0.98 for total 2000, behindDist ~40, good
                            if (progDelta > 0.52 && (1 - progDelta) * totalLength > 28) complete = true;
                            // Also if target far behind (> half lap) consider complete (lapped)
                            if (progDelta > 0.6) complete = true;
                        }
                    } else {
                        complete = true;
                    }
                    if (complete) {
                        // Try to return to right lane (voie de droite obligatoire)
                        int returnLane = lane - 1;
                        if (returnLane >= 0) {
                            double retLen = laneLengths.get(returnLane);
                            boolean free = isLaneFree(car, returnLane, retLen);
                            if (free) {
                                changes.add(new LaneChange(car, returnLane, null));
                                car.setOvertaking(false);
                                car.setOvertakeTarget(null);
                                car.accelerateTrack();
                                logger.info("Car {} completed overtake of {} -> returning to lane {}", car.getId(), target!=null?target.getId():"?", returnLane);
                            } else {
                                // Stay left, keep speed
                                car.accelerateTrack();
                            }
                        } else {
                            car.setOvertaking(false);
                            car.setOvertakeTarget(null);
                        }
                    } else {
                        Car aheadLeft = findClosestAhead(car, getCarsInLaneForRoad(lane, roadId), totalLength);
                        double distLeft = aheadLeft != null ? arcDistance(car, aheadLeft, totalLength) : Double.MAX_VALUE;
                        if (aheadLeft != null && distLeft < OVERTAKE_TRIGGER && car.getMaxSpeed() > aheadLeft.getMaxSpeed()) {
                            // Want to overtake further left
                            int targetLane = lane + 1;
                            if (targetLane < laneCount) {
                                double tl = laneLengths.get(targetLane);
                                boolean free = isLaneFree(car, targetLane, tl);
                                boolean blocked = isOvertakingBlocked(car, targetLane, allCars, tl);
                                if (free && !blocked) {
                                    changes.add(new LaneChange(car, targetLane, aheadLeft));
                                    car.setOvertakeTarget(aheadLeft);
                                    car.accelerateTrack();
                                } else {
                                    if (distLeft <= STOP_DIST) car.stopTrack();
                                    else if (distLeft <= SAFE_FOLLOW) car.slowTrack();
                                    else car.slowTrack();
                                }
                            } else {
                                if (distLeft <= STOP_DIST) car.stopTrack();
                                else if (distLeft <= SAFE_FOLLOW) car.slowTrack();
                                else car.accelerateTrack();
                            }
                        } else {
                            if (aheadLeft != null && distLeft <= STOP_DIST) car.stopTrack();
                            else if (aheadLeft != null && distLeft <= SAFE_FOLLOW) car.slowTrack();
                            else car.accelerateTrack();
                        }
                    }
                } else {
                    // In left lane but not overtaking -> voie de droite obligatoire: return ASAP
                    car.incrementLeftLaneTicks();
                    int returnLane = lane - 1;
                    if (returnLane >= 0) {
                        double retLen = laneLengths.get(returnLane);
                        boolean free = isLaneFreeForReturn(car, returnLane, retLen);
                        // Forced return after too long in left lane not overtaking (French code: must return)
                        boolean forced = car.getLeftLaneTicks() > FORCED_RETURN_TICKS && isLaneFreeForForcedReturn(car, returnLane, retLen);
                        if (free || forced) {
                            changes.add(new LaneChange(car, returnLane, null));
                            car.setOvertaking(false);
                            car.setOvertakeTarget(null);
                            car.resetLeftLaneTicks();
                            car.accelerateTrack();
                            logger.info("Car {} in left lane {} returning to right lane {} (rabattement obligatoire{} )", car.getId(), lane, returnLane, forced ? " forcé" : "");
                            continue;
                        }
                    } else {
                        car.resetLeftLaneTicks();
                    }
                    Car aheadLeft = findClosestAhead(car, getCarsInLaneForRoad(lane, roadId), totalLength);
                    double distLeft = aheadLeft != null ? arcDistance(car, aheadLeft, totalLength) : Double.MAX_VALUE;
                    boolean wantsFurther = aheadLeft != null && distLeft < OVERTAKE_TRIGGER && car.getMaxSpeed() > aheadLeft.getMaxSpeed();
                    if (wantsFurther) {
                        int targetLane = lane + 1;
                        if (targetLane < laneCount) {
                            double tl = laneLengths.get(targetLane);
                            boolean free = isLaneFree(car, targetLane, tl);
                            boolean blocked = isOvertakingBlocked(car, targetLane, allCars, tl);
                            if (free && !blocked) {
                                changes.add(new LaneChange(car, targetLane, aheadLeft));
                                car.setOvertaking(true); car.setOvertakeTarget(aheadLeft);
                                car.accelerateTrack();
                            } else {
                                if (distLeft <= STOP_DIST) car.stopTrack(); else if (distLeft <= SAFE_FOLLOW) car.slowTrack(); else car.slowTrack();
                            }
                        } else {
                            // Leftmost lane cannot go further left -> must follow, no overtaking on right
                            if (distLeft <= STOP_DIST) car.stopTrack(); else if (distLeft <= SAFE_FOLLOW) car.slowTrack(); else car.slowTrack();
                        }
                    } else {
                        // Return was blocked and no further overtake needed -> must wait, check blocker
                        if (returnLane >= 0) {
                            Car blocker = findBlockingCarForReturn(car, returnLane, laneLengths.get(returnLane));
                            if (blocker != null) {
                                double deltaB = (blocker.getProgress() - car.getProgress() + 1.0) % 1.0;
                                double distAheadB = deltaB * laneLengths.get(returnLane);
                                double distBehindB = (1 - deltaB) * laneLengths.get(returnLane);
                                boolean blockerAhead = distAheadB < RETURN_SAFE_FRONT && distAheadB > 0.5 && deltaB < 0.5;
                                boolean blockerBehind = distBehindB < RETURN_SAFE_BACK && distBehindB > 0.5 && deltaB > 0.5;
                                if (blockerAhead && car.getMaxSpeed() > blocker.getMaxSpeed()) {
                                    car.setOvertaking(true);
                                    car.setOvertakeTarget(blocker);
                                    logger.info("Car {} lane {} blocked return to {} by {} ahead at {:.1f} -> must overtake blocker first (voie de gauche)", car.getId(), lane, returnLane, blocker.getId(), distAheadB);
                                    car.accelerateTrack();
                                } else if (blockerBehind) {
                                    logger.debug("Car {} lane {} blocked behind by {} at {:.1f} -> wait (code de la route)", car.getId(), lane, blocker.getId(), distBehindB);
                                    if (aheadLeft != null && distLeft <= STOP_DIST) car.stopTrack();
                                    else if (aheadLeft != null && distLeft <= SAFE_FOLLOW) car.slowTrack();
                                    else car.slowTrack();
                                } else {
                                    if (aheadLeft != null && distLeft <= STOP_DIST) car.stopTrack();
                                    else if (aheadLeft != null && distLeft <= SAFE_FOLLOW) car.slowTrack();
                                    else car.slowTrack();
                                }
                            } else {
                                if (aheadLeft != null && distLeft <= STOP_DIST) car.stopTrack();
                                else if (aheadLeft != null && distLeft <= SAFE_FOLLOW) car.slowTrack();
                                else car.slowTrack();
                            }
                        } else {
                            car.accelerateTrack();
                        }
                    }
                }
            }
        }

        // Advance all cars (using their current lane length before change, then lane change will affect next tick sync)
        for (Car car : allCars) {
            int lane = car.getLane();
            // If car is scheduled to change, we still advance with old lane length for this tick, then change
            double tl = laneLengths.getOrDefault(lane, laneLengths.get(0));
            car.advanceTrack(tl);
        }

        // Apply lane changes (move between carLists)
        for (LaneChange lc : changes) {
            Car car = lc.car;
            int from = car.getLane();
            int to = lc.toLane;
            // remove from old list
            for (List<Car> list : carLists) {
                if (list.contains(car)) { list.remove(car); break; }
            }
            while (carLists.size() <= to) carLists.add(new ArrayList<>());
            carLists.get(to).add(car);
            car.setLane(to);
            // if this was start of overtake, keep overtaking flag; if return, already cleared
        }
    }

    private double arcDistance(Car from, Car to, double totalLength) {
        double delta = (to.getProgress() - from.getProgress() + 1.0) % 1.0;
        if (delta < 1e-6) return totalLength; // same position, treat as full lap
        return delta * totalLength;
    }

    private Car findClosestAhead(Car me, List<Car> laneCars, double totalLength) {
        if (laneCars == null || laneCars.isEmpty()) return null;
        Car best = null; double bestDist = Double.MAX_VALUE;
        for (Car other : laneCars) {
            if (other == me) continue;
            double d = arcDistance(me, other, totalLength);
            if (d < 1e-6) continue;
            if (d < bestDist) { bestDist = d; best = other; }
        }
        return best;
    }

    private Car findClosestAheadInLeftLanes(Car me, int myLane, List<Car> allCars, double totalLength) {
        Car best = null; double bestDist = Double.MAX_VALUE;
        for (Car other : allCars) {
            if (other == me) continue;
            if (other.getRoadId() != me.getRoadId()) continue;
            if (other.getLane() <= myLane) continue; // only lanes to the left (higher index)
            double d = arcDistance(me, other, totalLength);
            if (d < 1e-6 || d > totalLength * 0.5) continue; // only consider ahead within half lap
            if (d < bestDist) { bestDist = d; best = other; }
        }
        return best;
    }

    private List<Car> getCarsInLane(int lane) {
        if (lane <0 || lane >= carLists.size()) return new ArrayList<>();
        return carLists.get(lane);
    }
    private List<Car> getCarsInLaneForRoad(int lane, int roadId) {
        if (lane <0 || lane >= carLists.size()) return new ArrayList<>();
        List<Car> all = carLists.get(lane);
        List<Car> filtered = new ArrayList<>();
        for (Car c : all) if (c.getRoadId()==roadId) filtered.add(c);
        return filtered;
    }

    private boolean isLaneFree(Car me, int targetLane, double targetTotalLength) {
        List<Car> targetCars = getCarsInLaneForRoad(targetLane, me.getRoadId());
        if (targetCars.isEmpty()) return true;
        for (Car other : targetCars) {
            double delta = (other.getProgress() - me.getProgress() + 1.0) % 1.0;
            double distAhead = delta * targetTotalLength;
            double distBehind = (1 - delta) * targetTotalLength;
            // consider wrap: if other is ahead within SAFE_FRONT or behind within SAFE_BACK, lane not free
            if (distAhead < LANE_SAFE_FRONT && distAhead > 0.5) return false;
            if (distBehind < LANE_SAFE_BACK && distBehind > 0.5) return false;
            // also check very close behind (other just behind me)
            if (delta > 0.92) { // other is just behind (behindDist = (1-delta)*total)
                double behind = (1 - delta) * targetTotalLength;
                if (behind < LANE_SAFE_BACK) return false;
            }
        }
        return true;
    }

    private boolean isLaneFreeForReturn(Car me, int targetLane, double targetTotalLength) {
        List<Car> targetCars = getCarsInLaneForRoad(targetLane, me.getRoadId());
        if (targetCars.isEmpty()) return true;
        for (Car other : targetCars) {
            double delta = (other.getProgress() - me.getProgress() + 1.0) % 1.0;
            double distAhead = delta * targetTotalLength;
            double distBehind = (1 - delta) * targetTotalLength;
            if (distAhead < RETURN_SAFE_FRONT && distAhead > 0.5) return false;
            if (distBehind < RETURN_SAFE_BACK && distBehind > 0.5) return false;
            if (delta > 0.94) {
                double behind = (1 - delta) * targetTotalLength;
                if (behind < RETURN_SAFE_BACK) return false;
            }
        }
        return true;
    }

    private boolean isLaneFreeForForcedReturn(Car me, int targetLane, double targetTotalLength) {
        List<Car> targetCars = getCarsInLaneForRoad(targetLane, me.getRoadId());
        if (targetCars.isEmpty()) return true;
        for (Car other : targetCars) {
            double delta = (other.getProgress() - me.getProgress() + 1.0) % 1.0;
            double distAhead = delta * targetTotalLength;
            double distBehind = (1 - delta) * targetTotalLength;
            if (distAhead < 4.0 && distAhead > 0.5) return false;
            if (distBehind < 2.0 && distBehind > 0.5) return false;
        }
        return true;
    }

    private Car findBlockingCarForReturn(Car me, int targetLane, double targetTotalLength) {
        List<Car> targetCars = getCarsInLaneForRoad(targetLane, me.getRoadId());
        if (targetCars.isEmpty()) return null;
        Car blocker = null; double bestDist = Double.MAX_VALUE;
        for (Car other : targetCars) {
            double delta = (other.getProgress() - me.getProgress() + 1.0) % 1.0;
            double distAhead = delta * targetTotalLength;
            double distBehind = (1 - delta) * targetTotalLength;
            if ((distAhead < RETURN_SAFE_FRONT && distAhead > 0.5) || (distBehind < RETURN_SAFE_BACK && distBehind > 0.5)) {
                // This car blocks return - pick closest ahead
                if (distAhead < bestDist) { bestDist = distAhead; blocker = other; }
                if (distBehind < bestDist && distBehind > 0.5) { bestDist = distBehind; blocker = other; }
            }
        }
        // If no blocker found via strict, check closest ahead in return lane
        if (blocker == null) {
            double bestAhead = Double.MAX_VALUE; Car bestA = null;
            for (Car other : targetCars) {
                double d = arcDistance(me, other, targetTotalLength);
                if (d < bestAhead) { bestAhead = d; bestA = other; }
            }
            if (bestA != null && bestAhead < LANE_SAFE_FRONT) blocker = bestA;
        }
        return blocker;
    }

    private boolean isOvertakingBlocked(Car me, int targetLane, List<Car> allCars, double targetLen) {
        for (Car other : allCars) {
            if (other == me) continue;
            if (!other.isOvertaking()) continue;
            if (other.getLane() != targetLane) continue;
            if (other.getRoadId() != me.getRoadId()) continue;
            double delta = (other.getProgress() - me.getProgress() + 1.0) % 1.0;
            double distAhead = delta * targetLen;
            double distBehind = (1 - delta) * targetLen;
            // if other overtaking car is within block distance ahead or behind
            if (distAhead < OVERTAKE_BLOCK_DIST && distAhead > 0.5) return true;
            if (distBehind < OVERTAKE_BLOCK_DIST && distBehind > 0.5) return true;
        }
        return false;
    }

    private static class LaneChange {
        Car car; int toLane; Car target;
        LaneChange(Car c,int to, Car t){car=c; toLane=to; target=t;}
        LaneChange(Car c,int to){this(c,to,null);}
    }

    @Override
    public void run() {
        long startTime, urdTime, waitTime;
        while (running) {
            startTime = System.nanoTime();
            update();
            repaint();
            urdTime = (System.nanoTime() - startTime) / 1_000_000;
            waitTime = targetTime - urdTime;
            try { Thread.sleep(Math.max(waitTime, 0)); } catch (InterruptedException e) {
                logger.error("Simulation thread interrupted", e);
                Thread.currentThread().interrupt(); running=false;
            }
        }
    }

    private void adjustSpeedToNextCar(Car cur, Car next) {
        if (next.getY() == cur.getY()) {
            int d = Math.abs(next.getX() - cur.getX());
            if (d <= 20) {
                if (d > 5) { logger.debug("Car {} close {} from {} slow", cur.getId(), d, next.getId()); cur.slowX(); }
                else { logger.debug("Car {} too close {} from {} stop", cur.getId(), d, next.getId()); cur.stopX(); }
            } else { logger.debug("Car {} good {} from {} accel", cur.getId(), d, next.getId()); cur.accelerateX(); }
        } else cur.accelerateX();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight();
        if (isCircuitMode() && circuit != null) {
            circuit.draw(g2, w, h);
            int totalCars = carLists.stream().mapToInt(List::size).sum();
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            String title = circuit.getName();
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(title);
            g2.drawString(title, w/2 - tw/2, 28);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.setColor(new Color(220,220,220));
            String info = circuit.getInfo(totalCars);
            fm = g2.getFontMetrics(); tw = fm.stringWidth(info);
            g2.drawString(info, w/2 - tw/2, 46);
            // Lane occupancy HUD - proof voie de droite obligatoire
            long rightCount = carLists.size()>0 ? carLists.get(0).size() : 0;
            long leftCount = carLists.size()>1 ? carLists.get(1).size() : 0;
            long left2Count = carLists.size()>2 ? carLists.get(2).size() : 0;
            long overtakingCount = carLists.stream().flatMap(List::stream).filter(Car::isOvertaking).count();
            g2.setFont(new Font("SansSerif", Font.BOLD, 11));
            String hud = String.format("Voie de droite : %d  |  Voie de gauche: %d" + (left2Count>0 ? "  |  Voie 3: %d" : "") + "  |  Depassements: %d / %d", rightCount, leftCount, left2Count, overtakingCount, totalCars);
            fm = g2.getFontMetrics(); tw = fm.stringWidth(hud);
            g2.setColor(new Color(0,0,0,150));
            g2.fillRoundRect(w/2 - tw/2 -6, 52, tw+12, 16, 8,8);
            g2.setColor(new Color(255,215,0));
            g2.drawString(hud, w/2 - tw/2, 64);
            // Legend
            g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
            String leg = "Voie de droite = par defaut (interieur pour ce circuit) - Voie de gauche = depassement uniquement - rotation conservee (CCW Manhattan / CW Silverstone)";
            fm = g2.getFontMetrics(); tw = fm.stringWidth(leg);
            g2.setColor(new Color(0,0,0,120));
            g2.fillRoundRect(w/2 - tw/2 -5, h-20, tw+10, 14, 6,6);
            g2.setColor(Color.WHITE);
            g2.drawString(leg, w/2 - tw/2, h-10);
            carLists.forEach(cars -> drawCars(g, cars));
        } else {
            g.setColor(Color.DARK_GRAY);
            g.drawLine(0,40,w,40); g.drawLine(0,60,w,60); g.drawLine(0,80,w,80); g.drawLine(0,100,w,100);
            carLists.forEach(cars -> drawCars(g, cars));
        }
    }

    private void drawCars(Graphics g, List<Car> cars) {
        for (Car car : cars) createObject(g, car);
    }

    private void createObject(Graphics g, Car car) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (car.isCircuitMode() && circuit != null) {
            int lane = car.getLane();
            double prog = car.getProgress();
            int w = getWidth(), h = getHeight();
            double angle = circuit.getAngle(lane, prog, w, h);
            int cx = car.getX(), cy = car.getY();
            int carW = 11, carH = 6;
            // highlight overtaking cars with yellow border
            if (car.isOvertaking()) {
                g2.setColor(new Color(255,215,0,180));
                g2.translate(cx,cy); g2.rotate(angle);
                g2.fillRect(-carW/2-2, -carH/2-2, carW+4, carH+4);
                g2.setColor(car.getColor());
                g2.fillRect(-carW/2, -carH/2, carW, carH);
            } else {
                g2.setColor(car.getColor());
                g2.translate(cx,cy); g2.rotate(angle);
                g2.fillRect(-carW/2, -carH/2, carW, carH);
            }
            g2.setColor(Color.BLACK);
            g2.fillRect(-1, -carH/2+1, 4, carH-2);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 7));
            g2.rotate(-angle);
            g2.drawString(String.valueOf(car.getId()), -8, -9);
            // arrow for overtaking
            if (car.isOvertaking()) {
                g2.setColor(Color.YELLOW);
                g2.setFont(new Font("SansSerif", Font.BOLD, 6));
                g2.drawString("↑", 4, -8);
            }
            g2.dispose();
        } else {
            g.setColor(car.getColor()); g.fillRect(car.getX(), car.getY(), 5,5);
            g.drawString(String.valueOf(car.getId()), car.getX(), car.getY()-5);
        }
    }
}
