/*
 * GeoGebra - Dynamic Mathematics for Everyone
 * Copyright (c) GeoGebra GmbH, Altenbergerstr. 69, 4040 Linz, Austria
 * https://www.geogebra.org
 *
 * This file is licensed by GeoGebra GmbH under the EUPL 1.2 licence and
 * may be used under the EUPL 1.2 in compatible projects (see Article 5
 * and the Appendix of EUPL 1.2 for details).
 * You may obtain a copy of the licence at:
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Note: The overall GeoGebra software package is free to use for
 * non-commercial purposes only.
 * See https://www.geogebra.org/license for full licensing details
 */

package org.geogebra.common.kernel.scripting;

import org.geogebra.common.kernel.Kernel;
import org.geogebra.common.kernel.arithmetic.Command;
import org.geogebra.common.kernel.commands.CommandProcessor;
import org.geogebra.common.kernel.commands.EvalInfo;
import org.geogebra.common.kernel.geos.GeoButton;
import org.geogebra.common.kernel.geos.GeoElement;
import org.geogebra.common.kernel.geos.GeoText;
import org.geogebra.common.main.MyError;
import org.geogebra.common.plugin.EventType;
import org.geogebra.common.plugin.ScriptType;
import org.geogebra.common.plugin.script.Script;

/**
 * Button[], Button[caption], Button[caption, script]
 *
 * @author Zbynek
 *
 */
public class CmdButton extends CommandProcessor {

	/**
	 * Create new command processor
	 *
	 * @param kernel
	 *            kernel
	 */
	public CmdButton(Kernel kernel) {
		super(kernel);
	}

	@Override
	public final GeoElement[] process(Command c, EvalInfo info) throws MyError {
		int n = c.getArgumentNumber();
		GeoElement[] arg;

		switch (n) {
			case 2:
				arg = resArgs(c, info);
				if (arg[0].isGeoText() && arg[1].isGeoText()) {
					String caption = ((GeoText) arg[0]).getTextString();
					String scriptText = ((GeoText) arg[1]).getTextString();

					GeoButton gb = new GeoButton(cons);
					gb.setLabelVisible(true);
					gb.setCaption(caption);
					gb.setLabel(c.getLabel());

					// 设置点击脚本
					if (scriptText != null && !scriptText.isEmpty()) {
						ScriptType scriptType = ScriptType.GGBSCRIPT;
						if (scriptText.indexOf("ggbApplet.") > -1) {
							scriptType = ScriptType.JAVASCRIPT;
						}
						Script script = app.createScript(scriptType, scriptText, true);
						gb.setScript(script, EventType.CLICK);
					}

					return new GeoElement[] {gb};
				}
				throw argErr(c, arg[0]);
			case 1:
				arg = resArgs(c, info);
				if (arg[0].isGeoText()) {
					String caption = ((GeoText) arg[0]).getTextString();
					GeoButton gb = new GeoButton(cons);
					gb.setLabelVisible(true);
					gb.setCaption(caption);
					gb.setLabel(c.getLabel());
					return new GeoElement[] {gb};
				}
				throw argErr(c, arg[0]);
			case 0:
				GeoButton gb = new GeoButton(cons);
				gb.setLabelVisible(true);
				gb.setLabel(c.getLabel());
				return new GeoElement[] {gb};

			default:
				throw argNumErr(c);
		}
	}
}
