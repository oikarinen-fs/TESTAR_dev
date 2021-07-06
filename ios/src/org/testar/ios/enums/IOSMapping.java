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

package org.testar.ios.enums;

import static es.upv.staq.testar.StateManagementTags.*;
import java.util.HashMap;
import java.util.Map;

import org.fruit.alayer.Tag;
import org.fruit.alayer.Tags;

public class IOSMapping {
	// a mapping from the state management tags to IOS tags
    private static Map<Tag<?>, Tag<?>> stateTagMappingIOS = new HashMap<Tag<?>, Tag<?>>()
    {
        {
            put(WidgetPath, Tags.Path);
            put(WidgetTitle, IOSTags.iosText);
            put(WidgetControlType, IOSTags.iosClassName);
            put(WidgetClassName, IOSTags.iosClassName);
            put(WidgetAutomationId, IOSTags.iosResourceId);
            put(WidgetIsEnabled, IOSTags.iosEnabled);
            put(WidgetFrameworkId, IOSTags.iosPackageName);
        }
    };
    
    /**
     * This method will return its equivalent, internal IOS tag, if available.
     * @param mappedTag
     * @return
     */
    public static <T> Tag<T> getMappedStateTag(Tag<T> mappedTag) {
        return (Tag<T>) stateTagMappingIOS.getOrDefault(mappedTag, null);
    }
}