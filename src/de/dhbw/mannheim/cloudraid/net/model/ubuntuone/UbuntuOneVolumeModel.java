/*
 * Copyright 2011 by the CloudRAID Team, see AUTHORS for more details.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package de.dhbw.mannheim.cloudraid.net.model.ubuntuone;

import org.json.JSONException;
import org.json.JSONObject;

import de.dhbw.mannheim.cloudraid.net.model.VolumeModel;

public class UbuntuOneVolumeModel extends VolumeModel {

	public UbuntuOneVolumeModel(JSONObject object) {
		String[] names = JSONObject.getNames(object);
		for (int i = 0; i < names.length; i++) {
			try {
				this.addMetadata(names[i], object.get(names[i]));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.setName(((String) this.getMetadata("path")).substring(2));
	}

	public UbuntuOneVolumeModel(String content) throws JSONException {
		this(new JSONObject(content));
	}

	@Override
	public String toString() {
		return String.format("@UbuntuOneVolume(name=%s)", this.getName());
	}
}