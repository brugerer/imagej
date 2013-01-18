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

package imagej.core.tools;

import imagej.data.ChannelCollection;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.display.event.input.MsMovedEvent;
import imagej.event.StatusService;
import imagej.plugin.Plugin;
import imagej.tool.AbstractTool;
import imagej.tool.Tool;

import java.util.HashMap;

import net.imglib2.img.ImgPlus;
import net.imglib2.meta.Axes;
import net.imglib2.ops.function.BijectiveFunction;
import net.imglib2.type.numeric.real.DoubleType;

import org.eclipse.uomo.units.SI;
import org.eclipse.uomo.units.impl.system.Imperial;
import org.unitsofmeasurement.unit.Unit;
import org.unitsofmeasurement.unit.UnitConverter;

// UCAR compatibility
//import ucar.units.SI;
//import ucar.units.StandardUnitDB;
//import ucar.units.Unit;

/**
 * Displays pixel values under the cursor.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Tool.class, name = "Probe", alwaysActive = true)
public class PixelProbe extends AbstractTool {

	private final PixelRecorder recorder = new PixelRecorder(false);

	private UnitLibrary UNITS = null;

	private class UnitLibrary {

		private HashMap<String, Unit<?>> unitMap;

		UnitLibrary() {

			unitMap = new HashMap<String, Unit<?>>();
			unitMap.put("m", SI.METRE);
			unitMap.put("meter", SI.METRE);
			unitMap.put("metre", SI.METRE);
			// unitMap.put("micron", SI.METRE);
			// unitMap.put("nanometer", SI.METRE);
			// unitMap.put("km", SI.METRE);
			unitMap.put("in", Imperial.INCH);
			unitMap.put("inch", Imperial.INCH);
			// unitMap.put("foot", Imperial.INCH);
			// unitMap.put("yard", Imperial.INCH);
			// unitMap.put("mile", Imperial.INCH);
		}

		public Unit<?> getUnit(String unit) {
			return unitMap.get(unit);
		}
	}

	// -- Tool methods --

	// NB - this tool does not consume the events by design
	
	@Override
	public void onMouseMove(final MsMovedEvent evt) {
		final StatusService statusService =
				evt.getContext().getService(StatusService.class);
		final ImageDisplayService dispService =
				evt.getContext().getService(ImageDisplayService.class);
		final ImageDisplay disp = dispService.getActiveImageDisplay();
		if ((disp == null) || !recorder.record(evt)) {
			statusService.clearStatus();
			return;
		}
		Dataset ds = dispService.getActiveDataset(disp);
		ImgPlus<?> imgPlus = ds.getImgPlus();
		final int xAxis = ds.getAxisIndex(Axes.X);
		final int yAxis = ds.getAxisIndex(Axes.Y);
		final double xcal = ds.calibration(xAxis);
		final double ycal = ds.calibration(yAxis);
		final int channelIndex = disp.getAxisIndex(Axes.CHANNEL);
		final long cx = recorder.getCX();
		final long cy = recorder.getCY();
		ChannelCollection values = recorder.getValues();
		// TODO - hack. these will later be taken straight from ImgPlus.
		// NaN workaround should not be needed if we fix initialization bugs
		Axis<?> X = new LinearAxis(0, Double.isNaN(xcal) ? 1 : xcal);
		X.setUnit(imgPlus.calibrationUnit(xAxis));
		// NaN workaround should not be needed if we fix initialization bugs
		Axis<?> Y = new LogAxis(0, Double.isNaN(ycal) ? 1 : ycal);
		Y.setUnit(imgPlus.calibrationUnit(yAxis));
		// TEMP HACK for testing purposes
		disp.setUnit(Axes.X, "meter");
		disp.setUnit(Axes.Y, "meter");
		StringBuilder builder = new StringBuilder();
		builder.append("x=");
		appendAxisValue(builder, cx, X, disp.getUnit(Axes.X));
		builder.append(", y=");
		appendAxisValue(builder, cy, Y, disp.getUnit(Axes.Y));
		builder.append(", value=");
		// single channel image
		if ((channelIndex == -1) || (disp.dimension(channelIndex) == 1))
		{
			String valueStr = valueString(values.getChannelValue(0));
			builder.append(valueStr);
		}
		else { // has multiple channels
			int currChannel = disp.getIntPosition(channelIndex);
			String valueStr = valueString(values.getChannelValue(currChannel));
			builder.append(valueStr);
			builder.append(" from (");
			for (int i = 0; i < values.getChannelCount(); i++) {
				valueStr = valueString(values.getChannelValue(i));
				if (i > 0) builder.append(",");
				builder.append(valueStr);
			}
			builder.append(")");
		}
		statusService.showStatus(builder.toString());
	}
	
	// -- helpers --
	
	public Unit<?> getUnit(String unit) {
		if (UNITS == null) UNITS = new UnitLibrary();
		return UNITS.getUnit(unit);
	}

	private String valueString(double value) {
		if (recorder.getDataset().isInteger())
			return String.format("%d",(long)value);
		return String.format("%f", value);
	}

	private void appendAxisValue(StringBuilder builder, double value,
		Axis<?> axis, String dispUnitName)
	{
		double scaledValue = axis.getAbsoluteMeasure(value);

		// SymbolMap unitMap = null;
		// Unit<?> userUnit = unitMap.getUnit(axis.getUnit());
		// Unit<?> displayUnit = unitMap.getUnit(dispUnitName);
		// Unit<?> userUnit = null;
		// Unit<?> displayUnit = null;
		Unit<?> userUnit = getUnit(axis.getUnit());
		Unit<?> displayUnit = getUnit(dispUnitName);

		if (userUnit == null) {
			builder.append(String.format("%.3f", scaledValue));
			if (displayUnit != null) {
				// treat values as if they are in desired units
				builder.append(" ");
				builder.append(displayUnit.getSymbol());
			}
		}
		else { // userUnit != null
			if (displayUnit == null) {
				// TEMP? treat user unit as display unit
				displayUnit = userUnit;
				/*
				// ideally this case should never happen since displayUnit should fall
				// back to dataset unit which we know is not null
				throw new IllegalStateException(
					"null display unit should not be possible here");
				*/
			}
			double val;
			try {
				UnitConverter converter = userUnit.getConverterTo((Unit) displayUnit);
				val = converter.convert(scaledValue);
			}
			catch (Exception e) {
				val = scaledValue;
			}
			builder.append(val);
			builder.append(displayUnit.getSymbol());
		}
	}

	// NOTE that one could use any text specified equation as a scaling axis! It
	// might require edits to that equation class since it uses long[] input.

	// TODO - write test code verifying that the definitions of the scaling funcs
	// are indeed bijective

	private abstract class AbstractScalingInfo {

		protected double offset, scale;

		public AbstractScalingInfo(double offset, double scale) {
			this.offset = offset;
			this.scale = scale;
		}

		public double getOffset() {
			return offset;
		}

		public double getScale() {
			return scale;
		}
	}

	private class LinearScalingFunction extends AbstractScalingInfo implements
		BijectiveFunction<DoubleType, DoubleType>
	{

		public LinearScalingFunction(double offset, double scale) {
			super(offset, scale);
		}

		@Override
		public void compute(DoubleType input, DoubleType output) {
			double value = offset + scale * input.get();
			output.setReal(value);
		}

		@Override
		public void computeInverse(DoubleType output, DoubleType input) {
			double value = (output.get() - offset) / scale;
			input.setReal(value);
		}

		@Override
		public DoubleType createOutput() {
			return new DoubleType();
		}

		@Override
		public DoubleType createInput() {
			return new DoubleType();
		}

		@Override
		public LinearScalingFunction copy() {
			return new LinearScalingFunction(offset, scale);
		}

	}

	// TODO - do we need an InverseLogScalingFunction?

	// NB - only works for input values >= 0. For values < 1 the output is large
	// and negative. Users need to think about best way to handle these
	// constraints for their particular case.

	private class LogScalingFunction extends AbstractScalingInfo implements
		BijectiveFunction<DoubleType, DoubleType>
	{

		public LogScalingFunction(double offset, double scale) {
			super(offset, scale);
		}

		@Override
		public void compute(DoubleType input, DoubleType output) {
			double value = offset + scale * Math.log(input.get());
			output.setReal(value);
		}

		@Override
		public void computeInverse(DoubleType output, DoubleType input) {
			double value = Math.exp((output.get() - offset) / scale);
			input.setReal(value);
		}

		@Override
		public DoubleType createOutput() {
			return new DoubleType();
		}

		@Override
		public DoubleType createInput() {
			return new DoubleType();
		}

		@Override
		public LogScalingFunction copy() {
			return new LogScalingFunction(offset, scale);
		}

	}

	private class PowerScalingFunction extends AbstractScalingInfo implements
		BijectiveFunction<DoubleType, DoubleType>
	{

		private final double power;

		public PowerScalingFunction(double offset, double scale, double power) {
			super(offset, scale);
			this.power = power;
		}

		@Override
		public void compute(DoubleType input, DoubleType output) {
			double value = offset + scale * Math.pow(input.get(), power);
			output.setReal(value);
		}

		@Override
		public void computeInverse(DoubleType output, DoubleType input) {
			double value = Math.pow(((output.get() - offset) / scale), 1 / power);
			input.setReal(value);
		}

		@Override
		public DoubleType createOutput() {
			return new DoubleType();
		}

		@Override
		public DoubleType createInput() {
			return new DoubleType();
		}

		@Override
		public PowerScalingFunction copy() {
			return new PowerScalingFunction(offset, scale, power);
		}

		public double getPower() {
			return power;
		}

	}

	// NB - can match Math.exp() behavior by passing Math.E as base.

	private class ExponentialScalingFunction extends AbstractScalingInfo
		implements
		BijectiveFunction<DoubleType, DoubleType>
	{

		private final double base;

		public ExponentialScalingFunction(double offset, double scale, double base)
		{
			super(offset, scale);
			this.base = base;
		}

		@Override
		public void compute(DoubleType input, DoubleType output) {
			double value = offset + scale * Math.pow(base, input.get());
			output.setReal(value);
		}

		@Override
		public void computeInverse(DoubleType output, DoubleType input) {
			double value = log(base, ((output.get() - offset) / scale));
			output.setReal(value);
		}

		@Override
		public DoubleType createOutput() {
			return new DoubleType();
		}

		@Override
		public DoubleType createInput() {
			return new DoubleType();
		}

		@Override
		public ExponentialScalingFunction copy() {
			return new ExponentialScalingFunction(offset, scale, base);
		}

		public double getBase() {
			return base;
		}

		// i.e. do a 6-based log via log(6, val);

		private double log(double logBase, double val) {
			return Math.log(val) / Math.log(logBase);
		}
	}

	private interface Axis<T extends BijectiveFunction<DoubleType, DoubleType>> {

		T getFunction();

		double getRelativeMeasure(double absoluteMeasure);

		double getAbsoluteMeasure(double relativeMeasure);

		void setUnit(String unit);

		String getUnit();
	}

	private abstract class AbstractAxis<T extends BijectiveFunction<DoubleType, DoubleType>>
		implements Axis<T>
	{

		private final T function;
		private String unitName = null;
		private final DoubleType abs = new DoubleType();
		private final DoubleType rel = new DoubleType();

		public AbstractAxis(T func) {
			this.function = func;
		}

		@Override
		public T getFunction() {
			return function;
		}

		@Override
		synchronized public double getRelativeMeasure(double absoluteMeasure) {
			abs.setReal(absoluteMeasure);
			getFunction().computeInverse(abs, rel);
			return rel.get();
		}

		@Override
		synchronized public double getAbsoluteMeasure(double relativeMeasure) {
			rel.setReal(relativeMeasure);
			getFunction().compute(rel, abs);
			return abs.get();
		}

		@Override
		public String getUnit() {
			return unitName;
		}

		@Override
		public void setUnit(String unit) {
			unitName = unit;
		}
	}

	// TODO - should Axis class store an Axis_Type with it? Or no?

	// TODO - is it incorrect to treat axes as some scaling func? I.e. is it
	// fine to always have linear scaling? Or is the scaling function
	// abstraction useful?
	
	// TODO - also is a BijectiveFunction always needed? Are we limiting ourselves
	// here? Could have two funcs: a forward (required) and an inverse (optional)

	// TODO - do we just have axes defined via any old bijective func as part
	// of constructor?
	
	// TODO - do we want to define an axis by an equation instead of all these
	// classes? Sort of like IJ1's CurveFitter class.
	
	// TODO - the axes classes below may be limited in that they all emanate
	// from origin 0,0. Invert Y is one issue. But so is a LogAxis that we
	// might want to accelerate faster from right to left.
	
	// TODO - there is little reason for these next classes except for the
	// ability to have a correctly typed getFunction() method.
	
	private class LinearAxis extends AbstractAxis<LinearScalingFunction> {

		public LinearAxis(double offset, double scale) {
			super(new LinearScalingFunction(offset, scale));
		}

	}

	private class LogAxis extends AbstractAxis<LogScalingFunction> {

		public LogAxis(double offset, double scale) {
			super(new LogScalingFunction(offset, scale));
		}
	}

	private class PowerAxis extends AbstractAxis<PowerScalingFunction> {

		public PowerAxis(double offset, double scale, double power) {
			super(new PowerScalingFunction(offset, scale, power));
		}

	}

	private class ExponentialAxis extends
		AbstractAxis<ExponentialScalingFunction>
	{

		public ExponentialAxis(double offset, double scale, double base) {
			super(new ExponentialScalingFunction(offset, scale, base));
		}

	}
}
