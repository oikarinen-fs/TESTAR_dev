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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fruit.alayer.Role;
import org.fruit.alayer.Roles;

public class IOSRoles {
	private IOSRoles() {
	}

	private static final Map<String, Role> tagToRole = new HashMap<>();

	public static final Role
	iosWidget = from("-1", "iosWidget", Roles.Widget),
	iosUnknown = from("-1", "iosUnknown", Roles.Widget);

	private static Role from(String tag, String name, Role... inheritFrom) {
		Role ret = Role.from(name, inheritFrom);
		tagToRole.put(tag, ret);
		return ret;
	}

	public static Role fromTypeId(String tag) {
		Role ret = tagToRole.get(tag);
		return (ret == null) ? iosUnknown : ret;
	}

	public static Collection<Role> rolesSet() {
		return tagToRole.values();
	}

	public static Role[] nativeClickableRoles() {
		return new Role[]{};
	}

	public static List<String> clickableInputTypes() {
		return Arrays.asList("");
	}

	public static Role[] nativeTypeableRoles() {
		return new Role[]{};
	}

	public static List<String> typeableInputTypes() {
		return Arrays.asList("");
	}

}
