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
package org.movsim.simulator.vehicles;

import org.movsim.input.model.vehicle.behavior.NoiseInputData;
import org.movsim.utilities.MyRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Auto-generated Javadoc
// Acceleration noise (white noise OR correlated noise (wiener process)
// for reference see paper:
// M. Treiber, A. Kesting, D. Helbing
// Understanding widely scattered traffic flows, the capacity drop, and platoons as effects of variance-driven time gaps
// Phys. Rev. E 74, 016123 (2006)

/**
 * The Class NoiseImpl.
 */
public class Noise {

    /** The Constant logger. */
    final static Logger logger = LoggerFactory.getLogger(Noise.class);

    /** The Constant SQRT12. */
    static final double SQRT12 = Math.sqrt(12.);

    // input:
    /** The is wiener process. */
    private final boolean isWienerProcess;

    /** The tau relax acc. */
    private final double tauRelaxAcc; // in s

    /** The fluct strength. */
    private final double fluctStrength; // fluct. strength (m^2/s^3) of
                                        // dv/dt=a_det+xi(t), in case of wiener:
                                        // stand. deviation

    // output: delta-correlated random process, var=Q_acc/dt
    /** The xi acc. */
    private double xiAcc = 0;

    /**
     * Instantiates a new noise.
     * 
     * @param parameters
     *            the parameters
     */
    public Noise(NoiseInputData parameters) {

        fluctStrength = parameters.getFluctStrength();
        tauRelaxAcc = parameters.getTau();

        isWienerProcess = (tauRelaxAcc == 0) ? false : true;
        logger.debug("tauRelaxAcc = {}, isWienerProcess = {}", tauRelaxAcc, isWienerProcess);

        // init
        xiAcc = 0;
    }

    // #############################################################
    // update
    // #############################################################

    /**
     * Update.
     * 
     * @param dt
     *            delta-t, simulation time interval, seconds
     */
    public void update(double dt) {

        final double randomVar = MyRandom.nextDouble(); // G(0,1)
        final double randomMu0Sigma1 = SQRT12 * (randomVar - 0.5); // Gleichverteilung
                                                                   // mit
                                                                   // mean=0,
                                                                   // var=1

        // dv(est)=dv + s/ttc*xi_v(t), xi_v(t)= Wiener(1, tau_dv) process
        // Q_dv such that <(xi_dv)^2> = (stddev_dv)^2
        // => Q_dv=2*(stddev_dv)^2/tau

        if (isWienerProcess) {
            final double betaAcc = Math.exp(-dt / tauRelaxAcc);
            xiAcc = betaAcc * xiAcc + fluctStrength * Math.sqrt(2 * dt / tauRelaxAcc) * randomMu0Sigma1;
            // logger.debug("WienerProcess: betaAcc = {}, stdDevAcc*Math.sqrt(2*dt/tauRelaxAcc)*randomMu0Sigma1= {}",
            // betaAcc, (fluctStrength * Math.sqrt(2 * dt / tauRelaxAcc) *
            // randomMu0Sigma1));
        } else {
            // delta-correlated acc noise:
            xiAcc = Math.sqrt(fluctStrength / dt) * randomMu0Sigma1;
        }

    }

    /**
     * Gets the acc error.
     * 
     * @return the acc error
     */
    public double getAccError() {
        // logger.debug("xiAcc = ", xiAcc);
        return xiAcc;
    }

}
