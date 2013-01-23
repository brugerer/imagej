/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2012 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of any organization.
 * #L%
 */

package imagej.data.overlay;

import imagej.ImageJ;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import net.imglib2.roi.RectangleRegionOfInterest;

/**
 * An axis-aligned rectangle, backed by a {@link RectangleRegionOfInterest}.
 * 
 * @author Lee Kamentsky
 * @author Barry DeZonia
 */
public class RectangleOverlay extends
	AbstractROIOverlay<RectangleRegionOfInterest>
{
	private static final long serialVersionUID = 1L;

	// default constructor for use by serialization code
	//   (see AbstractOverlay::duplicate())
	public RectangleOverlay() {
		super(new RectangleRegionOfInterest(new double[] { 0, 0 },
			new double[] { 0, 0 }));
	}
	
	public RectangleOverlay(final ImageJ context) {
		super(context, new RectangleRegionOfInterest(new double[] { 0, 0 },
			new double[] { 0, 0 }));
	}

	@Override
	public void writeExternal(final ObjectOutput out) throws IOException {
		super.writeExternal(out);
		final RectangleRegionOfInterest roi = getRegionOfInterest();
		out.writeDouble(roi.getOrigin(0));
		out.writeDouble(roi.getOrigin(1));
		out.writeDouble(roi.getExtent(0));
		out.writeDouble(roi.getExtent(1));
	}

	@Override
	public void readExternal(final ObjectInput in) throws IOException,
		ClassNotFoundException
	{
		super.readExternal(in);
		final RectangleRegionOfInterest roi = getRegionOfInterest();
		roi.setOrigin(in.readDouble(), 0);
		roi.setOrigin(in.readDouble(), 1);
		roi.setExtent(in.readDouble(), 0);
		roi.setExtent(in.readDouble(), 1);
	}
	
	/*
	@Override
	public Overlay duplicate() {
		RectangleOverlay overlay = new RectangleOverlay(getContext());
		RegionOfInterest origRoi = getRegionOfInterest();
		double[] origin = new double[origRoi.numDimensions()];
		double[] extent = new double[origRoi.numDimensions()];
		for (int i = 0; i < origin.length; i++) {
			origin[i] = origRoi.realMin(i);
			extent[i] = origRoi.realMax(i) - origin[i];
		}
		overlay.getRegionOfInterest().setOrigin(origin);
		overlay.getRegionOfInterest().setExtent(extent);
		overlay.setAlpha(getAlpha());
		overlay.setAxis(Axes.X, Axes.X.ordinal());
		overlay.setAxis(Axes.Y, Axes.Y.ordinal());
		overlay.setFillColor(getFillColor());
		overlay.setLineColor(getLineColor());
		overlay.setLineEndArrowStyle(getLineEndArrowStyle());
		overlay.setLineStartArrowStyle(getLineStartArrowStyle());
		overlay.setLineStyle(getLineStyle());
		overlay.setLineWidth(getLineWidth());
		overlay.setName(getName());
		return overlay;
	}
	*/
	
	@Override
	public void move(double[] deltas) {
		getRegionOfInterest().move(deltas);
	}
	
	public void setOrigin(double val, int dim) {
		getRegionOfInterest().setOrigin(val, dim);
	}

	public void setExtent(double val, int dim) {
		getRegionOfInterest().setExtent(val, dim);
	}
	
	public double getOrigin(int dim) {
		return getRegionOfInterest().getOrigin(dim);
	}

	public double getExtent(int dim) {
		return getRegionOfInterest().getExtent(dim);
	}
}
