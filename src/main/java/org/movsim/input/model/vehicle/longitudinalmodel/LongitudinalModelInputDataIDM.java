/**
 * Copyright (C) 2010, 2011 by Arne Kesting, Martin Treiber, Ralph Germ, Martin Budden <info@movsim.org>
 * ----------------------------------------------------------------------
 * 
 * This file is part of
 * 
 * MovSim - the multi-model open-source vehicular-traffic simulator
 * 
 * MovSim is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * 
 * MovSim is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MovSim. If not, see <http://www.gnu.org/licenses/> or
 * <http://www.movsim.org>.
 * 
 * ----------------------------------------------------------------------
 */
package org.movsim.input.model.vehicle.longitudinalmodel;

// TODO: Auto-generated Javadoc
/**
 * The Interface LongitudinalModelInputDataIDM.
 */
public interface LongitudinalModelInputDataIDM extends LongitudinalModelInputData {

    /**
     * Gets the v0.
     * 
     * @return the v0
     */
    double getV0();

    /**
     * Gets the t.
     * 
     * @return the t
     */
    double getT();

    /**
     * Gets the s0.
     * 
     * @return the s0
     */
    double getS0();

    /**
     * Gets the s1.
     * 
     * @return the s1
     */
    double getS1();

    /**
     * Gets the delta.
     * 
     * @return the delta
     */
    double getDelta();

    /**
     * Gets the a.
     * 
     * @return the a
     */
    double getA();

    /**
     * Gets the b.
     * 
     * @return the b
     */
    double getB();

    /**
     * Gets the v0 default.
     * 
     * @return the v0 default
     */
    double getV0Default();

    /**
     * Gets the t default.
     * 
     * @return the t default
     */
    double getTDefault();

    /**
     * Gets the s0 default.
     * 
     * @return the s0 default
     */
    double getS0Default();

    /**
     * Gets the s1 default.
     * 
     * @return the s1 default
     */
    double getS1Default();

    /**
     * Gets the delta default.
     * 
     * @return the delta default
     */
    double getDeltaDefault();

    /**
     * Gets the a default.
     * 
     * @return the a default
     */
    double getaDefault();

    /**
     * Gets the b default.
     * 
     * @return the b default
     */
    double getbDefault();

    /*
     * (non-Javadoc)
     * 
     * @see org.movsim.input.model.vehicle.longModel.AccelerationModelInputData# resetParametersToDefault()
     */
    @Override
    void resetParametersToDefault();

    /**
     * Sets the v0.
     * 
     * @param v0
     *            the new v0
     */
    void setV0(double v0);

    /**
     * Sets the t.
     * 
     * @param timegap
     *            the new t
     */
    void setT(double timegap);

    /**
     * Sets the s0.
     * 
     * @param s0
     *            the new s0
     */
    void setS0(double s0);

    /**
     * Sets the s1.
     * 
     * @param s1
     *            the new s1
     */
    void setS1(double s1);

    /**
     * Sets the delta.
     * 
     * @param delta
     *            the new delta
     */
    void setDelta(double delta);

    /**
     * Sets the a.
     * 
     * @param a
     *            the new a
     */
    void setA(double a);

    /**
     * Sets the b.
     * 
     * @param b
     *            the new b
     */
    void setB(double b);

}