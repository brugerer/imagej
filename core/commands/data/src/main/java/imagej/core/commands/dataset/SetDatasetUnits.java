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

package imagej.core.commands.dataset;

import imagej.command.Command;
import imagej.data.Dataset;
import imagej.plugin.Parameter;
import imagej.plugin.Plugin;

// TODO
//   - list all axes of the Dataset dynamically rather than just X & Y
//   - allow one to specify offsets and scales too

/**
 * @author Barry DeZonia
 */
@Plugin(menuPath = "Image>Units>Set Data Units", initializer = "init")
public class SetDatasetUnits implements Command {

	// -- Parameters --

	@Parameter
	private Dataset dataset;

	@Parameter(label = "X Axis Unit", persist = false)
	private String xUnit;

	@Parameter(label = "Y Axis Unit", persist = false)
	private String yUnit;

	// -- Command methods --

	@Override
	public void run() {
		dataset.getImgPlus().axis(0).setUnit(xUnit);
		dataset.getImgPlus().axis(1).setUnit(yUnit);
	}

	// -- helpers --

	protected void init() {
		xUnit = dataset.getImgPlus().axis(0).getUnit();
		yUnit = dataset.getImgPlus().axis(1).getUnit();
	}

}
