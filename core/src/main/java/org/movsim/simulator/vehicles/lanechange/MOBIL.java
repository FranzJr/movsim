/*
 * Copyright (C) 2010, 2011, 2012 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden
 *                                   <movsim.org@gmail.com>
 * -----------------------------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator.
 * 
 * MovSim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MovSim. If not, see <http://www.gnu.org/licenses/>
 * or <http://www.movsim.org>.
 * 
 * -----------------------------------------------------------------------------------------
 */
package org.movsim.simulator.vehicles.lanechange;

import org.movsim.input.model.vehicle.lanechange.LaneChangeMobilData;
import org.movsim.simulator.roadnetwork.Lane;
import org.movsim.simulator.roadnetwork.LaneSegment;
import org.movsim.simulator.roadnetwork.RoadSegment;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.longitudinalmodel.LongitudinalModelBase.ModelName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class MOBIL.
 */
public class MOBIL {
    
    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(MOBIL.class);

    private double politeness;

    /** changing threshold */
    private double threshold;

    /** maximum safe braking decelerations */
    private double bSafe;

    /** minimum safe (net) distance */
    private double gapMin;

    /** bias (m/s^2) to drive */
    private double biasRight;

    private double thresholdRef;

    private double biasRightRef;

    private double bSafeRef;

    private double pRef;

    /**
     * Instantiates a new MOBIL.
     * 
     * @param vehicle
     *            the vehicle
     */
    public MOBIL(Vehicle vehicle) {
        // TODO handle this case with *no* <MOBIL> xml element
    }

    /**
     * Instantiates a new MOBIL.
     * 
     * @param vehicle
     *            the vehicle
     * @param lcMobilData
     *            the lane change MOBIL data
     */
    public MOBIL(Vehicle vehicle, LaneChangeMobilData lcMobilData) {
        if (lcMobilData.getSafeDeceleration() > vehicle.getMaxDeceleration()) {
            // MOBIL bSafe parameter should be typically chosen well below the physical maximum deceleration
            logger.error("not consistent modeling input data: MOBIL's bSafe must be <= vehicle's maximum deceleration."
                    + " Otherwise crashes could occur! Restrict bSafe to maximum deceleration={}",
                    vehicle.getMaxDeceleration());
        }
        bSafeRef = bSafe = Math.min(lcMobilData.getSafeDeceleration(), vehicle.getMaxDeceleration());
        biasRightRef = biasRight = lcMobilData.getRightBiasAcceleration();
        gapMin = lcMobilData.getMinimumGap();
        thresholdRef = threshold = lcMobilData.getThresholdAcceleration();
        pRef = politeness = lcMobilData.getPoliteness();
    }

    public MOBIL(Vehicle vehicle, double minimumGap, double safeDeceleration, double politeness,
            double thresholdAcceleration, double rightBiasAcceleration) {
        bSafeRef = bSafe = safeDeceleration;
        biasRightRef = biasRight = rightBiasAcceleration;
        gapMin = minimumGap;
        thresholdRef = threshold = thresholdAcceleration;
        pRef = this.politeness = politeness;
    }

    public boolean safetyCheckAcceleration(double acc) {
        return acc <= -bSafe;
    }

    public double calcAccelerationBalance(Vehicle me, int direction, RoadSegment roadSegment) {

        // set prospectiveBalance to large negative to indicate no lane change when not safe
        double prospectiveBalance = -Double.MAX_VALUE;
        final int currentLane = me.getLane();
        final int newLane = currentLane + direction;
        final LaneSegment newLaneSegment = roadSegment.laneSegment(newLane);
        if (newLaneSegment.type() == Lane.Type.ENTRANCE) {
            // never change lane into an entrance lane
            return prospectiveBalance;
        }

        final Vehicle newFront = newLaneSegment.frontVehicle(me);
        if (newFront != null) {
            if (newFront.inProcessOfLaneChange()) {
                return prospectiveBalance;
            }
            final double gapFront = me.getNetDistance(newFront);
            if (gapFront < gapMin) {
                return prospectiveBalance;
            }
        }
        final Vehicle newBack = newLaneSegment.rearVehicle(me);
        if (newBack != null) {
            if (newBack.inProcessOfLaneChange()) {
                return prospectiveBalance;
            }
            final double gapRear = newBack.getNetDistance(me);
            if (gapRear < gapMin) {
                return prospectiveBalance;
            }
        }
        final LaneSegment currentLaneSegment = roadSegment.laneSegment(currentLane);
        final Vehicle oldFront = currentLaneSegment.frontVehicle(me);
        if (oldFront != null) {
            if (oldFront.inProcessOfLaneChange()) {
                return prospectiveBalance;
            }
        }

        // new situation: newBack with me as leader and following left lane cases
        // TO_LEFT --> just the actual situation
        // TO_RIGHT --> consideration of left-lane (with me's leader) has no effect
        // temporarily add the current vehicle to the new lane to calculate the new accelerations
        me.setLane(newLane);
        final int index = newLaneSegment.addVehicleTemp(me);
        final double newBackNewAcc = newBack == null ? 0 : newBack.calcAccModel(newLaneSegment, null);
        final double meNewAcc = me.calcAccModel(newLaneSegment, null);
        newLaneSegment.removeVehicle(index);
        me.setLane(currentLane);

        if (safetyCheckAcceleration(newBackNewAcc)) {
            return prospectiveBalance;
        }

        // check now incentive criterion
        // consider three vehicles: me, oldBack, newBack

        // old situation for me
        final double meOldAcc = me.calcAccModel(currentLaneSegment, null);

        // old situation for old back
        // in old situation same left lane as me
        final Vehicle oldBack = currentLaneSegment.rearVehicle(me);
        final double oldBackOldAcc = (oldBack != null) ? oldBack.calcAccModel(currentLaneSegment, null) : 0.0;

        // old situation for new back: just provides the actual left-lane situation
        final double newBackOldAcc = (newBack != null) ? newBack.calcAccModel(newLaneSegment, null) : 0.0;

        // new situation for new back:
        final double oldBackNewAcc;
        if (oldBack == null) {
            oldBackNewAcc = 0.0;
        } else {
            // cannot temporarily remove the current vehicle from the current lane, since we are in a loop
            // that iterates over the vehicles in the current lane. So calculate oldBackNewAcc based on just
            // the front vehicle.
            if (currentLaneSegment.frontVehicle(me) != null) { // TODO remove quickhack for avoiding nullpointer
                oldBackNewAcc = oldBack.getLongitudinalModel().calcAcc(oldBack, currentLaneSegment.frontVehicle(me));
            } else {
                oldBackNewAcc = 0.0;
            }

            // currentLaneSegment.removeVehicle(me);
            // oldBackNewAcc = oldBack.calcAccModel(currentLaneSegment, null);
            // currentLaneSegment.addVehicle(me);
        }

        // MOBIL trade-off for driver and neighborhood
        final double oldBackDiffAcc = oldBackNewAcc - oldBackOldAcc;
        final double newBackDiffAcc = newBackNewAcc - newBackOldAcc;
        final double meDiffAcc = meNewAcc - meOldAcc;

        final int changeTo = newLaneSegment.lane() - currentLaneSegment.lane();

        // hack for CCS
        if (me.getLongitudinalModel().modelName() == ModelName.CCS) {
            double biasForced = 10000;
            double biasNormal = 0.02;
            double bias;
            final int laneCount = roadSegment.laneCount();

            if (roadSegment.laneSegment(currentLane).type() == Lane.Type.ENTRANCE) {
                double factor = (currentLane > 0.5 * (laneCount - 1)) ? (laneCount - currentLane) : (currentLane + 1);
                // System.out.println("currentLane: " + currentLane + " factor*biasForced=" + factor * biasForced);
                return biasForced * factor;
            }

            // assume increasing lane index from right to left
            bias = +2 * biasNormal / (laneCount - 1) * (currentLane - (0.5 * (laneCount - 1)));

            prospectiveBalance = meDiffAcc + politeness * (oldBackDiffAcc + newBackDiffAcc) - threshold - bias
                    * direction;

            // ###########################################################
            // new hack: bias considering BOTH the plus and the minus lane

            // Parameter considering BOTH the plus and the minus lane
            double vc = 0.5; // maximum speed for the method to be effective
            double biasmax = 50; // maximum bias
            double fracCoop = 0.8; // fraction of cooperative runners/vehicles
            double b = 10; // normal deceleration (=b in acc models)
            double dt = 0.2; // !! get elsewhere!!

            // local variables need for both (1) and (2)

            int lanePlus = currentLane + direction;
            int laneMinus = currentLane - direction;
            if ((Math.min(lanePlus, laneMinus) < 0) || (Math.max(lanePlus, laneMinus) > laneCount - 1))
                return prospectiveBalance;

            final LaneSegment laneSegmentPlus = roadSegment.laneSegment(lanePlus);
            final LaneSegment laneSegmentMinus = roadSegment.laneSegment(laneMinus);
            final Vehicle frontPlus = laneSegmentPlus.frontVehicle(me);
            final Vehicle rearPlus = laneSegmentPlus.rearVehicle(me);
            final Vehicle frontMinus = laneSegmentMinus.frontVehicle(me);
            final Vehicle rearMinus = laneSegmentMinus.rearVehicle(me);
            if ((frontPlus == null) || (frontMinus == null) || (rearPlus == null) || (rearMinus == null))
                return prospectiveBalance;

            double vPlus = Math.min(frontPlus.getSpeed(), rearPlus.getSpeed());
            double vMinus = Math.min(frontMinus.getSpeed(), rearMinus.getSpeed());
            double vAdj = Math.min(vPlus, vMinus);

            // (1) cooperative braking of runners/vehicles
            // to make space for agents on congested adjacent lane(s)

            int vehPerCoopVeh = (int) (1. / (fracCoop + 1e-6));
            double accCoop = 0;
            if (me.getId() % vehPerCoopVeh == 0) {
                accCoop = -b * Math.max(0., (vc - vAdj) / vc)
                        * (Math.min(1., Math.max(0, ((me.getSpeed() - vc) / vc))));
                accCoop = Math.max(-b, accCoop + me.getAcc());
                me.setSpeed(me.getSpeed() + dt * accCoop);
            }

            // (2) anticipatory lane changes of runners adjacent of congested lanes
            // to the free lane on the other side (if applicable)

            int isEffective = ((vPlus - me.getSpeed()) * (me.getSpeed() - vMinus) > 0) ? 1 : 0;
            double abiasBoth = isEffective * ((vPlus - vMinus > 0) ? 1 : -1) * biasmax * Math.max(0., (vc - vAdj) / vc);
            prospectiveBalance += abiasBoth;
            // System.out.println("currentLane=" + currentLane + " direction=" + direction + " vPlus=" + vPlus
            // + " vMinus=" + vMinus + " v=" + me.getSpeed());
            // System.out.println("abiasBoth=" + abiasBoth);

            // ###########################################################

            return prospectiveBalance; // quick hack ends here
        }

        // MOBIL's incentive formula

        final int biasSign = (changeTo == Lane.TO_LEFT) ? 1 : -1;

        prospectiveBalance = meDiffAcc + politeness * (oldBackDiffAcc + newBackDiffAcc) - threshold - biasSign
                * biasRight;

        return prospectiveBalance;
    }

    public double getMinimumGap() {
        return gapMin;
    }

    public double getSafeDeceleration() {
        return bSafe;
    }
}
