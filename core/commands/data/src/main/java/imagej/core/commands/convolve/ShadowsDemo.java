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

package imagej.core.commands.convolve;

import imagej.Cancelable;
import imagej.command.ContextCommand;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import imagej.data.display.OverlayService;
import imagej.display.Display;
import imagej.display.event.DisplayDeletedEvent;
import imagej.display.event.input.KyPressedEvent;
import imagej.event.EventHandler;
import imagej.event.EventService;
import imagej.event.StatusService;
import imagej.input.KeyCode;
import imagej.menu.MenuConstants;
import imagej.plugin.Menu;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;
import imagej.util.RealRect;
import net.imglib2.Axis;
import net.imglib2.meta.Axes;
import net.imglib2.meta.AxisType;

/**
 * Implements legacy ImageJ's Shadows Demo plugin functionality.
 * 
 * @author Barry DeZonia
 */
@Plugin(menu = {
	@Menu(label = MenuConstants.PROCESS_LABEL,
		weight = MenuConstants.PROCESS_WEIGHT,
		mnemonic = MenuConstants.PROCESS_MNEMONIC),
	@Menu(label = "Shadows", mnemonic = 's'),
	@Menu(label = "Shadows Demo", weight = 200) }, headless = true)
public class ShadowsDemo extends ContextCommand implements Cancelable {

	private static final double[][] KERNELS = new double[][] {
		ShadowsNorth.KERNEL, ShadowsNortheast.KERNEL, ShadowsEast.KERNEL,
		ShadowsSoutheast.KERNEL, ShadowsSouth.KERNEL, ShadowsSouthwest.KERNEL,
		ShadowsWest.KERNEL, ShadowsNorthwest.KERNEL };

	// -- instance variables that are Parameters --

	@Parameter
	private EventService eventService;

	@Parameter
	private StatusService statusService;

	@Parameter
	private ImageDisplayService imgDispService;

	@Parameter
	private OverlayService overlayService;

	@Parameter
	private ImageDisplay display;

	// -- private instance variables --

	private boolean userHasQuit = false;

	private String err;

	// -- public interface --

	/**
	 * Runs the plugin. The plugin continually runs each shadow transformation
	 * until ESC is pressed.
	 */
	@Override
	public void run() {
		if (unsupportedImage(display)) {
			err = "This command only works with a single plane of data";
			return;
		}
		statusService.showStatus("Press ESC to terminate");

		final Dataset input = imgDispService.getActiveDataset(display);
		final RealRect selection = overlayService.getSelectionBounds(display);
		final Dataset originalData = input.duplicate();
		userHasQuit = false;
		while (!userHasQuit) {
			for (int i = 0; i < KERNELS.length; i++) {
				final Convolve3x3Operation operation =
					new Convolve3x3Operation(input, selection, KERNELS[i]);
				operation.run();
				try {
					Thread.sleep(100);
				}
				catch (final Exception e) {
					// do nothing
				}
				originalData.copyInto(input);
				if (userHasQuit) break;
			}
		}
		statusService.showStatus("Shadows demo terminated");
	}

	@Override
	public boolean isCanceled() {
		return err != null;
	}

	@Override
	public String getCancelReason() {
		return err;
	}

	public void setDisplay(ImageDisplay disp) {
		display = disp;
	}
	
	public ImageDisplay getDisplay() {
		return display;
	}
	
	// -- event handlers --
	
	@EventHandler
	protected void onEvent(final KyPressedEvent event) {
		if (event.getCode() == KeyCode.ESCAPE) {
			final Display<?> disp = event.getDisplay();
			if (disp != null) {
				if (disp == display) userHasQuit = true;
			}
			else { // disp == null : event from application bar
				if (imgDispService.getActiveImageDisplay() == display) {
					userHasQuit = true;
				}
			}
		}
	}

	@EventHandler
	protected void onEvent(final DisplayDeletedEvent event) {
		if (event.getObject() == display) userHasQuit = true;
	}

	// -- helpers --
	
	/**
	 * Returns true if image cannot be represented as a single plane for display.
	 * This mirrors legacy ImageJ's behavior.
	 */
	private boolean unsupportedImage(ImageDisplay disp) {
		final Dataset input = imgDispService.getActiveDataset(disp);
		final Axis<?>[] axes = input.getAxes();
		final long[] dims = input.getDims();
		for (int i = 0; i < axes.length; i++) {
			final AxisType axis = axes[i].getType();
			if (axis == Axes.X) continue;
			if (axis == Axes.Y) continue;
			if (axis == Axes.CHANNEL && input.isRGBMerged()) continue;
			if (dims[i] != 1) return true;
		}
		return false;
	}

}
