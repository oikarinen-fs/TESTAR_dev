/***************************************************************************************************
 *
 * Copyright (c) 2020 Universitat Politecnica de Valencia - www.upv.es
 * Copyright (c) 2020 Open Universiteit - www.ou.nl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holder nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************************************/

package org.testar.ios;

import org.fruit.alayer.AWTCanvas;
import org.fruit.alayer.Action;
import org.fruit.alayer.Rect;
import org.fruit.alayer.Shape;
import org.fruit.alayer.State;
import org.fruit.alayer.Tags;
import org.testar.ios.actions.IOSActionClick;

import es.upv.staq.testar.ProtocolUtil;
import es.upv.staq.testar.serialisation.ScreenshotSerialiser;

public class IOSProtocolUtil extends ProtocolUtil {

	public static String getActionshot(State state, Action action) {

		if(action instanceof IOSActionClick) {
			try {
				IOSActionClick iosClick = (IOSActionClick) action;
				Shape iosClickShape = iosClick.get(Tags.OriginWidget).get(Tags.Shape);

				Rect rect = Rect.from(iosClickShape.x(), iosClickShape.y(), iosClickShape.width(), iosClickShape.height());
				AWTCanvas scrshot = AWTCanvas.fromScreenshot(rect, state.get(Tags.HWND, (long)0));
				return ScreenshotSerialiser.saveActionshot(state.get(Tags.ConcreteIDCustom, "NoConcreteIdAvailable"), action.get(Tags.ConcreteIDCustom, "NoConcreteIdAvailable"), scrshot);
			} catch(Exception e) {}
		}

		return ProtocolUtil.getActionshot(state, action);
	}

}