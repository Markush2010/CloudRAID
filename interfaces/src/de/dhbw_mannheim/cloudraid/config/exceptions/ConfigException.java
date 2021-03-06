/*
 * Copyright 2011 - 2012 by the CloudRAID Team
 * see AUTHORS for more details
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

package de.dhbw_mannheim.cloudraid.config.exceptions;

/**
 * @author Markus Holtermann
 * 
 */
public class ConfigException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3356582921046096965L;

	public ConfigException() {
		super();
	}

	public ConfigException(String arg0) {
		super(arg0);
	}

	public ConfigException(String arg0, Throwable cause) {
		super(arg0, cause);
	}

	public ConfigException(Throwable cause) {
		super(cause);
	}

}
