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
package org.movsim.simulator.vehicles.longitudinalmodel.acceleration;

import org.movsim.input.model.vehicle.longitudinalmodel.LongitudinalModelInputDataKKW;
import org.movsim.simulator.vehicles.Vehicle;
import org.movsim.simulator.vehicles.longitudinalmodel.LongitudinalModelBase;
import org.movsim.utilities.MyRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
// paper reference / Kerner book
// TODO consider also external speed limits 
/**
 * The Class KKW.
 */
public class KKW extends LongitudinalModelBase {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(KKW.class);

    /**
     * The Constant dtCA. constant update timestep for CA
     */
    private static final double dtCA = 1; //

    /**
     * The k. Multiplikator fuer sync-Abstand D=lveh+k*v*tau
     */
    private final double k;

    /**
     * The pb0. "Troedelwahrsch." for standing vehicles
     */
    private final double pb0;

    /**
     * The pb1. "Troedelwahrsch." for moving vehicles
     */
    private final double pb1;

    /**
     * The pa1. "Beschl.=Anti-Troedelwahrsch." falls v<vp
     */
    private final double pa1;

    /**
     * The pa2. "Beschl.=Anti-Troedelwahrsch." falls v>=vp
     */
    private final double pa2;

    /**
     * The vp. Geschw., ab der weniger "anti-getroedelt" wird
     */
    private final double vp;

    /** The vehicle length. */
    private final double length;

    /**
     * Instantiates a new kCA.
     * 
     * @param parameters
     *            the parameters
     * @param length
     *            the length
     */
    public KKW(LongitudinalModelInputDataKKW parameters, double length) {
        super(ModelName.KKW, parameters);
        this.length = length; // model parameter!
        logger.debug("init model parameters");
        this.v0 = parameters.getV0();
        this.k = parameters.getK();
        this.pb0 = parameters.getPb0();
        this.pb1 = parameters.getPb1();
        this.pa1 = parameters.getPa1();
        this.pa2 = parameters.getPa2();
        this.vp = parameters.getVp();
    }

    @Override
    protected void setDesiredSpeed(double v0) {
        this.v0 = (int) v0;
    }

    @Override
    public double getS0() {
        throw new UnsupportedOperationException("getS0 not applicable for KKW model.");
    }

    /**
     * Gets the k.
     * 
     * @return the k
     */
    public double getK() {
        return k;
    }

    /**
     * Gets the pb0.
     * 
     * @return the pb0
     */
    public double getPb0() {
        return pb0;
    }

    /**
     * Gets the pb1.
     * 
     * @return the pb1
     */
    public double getPb1() {
        return pb1;
    }

    /**
     * Gets the pa1.
     * 
     * @return the pa1
     */
    public double getPa1() {
        return pa1;
    }

    /**
     * Gets the pa2.
     * 
     * @return the pa2
     */
    public double getPa2() {
        return pa2;
    }

    /**
     * Gets the vp.
     * 
     * @return the vp
     */
    public double getVp() {
        return vp;
    }

    @Override
    public double calcAcc(Vehicle me, Vehicle frontVehicle, double alphaT, double alphaV0, double alphaA) {
        // Local dynamical variables
        final double s = me.getNetDistance(frontVehicle);
        final double v = me.getSpeed();
        final double dv = me.getRelSpeed(frontVehicle);

        return acc(s, v, dv, alphaT, alphaV0);
    }

    @Override
    public double calcAccSimple(double s, double v, double dv) {
        return acc(s, v, dv, 1.0, 1.0);
    }

    /**
     * Acc simple.
     * 
     * @param s
     *            the s
     * @param v
     *            the v
     * @param dv
     *            the dv
     * @param alphaT
     *            the alpha t
     * @param alphaV0
     *            the alpha v0
     * @return the double
     */
    private double acc(double s, double v, double dv, double alphaT, double alphaV0) {

        final int v0Loc = (int) (alphaV0 * v0 + 0.5); // adapt v0 spatially
        final int vLoc = (int) (v + 0.5);

        final double kLoc = alphaT * k;
        // cell length/dt^2 with dt=1 s and length 0.5 m => 0.5 m/s^2
        final int a = 1;

        final double pa = (vLoc < vp) ? pa1 : pa2;
        final double pb = (vLoc < 1) ? pb0 : pb1;
        // double bei Kerner, da k reelle Zahl
        final double D = length + kLoc * vLoc * dtCA;

        // dynamic part
        // (Delta x-d)/tau mit s=Delta x-d und tau=1 (s)
        final int vSafe = (int) s;
        final int dvSign = (dv < -0.5) ? 1 : (dv > 0.5) ? -1 : 0;
        final int vC = (s > D - length) ? vLoc + a * (int) dtCA : vLoc + a * (int) dtCA * dvSign;
        int vtilde = Math.min(Math.min(v0Loc, vSafe), vC);
        vtilde = Math.max(0, vtilde);

        // stochastic part
        final double r1 = MyRandom.nextDouble(); // noise terms ~ G(0,1)
        final int xi = (r1 < pb) ? -1 : (r1 < pb + pa) ? 1 : 0;

        int vNew = 0;
        vNew = Math.min(vtilde + a * (int) dtCA * xi, vLoc + a * (int) dtCA);
        vNew = Math.min(Math.min(v0Loc, vSafe), vNew);
        vNew = Math.max(0, vNew);

        return ((vNew - vLoc) / dtCA);
    }
}
