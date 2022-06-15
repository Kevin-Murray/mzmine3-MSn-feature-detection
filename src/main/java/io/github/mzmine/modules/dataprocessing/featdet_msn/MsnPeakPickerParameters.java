/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package io.github.mzmine.modules.dataprocessing.featdet_msn;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelectionParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTToleranceParameter;

public class MsnPeakPickerParameters extends SimpleParameterSet {

    public static final RawDataFilesParameter dataFiles = new RawDataFilesParameter();

    /**
     * Scan selection. MSn level must be set through MS Level field. Default MS3.
     */
    public static final ScanSelectionParameter scanSelection
            = new ScanSelectionParameter(new ScanSelection(3));

    /**
     * MZ tolerance for precursor chromatogram building.
     */
    public static final MZToleranceParameter mzDifference = new MZToleranceParameter();

    /**
     * RT tolerance for precursor chromatogram building.
     */
    public static final RTToleranceParameter rtTolerance = new RTToleranceParameter();

    public MsnPeakPickerParameters() {
        super(new Parameter[]{dataFiles, scanSelection, mzDifference,  rtTolerance});
    }
}
