/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2009 - 2013 Board of Regents of the University of
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

package imagej.legacy.translate;

import ij.ImagePlus;
import ij.ImageStack;
import imagej.data.Dataset;
import imagej.data.display.ImageDisplay;
import imagej.data.display.ImageDisplayService;
import net.imglib2.img.Img;
import net.imglib2.img.cell.AbstractCellImg;

// TODO: virtual stack support is minorly problematic. Imglib has vstack impls
// but they use 1-pixel to 1-pixel converters. However in IJ2 in this case we
// have a 3 channel image going to a 1-channel rgb image. So to support virtual
// stacks I can't use Imglib's vstacks. I need to create a special one here
// that converts 3 channels to 1 ARGB. But also note that ARGB is not a RealType
// and thus cannot be the basis of a Dataset.
// Note I have written a FakeVirtualStack class to support this case.

/**
 * Creates {@link ImagePlus}es from {@link ImageDisplay}s containing color
 * merged data.
 * 
 * @author Barry DeZonia
 */
public class ColorImagePlusCreator implements ImagePlusCreator {

	// -- instance variables --

	private final ImageDisplayService imgDispSrv;
	
	private final ColorPixelHarmonizer pixelHarmonizer;
	private final MetadataHarmonizer metadataHarmonizer;
	private final PositionHarmonizer positionHarmonizer;
	private final NameHarmonizer nameHarmonizer;

	// -- public interface --

	public ColorImagePlusCreator(ImageDisplayService imgDispSrv) {
		this.imgDispSrv = imgDispSrv;
		pixelHarmonizer = new ColorPixelHarmonizer();
		metadataHarmonizer = new MetadataHarmonizer();
		positionHarmonizer = new PositionHarmonizer();
		nameHarmonizer = new NameHarmonizer();
	}
	
	/**
	 * Creates a color {@link ImagePlus} from a color {@link ImageDisplay}.
	 * Expects input expects input ImageDisplay to have isRgbMerged() set with 3
	 * channels of unsigned byte data.
	 */
	@Override
	public ImagePlus createLegacyImage(final ImageDisplay display) {
		final Dataset ds = imgDispSrv.getActiveDataset(display);
		Img<?> img = ds.getImgPlus().getImg();
		final ImagePlus imp;
		if (AbstractCellImg.class.isAssignableFrom(img.getClass())) {
			imp = cellImgCase(ds);
		}
		else {
			imp = makeColorImagePlus(ds);
			pixelHarmonizer.updateLegacyImage(ds, imp);
		}
		metadataHarmonizer.updateLegacyImage(ds, imp);
		positionHarmonizer.updateLegacyImage(display, imp);
		nameHarmonizer.updateLegacyImage(display, imp);
		return imp;
	}

	// -- private interface --

	/**
	 * Makes a color {@link ImagePlus} from a color {@link Dataset}. The ImagePlus
	 * will have the same X, Y, Z, & T dimensions. C will be 1. The data values
	 * and metadata are not assigned. Throws an exception if the dataset is not
	 * color compatible.
	 */
	private ImagePlus makeColorImagePlus(final Dataset ds) {
		if (!LegacyUtils.isColorCompatible(ds)) {
			throw new IllegalArgumentException("Dataset is not color compatible");
		}

		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);
		final int w = dimValues[0];
		final int h = dimValues[1];
		final int c = dimValues[2] / 3;
		final int z = dimValues[3];
		final int t = dimValues[4];

		final ImageStack stack = new ImageStack(w, h, c * z * t);

		for (int i = 0; i < c * z * t; i++)
			stack.setPixels(new int[w * h], i + 1);

		final ImagePlus imp = new ImagePlus(ds.getName(), stack);

		imp.setDimensions(c, z, t);

		imp.setOpenAsHyperStack(imp.getNDimensions() > 3);

		return imp;
	}

	private ImagePlus cellImgCase(Dataset ds) {
		ImageStack stack = new MergedRgbVirtualStack(ds);
		ImagePlus imp = new ImagePlus(ds.getName(), stack);

		// TODO - is this right? what about 6d and up? encoded channels?
		final int[] dimIndices = new int[5];
		final int[] dimValues = new int[5];
		LegacyUtils.getImagePlusDims(ds, dimIndices, dimValues);
		final int cCount = dimValues[2];
		final int zCount = dimValues[3];
		final int tCount = dimValues[4];
		imp.setDimensions(cCount, zCount, tCount);

		// TODO - is this right?
		imp.setOpenAsHyperStack(ds.numDimensions() > 3);

		return imp;
	}

}
