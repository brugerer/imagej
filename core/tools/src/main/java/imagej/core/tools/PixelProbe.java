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
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.display.event.input.MsMovedEvent;
import imagej.event.StatusService;
import imagej.plugin.Plugin;
import imagej.tool.AbstractTool;
import imagej.tool.Tool;
import imagej.util.UnitUtils;
import net.imglib2.Axis;
import net.imglib2.meta.Axes;

/**
 * Displays pixel values under the cursor.
 * 
 * @author Barry DeZonia
 */
@Plugin(type = Tool.class, name = "Probe", alwaysActive = true)
public class PixelProbe extends AbstractTool {

	private final PixelRecorder recorder = new PixelRecorder(false);

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
		final int xAxisIndex = disp.getAxisIndex(Axes.X);
		final int yAxisIndex = disp.getAxisIndex(Axes.Y);
		final Axis xAxis = disp.axis(xAxisIndex);
		final Axis yAxis = disp.axis(yAxisIndex);
		final int channelIndex = disp.getAxisIndex(Axes.CHANNEL);
		final long cx = recorder.getCX();
		final long cy = recorder.getCY();
		double xVal = xAxis.getCalibratedMeasure(cx);
		double yVal = yAxis.getCalibratedMeasure(cy);
		ChannelCollection values = recorder.getValues();
		StringBuilder builder = new StringBuilder();
		builder.append("x=");
		builder.append(String.format("%.2f", xVal));
		if (UnitUtils.filterUnit(xAxis.getUnit()) != null) {
			builder.append(" ");
			builder.append(xAxis.getUnit());
		}
		builder.append(", y=");
		builder.append(String.format("%.2f", yVal));
		if (UnitUtils.filterUnit(yAxis.getUnit()) != null) {
			builder.append(" ");
			builder.append(yAxis.getUnit());
		}
		builder.append(", value=");
		// single channel image
		if ((channelIndex == -1) ||
				(recorder.getDataset().dimension(channelIndex) == 1))
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
	
	private String valueString(double value) {
		if (recorder.getDataset().isInteger())
			return String.format("%d",(long)value);
		return String.format("%f", value);
	}

}
